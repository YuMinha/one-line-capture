package com.example.capture.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
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
class CaptureUpdateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaptureRepository captureRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private EntityManager entityManager;

    private long save(String text) throws Exception {
        String body = mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.id", Integer.class);
    }

    private void patchJson(long id, String json, int expectedStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/captures/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("EXPENSE를 TODO로 바꾸면 expense 행이 사라지고 todo 행이 생긴다")
    void 타입_변경() throws Exception {
        long id = save("점심 9000원");
        assertThat(expenseRepository.findById(id)).isPresent();

        mockMvc.perform(patch("/api/v1/captures/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"TODO","todo":{"title":"점심 약속","dueAt":"2026-08-26T04:00:00Z"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TODO"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.todo.title").value("점심 약속"))
                .andExpect(jsonPath("$.todo.dueAt").value("2026-08-26T04:00:00Z"))
                .andExpect(jsonPath("$.expense").doesNotExist());

        assertThat(expenseRepository.findById(id)).isEmpty();
        assertThat(todoRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("원문은 어떤 경우에도 바뀌지 않는다")
    void 원문_불변() throws Exception {
        long id = save("점심 9000원");

        patchJson(id, """
                {"type":"TODO","todo":{"title":"완전히 다른 제목"}}
                """, 200);

        assertThat(captureRepository.findById(id).orElseThrow().getRawText()).isEqualTo("점심 9000원");
    }

    @Test
    @DisplayName("타입이 그대로면 상세 필드만 바뀌고 행은 그대로다")
    void 필드만_수정() throws Exception {
        long id = save("점심 9000원");

        mockMvc.perform(patch("/api/v1/captures/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","expense":{"amount":12000,"merchant":"저녁","spentAt":"2026-08-27"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expense.amount").value(12000))
                .andExpect(jsonPath("$.expense.merchant").value("저녁"))
                .andExpect(jsonPath("$.expense.spentAt").value("2026-08-27"));

        assertThat(expenseRepository.findById(id)).isPresent();
        assertThat(todoRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("타입을 안 바꿔도 사용자가 손대면 source는 MANUAL이 된다")
    void source는_MANUAL() throws Exception {
        long id = save("점심 9000원");
        assertThat(captureRepository.findById(id).orElseThrow().getSource()).isEqualTo(CaptureSource.AUTO);

        patchJson(id, """
                {"type":"EXPENSE","expense":{"amount":9000,"merchant":"점심","spentAt":"2026-08-25"}}
                """, 200);

        assertThat(captureRepository.findById(id).orElseThrow().getSource()).isEqualTo(CaptureSource.MANUAL);
    }

    @Test
    @DisplayName("TODO를 LINK로 바꾸는 것도 된다")
    void 할일을_링크로() throws Exception {
        long id = save("우산 챙기기");

        patchJson(id, """
                {"type":"LINK","link":{"url":"https://a.com","note":"메모"}}
                """, 200);

        assertThat(todoRepository.findById(id)).isEmpty();
        assertThat(linkRepository.findById(id)).isPresent();
        Capture capture = captureRepository.findById(id).orElseThrow();
        assertThat(capture.getType()).isEqualTo(CaptureType.LINK);
    }

    @Test
    @DisplayName("타입에 맞는 상세를 안 주면 400")
    void 상세_누락() throws Exception {
        long id = save("점심 9000원");

        mockMvc.perform(patch("/api/v1/captures/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TODO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DETAIL_REQUIRED"));
    }

    @Test
    @DisplayName("없는 id는 404")
    void 없는_id() throws Exception {
        mockMvc.perform(patch("/api/v1/captures/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TODO\",\"todo\":{\"title\":\"x\"}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CAPTURE_NOT_FOUND"));
    }

    @Test
    @DisplayName("삭제하면 상세도 함께 사라진다")
    void 삭제는_CASCADE() throws Exception {
        long id = save("점심 9000원");

        mockMvc.perform(delete("/api/v1/captures/" + id))
                .andExpect(status().isNoContent());

        // 상세는 JPA가 아니라 DB의 ON DELETE CASCADE가 지운다. flush로 삭제를 DB에 보내고
        // clear로 영속성 컨텍스트를 비워야 DB가 실제로 지웠는지를 확인할 수 있다
        entityManager.flush();
        entityManager.clear();

        assertThat(captureRepository.findById(id)).isEmpty();
        assertThat(expenseRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("없는 것을 지우면 404")
    void 없는것_삭제() throws Exception {
        mockMvc.perform(delete("/api/v1/captures/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("단건 조회는 타입에 맞는 상세를 함께 준다")
    void 단건_조회() throws Exception {
        long id = save("점심 9000원");

        mockMvc.perform(get("/api/v1/captures/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.expense.amount").value(9000))
                .andExpect(jsonPath("$.todo").doesNotExist());
    }

    @Test
    @DisplayName("없는 단건은 404")
    void 없는_단건() throws Exception {
        mockMvc.perform(get("/api/v1/captures/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CAPTURE_NOT_FOUND"));
    }
}
