package com.example.capture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.capture.common.ApiTokenFilter;
import com.example.capture.user.AppUser;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

// 필터를 끈 테스트(addFilters = false)에서는 userId를 실어줄 주체가 없다.
// 모든 요청을 주인(id=1)이 보낸 것으로 친다. 격리는 UserIsolationTest가 필터를 켠 채로 따로 검증한다
@TestConfiguration
public class AsOwner {

    @Bean
    MockMvcBuilderCustomizer asOwnerRequest() {
        return builder -> builder.defaultRequest(get("/").requestAttr(ApiTokenFilter.USER_ID, AppUser.OWNER_ID));
    }
}
