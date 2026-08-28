package com.example.capture.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    // 테스트에서는 Clock.fixed(...)로 갈아끼운다
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
