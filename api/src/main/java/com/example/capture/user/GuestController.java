package com.example.capture.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 가입 없이 기기마다 계정을 하나씩 준다. 친구가 주소만 열어도 바로 써볼 수 있게 (spec.md §2)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class GuestController {

    private final AppUserRepository appUserRepository;

    public record GuestResponse(String token) {}

    // 원문은 이 응답으로 한 번만 나간다. DB에는 해시만 남으므로 서버도 다시 보여줄 수 없다
    // ponytail: 발급 횟수 제한 없음. 남용되면 IP당 분당 N회 제한을 필터에 추가
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestResponse issue() {
        String token = AppUser.newToken();
        appUserRepository.save(new AppUser(AppUser.hashOf(token)));
        return new GuestResponse(token);
    }
}
