package com.example.capture.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 주인 토큰의 진실은 여전히 .env의 API_TOKEN이다. 마이그레이션 SQL은 환경변수를 모르므로
// 기동할 때마다 여기서 맞춘다. 덕분에 재발급 절차가 전과 같다: .env 수정 후 api 재시작 (stack.md §5)
@Component
public class OwnerTokenSync implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final String apiToken;

    public OwnerTokenSync(AppUserRepository appUserRepository, @Value("${app.api-token}") String apiToken) {
        this.appUserRepository = appUserRepository;
        this.apiToken = apiToken;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser owner = appUserRepository.findById(AppUser.OWNER_ID)
                .orElseThrow(() -> new IllegalStateException("주인 행(app_user id=1)이 없다. V5 마이그레이션을 확인할 것"));
        owner.changeTokenHash(AppUser.hashOf(apiToken));
    }
}
