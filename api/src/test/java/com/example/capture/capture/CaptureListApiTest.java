package com.example.capture.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
// 인증은 ApiTokenFilterTest가 따로 검증한다. 여기서는 끄고 기능만 본다
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CaptureListApiTest {

    @Autowired
    private MockMvc mockMvc;

    private void save(String text) throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("타입 필터 없이 조회하면 최신순으로 요약만 내려온다")
    void 전체_조회는_요약만() throws Exception {
        save("점심 9000원");
        save("우산 챙기기");

        mockMvc.perform(get("/api/v1/captures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                // 최신순이므로 마지막에 저장한 것이 앞에 온다
                .andExpect(jsonPath("$.items[0].rawText").value("우산 챙기기"))
                .andExpect(jsonPath("$.items[1].rawText").value("점심 9000원"))
                // 타입이 섞인 목록은 상세를 내려주지 않는다
                .andExpect(jsonPath("$.items[1].expense").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("타입 필터를 주면 상세까지 함께 내려온다")
    void 타입_필터는_상세_포함() throws Exception {
        save("점심 9000원");
        save("우산 챙기기");
        save("스벅 5,500원");

        mockMvc.perform(get("/api/v1/captures").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].expense.amount").value(5500))
                .andExpect(jsonPath("$.items[1].expense.amount").value(9000))
                .andExpect(jsonPath("$.items[0].todo").doesNotExist());
    }

    @Test
    @DisplayName("size로 자르고 nextCursor로 다음 페이지를 이어 받는다")
    void 커서_페이징() throws Exception {
        for (int i = 1; i <= 7; i++) {
            save("항목" + i + " " + (i * 1000) + "원");
        }

        String response = mockMvc.perform(get("/api/v1/captures")
                        .param("type", "EXPENSE").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNumber())
                .andReturn().getResponse().getContentAsString();

        long cursor = com.jayway.jsonpath.JsonPath.parse(response).read("$.nextCursor", Integer.class);

        mockMvc.perform(get("/api/v1/captures")
                        .param("type", "EXPENSE").param("size", "5")
                        .param("cursor", String.valueOf(cursor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                // 커서보다 작은 id만 온다 = 중복 없음
                .andExpect(jsonPath("$.items[0].id").value(org.hamcrest.Matchers.lessThan((int) cursor)));
    }

    @Test
    @DisplayName("size는 50을 넘지 못한다")
    void size_상한() throws Exception {
        save("점심 9000원");

        mockMvc.perform(get("/api/v1/captures").param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("done=false면 미완료 할일만 온다")
    void 할일_완료_필터() throws Exception {
        save("우산 챙기기");
        save("내일 3시 과제 제출");

        mockMvc.perform(get("/api/v1/captures").param("type", "TODO").param("done", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/captures").param("type", "TODO").param("done", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("빈 목록도 정상 응답이다")
    void 빈_목록() throws Exception {
        mockMvc.perform(get("/api/v1/captures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }
}
