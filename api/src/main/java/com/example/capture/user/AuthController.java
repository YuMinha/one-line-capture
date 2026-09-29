package com.example.capture.user;

import com.example.capture.common.ApiTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// guest·login·link는 필터가 열어 둔다. 토큰을 받으러 오는 사람은 아직 토큰이 없다 (ApiTokenFilter.OPEN_PATHS)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record TokenResponse(String token) {}

    public record RegisterRequest(String loginId, String email, String password) {}

    public record LoginRequest(String loginId, String password) {}

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse guest() {
        return new TokenResponse(authService.issueGuest());
    }

    @PostMapping("/register")
    public AuthService.Me register(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId,
                                   @RequestBody RegisterRequest request) {
        return authService.register(userId, request.loginId(), request.email(), request.password());
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return new TokenResponse(authService.login(request.loginId(), request.password()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestAttribute(ApiTokenFilter.TOKEN_ID) Long tokenId) {
        authService.logout(tokenId);
    }

    @GetMapping("/me")
    public AuthService.Me me(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId) {
        return authService.me(userId);
    }
}
