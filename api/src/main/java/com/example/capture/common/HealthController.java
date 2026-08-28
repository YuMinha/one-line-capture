package com.example.capture.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 예외 대상. compose healthcheck가 이 경로를 찌른다 (stack.md §3.1)
@RestController
public class HealthController {

    public record HealthResponse(String status) {}

    @GetMapping("/api/v1/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}
