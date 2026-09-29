package com.example.capture.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 주인 토큰의 진실은 여전히 .env의 API_TOKEN이다. 마이그레이션 SQL은 환경변수를 모르므로
// 기동할 때마다 여기서 맞춘다. 덕분에 재발급 절차가 전과 같다: .env 수정 후 api 재시작 (stack.md §5).
// from_env 행만 건드린다. 주인이 로그인·연결 코드로 붙인 다른 기기는 그대로 산다
@Component
public class OwnerTokenSync implements ApplicationRunner {

    private final DeviceTokenRepository deviceTokenRepository;
    private final String apiToken;

    public OwnerTokenSync(DeviceTokenRepository deviceTokenRepository, @Value("${app.api-token}") String apiToken) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.apiToken = apiToken;
    }

    // 지우고 새로 넣지 않고 해시만 바꾼다. Hibernate는 flush할 때 INSERT를 DELETE보다 먼저 보내서,
    // 같은 해시를 지웠다 넣으면 UNIQUE에 걸린다
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = DeviceToken.hashOf(apiToken);
        deviceTokenRepository.findByUserIdAndFromEnvTrue(AppUser.OWNER_ID)
                .ifPresentOrElse(
                        token -> token.changeTokenHash(hash),
                        () -> deviceTokenRepository.save(new DeviceToken(AppUser.OWNER_ID, hash, true)));
    }
}
