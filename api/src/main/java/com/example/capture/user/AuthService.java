package com.example.capture.user;

import com.example.capture.common.ApiException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern LOGIN_ID = Pattern.compile("[a-z0-9_]{4,20}");
    // 형식이 맞는지만 본다. 진짜 받을 수 있는 주소인지는 메일 인증을 붙일 때 확인한다 (stack.md §5)
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final AppUserRepository appUserRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final LoginThrottle loginThrottle;
    private final LinkCodeRepository linkCodeRepository;
    private final Clock clock;

    // 0/O, 1/I/L을 뺐다. 폰 화면을 보고 PC에 옮겨 칠 때 헷갈리지 않게.
    // 31자 8칸 = 약 8.5×10^11. 5분 안에 대입으로 맞히는 건 불가능해서 시도 제한이 필요 없다 (stack.md §5)
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Me(boolean registered, String loginId, String email) {}

    public record IssuedCode(String code, Instant expiresAt) {}

    // 가입 없이 기기마다 계정을 하나씩 준다. 친구가 주소만 열어도 바로 써볼 수 있게 (spec.md §2)
    // ponytail: 발급 횟수 제한 없음. 남용되면 IP당 분당 N회 제한을 필터에 추가
    @Transactional
    public String issueGuest() {
        AppUser user = appUserRepository.save(new AppUser());
        return issueToken(user.getId());
    }

    @Transactional
    public Me register(Long userId, String loginId, String email, String password) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        if (user.isRegistered()) {
            throw conflict("ALREADY_REGISTERED", "이미 계정이 있습니다");
        }
        String id = normalize(loginId);
        String mail = normalize(email);
        if (!LOGIN_ID.matcher(id).matches()) {
            throw ApiException.badRequest("INVALID_LOGIN_ID", "아이디는 영문 소문자·숫자·_ 4~20자입니다");
        }
        if (mail.length() > 254 || !EMAIL.matcher(mail).matches()) {
            throw ApiException.badRequest("INVALID_EMAIL", "이메일 형식이 올바르지 않습니다");
        }
        if (password == null || password.length() < 8 || password.length() > 100) {
            throw ApiException.badRequest("INVALID_PASSWORD", "비밀번호는 8~100자입니다");
        }
        if (appUserRepository.existsByLoginId(id)) {
            throw conflict("LOGIN_ID_TAKEN", "이미 쓰는 아이디입니다");
        }
        if (appUserRepository.existsByEmail(mail)) {
            throw conflict("EMAIL_TAKEN", "이미 가입된 이메일입니다");
        }

        user.register(id, mail, Passwords.hash(password));
        try {
            // 위의 exists 검사와 저장 사이에 누가 같은 아이디로 가입하면 UNIQUE가 막는다. 그걸 409로 돌려준다
            appUserRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw conflict("LOGIN_ID_TAKEN", "이미 쓰는 아이디나 이메일입니다");
        }
        return me(userId);
    }

    // 없는 아이디와 틀린 비밀번호를 같은 응답으로 돌려준다. 구분하면 가입된 아이디를 캐낼 수 있다
    @Transactional
    public String login(String loginId, String password) {
        String id = normalize(loginId);
        loginThrottle.checkAllowed(id);

        AppUser user = appUserRepository.findByLoginId(id).orElse(null);
        String raw = password == null ? "" : password;
        // 없는 아이디여도 해시를 한 번 계산한다. 응답 시간으로도 구분되지 않게
        boolean ok = Passwords.matches(raw, user == null ? Passwords.DUMMY_HASH : user.getPasswordHash()) && user != null;
        if (!ok) {
            if (user != null) {
                loginThrottle.recordFailure(id);
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디나 비밀번호가 올바르지 않습니다");
        }
        loginThrottle.recordSuccess(id);
        return issueToken(user.getId());
    }

    // 이 기기의 토큰만 지운다. 폰에서 로그아웃해도 PC는 그대로다
    @Transactional
    public void logout(Long tokenId) {
        deviceTokenRepository.deleteById(tokenId);
    }

    // 새 코드를 받으면 이전 코드는 죽는다. 살아 있는 코드가 사용자당 하나뿐이어야 새어 나간 코드를 신경 쓸 곳이 준다
    @Transactional
    public IssuedCode issueLinkCode(Long userId) {
        linkCodeRepository.deleteByUserId(userId);
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        Instant expiresAt = Instant.now(clock).plus(CODE_TTL);
        linkCodeRepository.save(new LinkCode(DeviceToken.hashOf(code.toString()), userId,
                LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)));
        return new IssuedCode(code.substring(0, 4) + "-" + code.substring(4), expiresAt);
    }

    // 1회용이다. 쓰는 순간 지워서, 어깨너머로 본 사람이 같은 코드를 또 쓸 수 없다
    @Transactional
    public String redeemLinkCode(String code) {
        // 하이픈·공백·소문자로 쳐도 받아준다. 사람이 옮겨 치는 값이다
        String normalized = code == null ? "" : code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        LinkCode found = linkCodeRepository.findById(DeviceToken.hashOf(normalized))
                .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC)))
                .orElseThrow(() -> ApiException.badRequest("LINK_CODE_INVALID", "코드가 틀렸거나 만료됐습니다"));
        linkCodeRepository.delete(found);
        return issueToken(found.getUserId());
    }

    @Transactional(readOnly = true)
    public Me me(Long userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        return new Me(user.isRegistered(), user.getLoginId(), user.getEmail());
    }

    // 로그인 수단이 무엇이든 끝은 여기다. 인증 경로가 하나면 격리 검사도 한 곳에서 끝난다 (stack.md §5).
    // 원문은 반환값으로 한 번만 나간다. DB에는 해시만 남으므로 서버도 다시 보여줄 수 없다
    String issueToken(Long userId) {
        String token = DeviceToken.newToken();
        deviceTokenRepository.save(new DeviceToken(userId, DeviceToken.hashOf(token), false));
        return token;
    }

    // 폰 키보드는 첫 글자를 대문자로 올린다. 대소문자가 다르다고 로그인이 안 되면 안 된다
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
