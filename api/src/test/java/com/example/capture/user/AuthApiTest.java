package com.example.capture.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// 필터를 켠 채로 "다른 기기"를 토큰 두 개로 흉내 낸다
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    private static final String PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("게스트로 쓰다 가입하면 기록이 그대로이고, 다른 기기에서 로그인하면 같은 기록이 보인다")
    void 가입_후_다른_기기_로그인() throws Exception {
        String phone = issueGuest();
        create(phone, "우산 챙기기");
        String id = uniqueId();

        register(phone, id, id + "@example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.loginId").value(id));

        String pc = login(id, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String pcToken = JsonPath.read(pc, "$.token");

        mockMvc.perform(as(pcToken, get("/api/v1/captures")))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].rawText").value("우산 챙기기"));
    }

    @Test
    @DisplayName("폰 키보드가 첫 글자를 대문자로 올려도 로그인된다")
    void 아이디_대소문자_무시() throws Exception {
        String id = registeredUser();

        login(id.substring(0, 1).toUpperCase() + id.substring(1) + " ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("틀린 비밀번호와 없는 아이디는 같은 코드로 거절한다 (가입 여부를 캐낼 수 없다)")
    void 로그인_실패는_구분하지_않는다() throws Exception {
        String id = registeredUser();

        login(id, "wrong-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
        login("nobody_" + id, PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("5번 틀리면 맞는 비밀번호로도 잠시 못 들어온다")
    void 다섯번_실패하면_잠금() throws Exception {
        String id = registeredUser();

        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            login(id, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(id, PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_ATTEMPTS"));
    }

    @Test
    @DisplayName("4번 틀리고 성공하면 횟수가 초기화된다")
    void 성공하면_초기화() throws Exception {
        String id = registeredUser();

        for (int i = 0; i < LoginThrottle.MAX_FAILURES - 1; i++) {
            login(id, "wrong-password");
        }
        login(id, PASSWORD).andExpect(status().isOk());
        login(id, "wrong-password");
        login(id, PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("이미 쓰는 아이디·이메일, 이미 가입한 계정은 409")
    void 가입_중복() throws Exception {
        String id = registeredUser();

        register(issueGuest(), id, uniqueId() + "@example.com", PASSWORD)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("LOGIN_ID_TAKEN"));
        register(issueGuest(), uniqueId(), id + "@EXAMPLE.com", PASSWORD)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));

        String token = issueGuest();
        String other = uniqueId();
        register(token, other, other + "@example.com", PASSWORD).andExpect(status().isOk());
        String again = uniqueId();
        register(token, again, again + "@example.com", PASSWORD)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("아이디·이메일·비밀번호 형식이 틀리면 400이고 무엇이 틀렸는지 알려준다")
    void 가입_형식() throws Exception {
        String token = issueGuest();

        register(token, "ab", "a@example.com", PASSWORD).andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_ID"));
        register(token, "한글아이디", "a@example.com", PASSWORD).andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_ID"));
        register(token, uniqueId(), "not-an-email", PASSWORD).andExpect(jsonPath("$.error.code").value("INVALID_EMAIL"));
        register(token, uniqueId(), "a@example.com", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"));

        // 실패한 시도들이 계정을 반쯤 만들어 두지 않았다
        mockMvc.perform(as(token, get("/api/v1/auth/me"))).andExpect(jsonPath("$.registered").value(false));
    }

    @Test
    @DisplayName("로그아웃은 이 기기 토큰만 끊는다")
    void 로그아웃은_이_기기만() throws Exception {
        String phone = issueGuest();
        String id = uniqueId();
        register(phone, id, id + "@example.com", PASSWORD);
        String pc = JsonPath.read(login(id, PASSWORD).andReturn().getResponse().getContentAsString(), "$.token");

        mockMvc.perform(as(pc, post("/api/v1/auth/logout"))).andExpect(status().isNoContent());

        mockMvc.perform(as(pc, get("/api/v1/captures"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as(phone, get("/api/v1/captures"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("게스트의 내 정보는 registered=false")
    void 게스트_내_정보() throws Exception {
        mockMvc.perform(as(issueGuest(), get("/api/v1/auth/me")))
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.loginId").doesNotExist());
    }

    private String registeredUser() throws Exception {
        String id = uniqueId();
        register(issueGuest(), id, id + "@example.com", PASSWORD).andExpect(status().isOk());
        return id;
    }

    // 로그인 잠금은 메모리에 남아 테스트끼리 공유된다. 아이디가 겹치면 서로의 실패 횟수를 먹는다
    private String uniqueId() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private ResultActions register(String token, String loginId, String email, String password) throws Exception {
        return mockMvc.perform(as(token, post("/api/v1/auth/register"))
                .content("{\"loginId\":\"" + loginId + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions login(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"));
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("X-API-Token", token).contentType(MediaType.APPLICATION_JSON);
    }

    private void create(String token, String text) throws Exception {
        mockMvc.perform(as(token, post("/api/v1/captures")).content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated());
    }

    private String issueGuest() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
