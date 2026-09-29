package com.example.capture.user;

import com.example.capture.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// 이게 없으면 비밀번호를 무한히 대입해볼 수 있다. 같은 아이디로 5번 틀리면 5분 잠근다 (stack.md §5).
// ponytail: 메모리에만 있어 재시작하면 풀리고 서버가 2대면 따로 센다. 그때는 DB 컬럼(failed_count, locked_until)으로
@Component
@RequiredArgsConstructor
public class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK = Duration.ofMinutes(5);

    private record Failures(int count, Instant lockedUntil) {}

    private final Clock clock;
    // 있는 아이디만 센다. 아무 문자열이나 세면 공격자가 이 맵을 무한히 키울 수 있다
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    public void checkAllowed(String loginId) {
        Failures current = failures.get(loginId);
        if (current != null && current.lockedUntil() != null && Instant.now(clock).isBefore(current.lockedUntil())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "로그인 시도가 너무 많습니다. 5분 뒤에 다시 해 주세요");
        }
    }

    public void recordFailure(String loginId) {
        failures.compute(loginId, (id, current) -> {
            int count = (current == null ? 0 : current.count()) + 1;
            // 잠금이 풀린 뒤에는 다시 5번의 기회를 준다
            return count >= MAX_FAILURES
                    ? new Failures(0, Instant.now(clock).plus(LOCK))
                    : new Failures(count, null);
        });
    }

    public void recordSuccess(String loginId) {
        failures.remove(loginId);
    }
}
