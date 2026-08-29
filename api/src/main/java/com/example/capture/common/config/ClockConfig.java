package com.example.capture.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    // 저장은 UTC지만 파싱은 사용자 타임존이어야 한다. systemUTC()로 두면 KST 새벽에
    // "내일"이 하루 덜 밀린다 (stack.md §2.2). 테스트에서는 Clock.fixed(..., ZONE)로 갈아끼운다
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
