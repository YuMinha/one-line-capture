package com.example.capture.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LinkCodeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LinkCodeRepository linkCodeRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    @DisplayName("폰에서 받은 코드를 PC에 넣으면 같은 기록이 보인다")
    void 코드로_연결() throws Exception {
        String phone = issueGuest();
        create(phone, "점심 9000원");

        String code = issueCode(phone);
        assertThat(code).matches("[A-Z0-9]{4}-[A-Z0-9]{4}").doesNotContainPattern("[01OIL]");

        String pc = JsonPath.read(link(code).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get("/api/v1/captures").header("X-API-Token", pc))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].rawText").value("점심 9000원"));
    }

    @Test
    @DisplayName("코드는 한 번만 쓸 수 있다")
    void 한번만() throws Exception {
        String code = issueCode(issueGuest());

        link(code).andExpect(status().isOk());
        link(code).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LINK_CODE_INVALID"));
    }

    @Test
    @DisplayName("소문자·하이픈 없이·공백 섞어 쳐도 된다")
    void 입력_정규화() throws Exception {
        String code = issueCode(issueGuest());

        link(" " + code.replace("-", "").toLowerCase() + " ").andExpect(status().isOk());
    }

    @Test
    @DisplayName("새 코드를 받으면 이전 코드는 못 쓴다")
    void 새_코드가_이전_코드를_죽인다() throws Exception {
        String phone = issueGuest();
        String old = issueCode(phone);
        String fresh = issueCode(phone);

        link(old).andExpect(status().isBadRequest());
        link(fresh).andExpect(status().isOk());
    }

    @Test
    @DisplayName("5분이 지난 코드는 못 쓴다")
    void 만료() throws Exception {
        String phone = issueGuest();
        Long userId = deviceTokenRepository.findByTokenHash(DeviceToken.hashOf(phone)).orElseThrow().getUserId();
        linkCodeRepository.save(new LinkCode(DeviceToken.hashOf("EXPRD234"), userId,
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)));

        link("EXPR-D234").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LINK_CODE_INVALID"));
    }

    @Test
    @DisplayName("없는 코드·빈 코드는 400")
    void 없는_코드() throws Exception {
        link("ZZZZ-ZZZZ").andExpect(status().isBadRequest());
        link("").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("코드 발급은 로그인된 기기에서만")
    void 발급에는_토큰이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/link-code")).andExpect(status().isUnauthorized());
    }

    private String issueCode(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/link-code").header("X-API-Token", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.code");
    }

    private ResultActions link(String code) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/link").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"));
    }

    private void create(String token, String text) throws Exception {
        mockMvc.perform(post("/api/v1/captures").header("X-API-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated());
    }

    private String issueGuest() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
