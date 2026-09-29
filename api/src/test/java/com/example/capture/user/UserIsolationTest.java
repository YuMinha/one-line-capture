package com.example.capture.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// 필터를 켠 채로 실제 토큰 두 개를 쓴다. 쿼리 하나에서 user_id 조건을 빠뜨리면 여기서 빨개진다.
// 남의 것은 403이 아니라 404여야 한다 — 403은 "그 id가 존재한다"를 알려준다 (stack.md §2.2)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.api-token}")
    private String ownerToken;

    private String alice;
    private String bob;

    @BeforeEach
    void setUp() throws Exception {
        alice = issueGuest();
        bob = issueGuest();
    }

    @Test
    @DisplayName("남의 캡처는 단건 조회·수정·삭제 모두 404")
    void 남의_캡처_단건() throws Exception {
        long id = create(alice, "우산 챙기기");

        mockMvc.perform(as(bob, get("/api/v1/captures/" + id))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, patch("/api/v1/captures/" + id))
                        .content("{\"type\":\"TODO\",\"todo\":{\"title\":\"탈취\"}}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(bob, delete("/api/v1/captures/" + id))).andExpect(status().isNotFound());

        // 실패한 시도가 원본을 건드리지 않았다
        mockMvc.perform(as(alice, get("/api/v1/captures/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todo.title").value("우산 챙기기"));
    }

    @Test
    @DisplayName("남의 할일 완료·링크 읽음 토글은 404")
    void 남의_상세_토글() throws Exception {
        long todo = create(alice, "우산 챙기기");
        long link = create(alice, "https://example.com 정리글");

        mockMvc.perform(as(bob, patch("/api/v1/todos/" + todo)).content("{\"value\":true}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(bob, patch("/api/v1/links/" + link)).content("{\"value\":true}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(alice, get("/api/v1/captures/" + todo)))
                .andExpect(jsonPath("$.todo.done").value(false));
    }

    @Test
    @DisplayName("목록은 전체·타입별 모두 내 것만 나온다")
    void 목록_격리() throws Exception {
        create(alice, "점심 9000원");
        create(alice, "우산 챙기기");
        create(alice, "https://example.com 정리글");
        create(bob, "저녁 12000원");

        mockMvc.perform(as(bob, get("/api/v1/captures")))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].rawText").value("저녁 12000원"));
        for (String type : new String[] {"EXPENSE", "TODO", "LINK"}) {
            mockMvc.perform(as(alice, get("/api/v1/captures?type=" + type)))
                    .andExpect(jsonPath("$.items.length()").value(1));
        }
        mockMvc.perform(as(bob, get("/api/v1/captures?type=TODO")))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("지출 요약은 내 지출만 더한다")
    void 요약_격리() throws Exception {
        create(alice, "점심 9000원");
        create(bob, "저녁 12000원");

        mockMvc.perform(as(bob, get("/api/v1/summary/expenses")))
                .andExpect(jsonPath("$.totalAmount").value(12000))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("게스트는 주인 기록을 못 보고, 주인은 게스트 기록을 못 본다")
    void 주인과_게스트() throws Exception {
        long mine = create(ownerToken, "주인 비밀 5000원");
        long guests = create(alice, "게스트 메모");

        mockMvc.perform(as(alice, get("/api/v1/captures/" + mine))).andExpect(status().isNotFound());
        mockMvc.perform(as(ownerToken, get("/api/v1/captures/" + guests))).andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("X-API-Token", token).contentType(MediaType.APPLICATION_JSON);
    }

    private long create(String token, String text) throws Exception {
        String body = mockMvc.perform(as(token, post("/api/v1/captures")).content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String issueGuest() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
