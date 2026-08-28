package com.example.capture.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.example.capture.capture.domain.Todo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// @Transactional이라 테스트가 끝나면 롤백된다. 개발용 DB를 그대로 쓰므로 이게 없으면
// 테스트를 돌릴 때마다 실제 데이터가 쌓인다
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaptureApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaptureRepository captureRepository;

    @Autowired
    private TodoRepository todoRepository;

    private String body(String text) {
        return "{\"text\":\"" + text + "\"}";
    }

    @Test
    @DisplayName("한 줄을 보내면 201과 함께 capture/todo 행이 생긴다")
    void 저장_성공() throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("우산 챙기기")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("TODO"))
                .andExpect(jsonPath("$.source").value("AUTO"))
                .andExpect(jsonPath("$.rawText").value("우산 챙기기"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.todo.title").value("우산 챙기기"))
                .andExpect(jsonPath("$.todo.dueAt").doesNotExist())
                .andExpect(jsonPath("$.todo.done").value(false));

        List<Capture> captures = captureRepository.findAll();
        assertThat(captures).hasSize(1);
        Capture saved = captures.get(0);
        assertThat(saved.getRawText()).isEqualTo("우산 챙기기");
        assertThat(saved.getType()).isEqualTo(CaptureType.TODO);
        assertThat(saved.getSource()).isEqualTo(CaptureSource.AUTO);

        Todo todo = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(todo.getTitle()).isEqualTo("우산 챙기기");
        assertThat(todo.getDueAt()).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백은 제거하고 저장한다")
    void 공백_제거() throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  우산 챙기기  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawText").value("우산 챙기기"));
    }

    @Test
    @DisplayName("공백만 있는 문자열은 400 TEXT_REQUIRED")
    void 빈_문자열_거부() throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TEXT_REQUIRED"));

        assertThat(captureRepository.count()).isZero();
    }

    @Test
    @DisplayName("501자는 400 TEXT_TOO_LONG")
    void 길이_초과_거부() throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("가".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TEXT_TOO_LONG"));

        assertThat(captureRepository.count()).isZero();
    }

    // title은 200자, raw_text는 500자다. 이 경계에서 Data truncation으로 500이 났었다
    @Test
    @DisplayName("500자는 저장되고 raw_text는 온전하되 title만 200자로 잘린다")
    void 경계_500자() throws Exception {
        mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("가".repeat(500))))
                .andExpect(status().isCreated());

        Capture saved = captureRepository.findAll().get(0);
        assertThat(saved.getRawText()).hasSize(500);
        assertThat(todoRepository.findById(saved.getId()).orElseThrow().getTitle()).hasSize(200);
    }
}
