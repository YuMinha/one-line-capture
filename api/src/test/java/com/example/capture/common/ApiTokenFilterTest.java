package com.example.capture.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.capture.user.DeviceToken;
import com.example.capture.user.DeviceTokenRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 필터를 켠 채로 도는 유일한 테스트다
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiTokenFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    // 주인 토큰. OwnerTokenSync가 기동 시 app_user id=1에 심는다
    @Value("${app.api-token}")
    private String token;

    @Test
    @DisplayName("토큰이 없으면 401이고 에러 포맷은 그대로다")
    void 토큰_없음() throws Exception {
        mockMvc.perform(get("/api/v1/captures"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("토큰이 틀리면 401")
    void 토큰_틀림() throws Exception {
        mockMvc.perform(get("/api/v1/captures").header("X-API-Token", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("길이가 다른 토큰도 401 (비교 시 예외가 나지 않는다)")
    void 길이가_다른_토큰() throws Exception {
        mockMvc.perform(get("/api/v1/captures").header("X-API-Token", "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰이 맞으면 통과한다")
    void 토큰_맞음() throws Exception {
        mockMvc.perform(get("/api/v1/captures").header("X-API-Token", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("저장도 토큰이 있어야 된다")
    void 저장에도_적용된다() throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"우산\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/captures").header("X-API-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"우산\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("게스트 발급은 토큰 없이 부를 수 있고, 받은 토큰으로 바로 통과한다")
    void 게스트_발급() throws Exception {
        String guest = issueGuest();

        mockMvc.perform(get("/api/v1/captures").header("X-API-Token", guest))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게스트 토큰은 매번 다르고, DB에는 원문이 아니라 해시만 남는다")
    void 게스트_토큰은_해시로_저장() throws Exception {
        String first = issueGuest();
        String second = issueGuest();

        assertThat(first).isNotEqualTo(second).hasSizeGreaterThanOrEqualTo(43);
        assertThat(deviceTokenRepository.findByTokenHash(first)).isEmpty();
        assertThat(deviceTokenRepository.findByTokenHash(DeviceToken.hashOf(first))).isPresent();
    }

    @Test
    @DisplayName("빈 헤더는 401 (빈 문자열의 해시로 누군가와 맞아떨어지지 않는다)")
    void 빈_토큰() throws Exception {
        mockMvc.perform(get("/api/v1/captures").header("X-API-Token", ""))
                .andExpect(status().isUnauthorized());
    }

    private String issueGuest() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    @DisplayName("헬스체크는 토큰 없이 열려 있다")
    void 헬스체크는_예외() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }
}
