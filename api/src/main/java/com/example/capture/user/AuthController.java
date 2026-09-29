package com.example.capture.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record TokenResponse(String token) {}

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse guest() {
        return new TokenResponse(authService.issueGuest());
    }
}
