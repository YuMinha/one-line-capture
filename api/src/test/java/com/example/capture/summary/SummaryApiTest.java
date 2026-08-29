package com.example.capture.summary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SummaryApiTest {

    @Autowired
    private MockMvc mockMvc;

    // 지출일을 원하는 날짜로 두려면 저장 후 PATCH로 옮긴다. 파서는 항상 오늘로 찍는다
    private void expense(int amount, String spentAt) throws Exception {
        String body = mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"항목 " + amount + "원\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.parse(body).read("$.id", Integer.class);

        mockMvc.perform(patch("/api/v1/captures/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EXPENSE\",\"expense\":{\"amount\":" + amount
                                + ",\"merchant\":\"항목\",\"spentAt\":\"" + spentAt + "\"}}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("월 총액·건수·일별이 나온다")
    void 월별_요약() throws Exception {
        expense(9000, "2026-07-01");
        expense(3000, "2026-07-01");
        expense(12000, "2026-07-15");

        mockMvc.perform(get("/api/v1/summary/expenses").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-07"))
                .andExpect(jsonPath("$.totalAmount").value(24000))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.dailyTotals.length()").value(2))
                .andExpect(jsonPath("$.dailyTotals[0].date").value("2026-07-01"))
                .andExpect(jsonPath("$.dailyTotals[0].amount").value(12000))
                .andExpect(jsonPath("$.dailyTotals[0].count").value(2))
                .andExpect(jsonPath("$.dailyTotals[1].date").value("2026-07-15"))
                .andExpect(jsonPath("$.dailyTotals[1].amount").value(12000));
    }

    @Test
    @DisplayName("월 경계 - 앞뒤 달의 지출은 섞이지 않는다")
    void 월_경계() throws Exception {
        expense(1000, "2026-06-30");
        expense(2000, "2026-07-01");
        expense(4000, "2026-07-31");
        expense(8000, "2026-08-01");

        mockMvc.perform(get("/api/v1/summary/expenses").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(6000))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("지출이 없는 달은 0으로 답한다")
    void 빈_달() throws Exception {
        mockMvc.perform(get("/api/v1/summary/expenses").param("month", "2020-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.dailyTotals.length()").value(0));
    }

    @Test
    @DisplayName("할일과 링크는 집계에 들어가지 않는다")
    void 지출만_센다() throws Exception {
        expense(5000, "2026-07-10");
        mockMvc.perform(post("/api/v1/captures").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"우산 챙기기\"}")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/summary/expenses").param("month", "2026-07"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("month를 안 주면 이번 달이다")
    void 기본값은_이번달() throws Exception {
        mockMvc.perform(get("/api/v1/summary/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").isNotEmpty());
    }

    @Test
    @DisplayName("형식이 틀린 month는 400")
    void 잘못된_month() throws Exception {
        mockMvc.perform(get("/api/v1/summary/expenses").param("month", "2026년 8월"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_MONTH"));
    }
}
