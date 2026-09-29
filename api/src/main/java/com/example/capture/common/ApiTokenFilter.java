package com.example.capture.common;

import com.example.capture.user.AppUser;
import com.example.capture.user.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Spring Security를 넣지 않는다. "토큰 → 사용자 id" 조회 하나에 프레임워크는 과하다 (stack.md §0.3)
@Component
@RequiredArgsConstructor
public class ApiTokenFilter extends OncePerRequestFilter {

    // 컨트롤러는 @RequestAttribute(USER_ID)로 받는다. 헤더를 직접 읽는 곳이 필터 하나뿐이게
    public static final String USER_ID = "userId";

    private static final String HEADER = "X-API-Token";
    // 헬스체크는 compose가 토큰 없이 찌르고, 게스트 발급은 토큰이 없는 사람이 부른다
    private static final Set<String> OPEN_PATHS = Set.of("/api/v1/health", "/api/v1/auth/guest");

    private final AppUserRepository appUserRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (OPEN_PATHS.contains(request.getRequestURI())) {
            return true;
        }
        // 브라우저 프리플라이트에는 커스텀 헤더가 실리지 않는다
        return "OPTIONS".equals(request.getMethod());
    }

    // 원문이 아니라 해시로 찾으므로 MessageDigest.isEqual 같은 상수 시간 비교가 필요 없다.
    // 응답 시간이 새도 알 수 있는 건 해시 앞자리뿐이고, 그걸로 토큰을 역산할 수 없다 (stack.md §5)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Optional<AppUser> user = Optional.ofNullable(request.getHeader(HEADER))
                .filter(header -> !header.isBlank())
                .flatMap(header -> appUserRepository.findByTokenHash(AppUser.hashOf(header)));
        if (user.isEmpty()) {
            reject(response);
            return;
        }
        request.setAttribute(USER_ID, user.get().getId());
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        // 에러 포맷은 GlobalExceptionHandler와 같아야 한다. 필터는 그 바깥이라 직접 쓴다
        response.getWriter().write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"토큰이 올바르지 않습니다\"}}");
    }
}
