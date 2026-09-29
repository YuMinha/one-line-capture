package com.example.capture.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    // 가입 없이 기기마다 계정을 하나씩 준다. 친구가 주소만 열어도 바로 써볼 수 있게 (spec.md §2)
    // ponytail: 발급 횟수 제한 없음. 남용되면 IP당 분당 N회 제한을 필터에 추가
    @Transactional
    public String issueGuest() {
        AppUser user = appUserRepository.save(new AppUser());
        return issueToken(user.getId());
    }

    // 로그인 수단이 무엇이든 끝은 여기다. 인증 경로가 하나면 격리 검사도 한 곳에서 끝난다 (stack.md §5).
    // 원문은 반환값으로 한 번만 나간다. DB에는 해시만 남으므로 서버도 다시 보여줄 수 없다
    private String issueToken(Long userId) {
        String token = DeviceToken.newToken();
        deviceTokenRepository.save(new DeviceToken(userId, DeviceToken.hashOf(token), false));
        return token;
    }
}
