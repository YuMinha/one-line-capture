package com.example.capture.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Spring Security를 넣지 않는다. 토큰 하나를 비교하는 데 프레임워크는 과하다 (stack.md §0.3)
@Component
public class ApiTokenFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Token";
    private static final String HEALTH_PATH = "/api/v1/health";

    private final byte[] expected;

    public ApiTokenFilter(@Value("${app.api-token}") String apiToken) {
        this.expected = apiToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // compose healthcheck가 토큰 없이 찌른다 (stack.md §3.1)
        if (HEALTH_PATH.equals(request.getRequestURI())) {
            return true;
        }
        // 브라우저 프리플라이트에는 커스텀 헤더가 실리지 않는다
        return "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header == null || !matches(header)) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    // equals()는 다른 글자가 나오는 즉시 멈춰서 비교 시간으로 토큰을 추측할 수 있다.
    // MessageDigest.isEqual은 길이가 같으면 끝까지 본다 (stack.md §5)
    private boolean matches(String header) {
        return MessageDigest.isEqual(header.getBytes(StandardCharsets.UTF_8), expected);
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
