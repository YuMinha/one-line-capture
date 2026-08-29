package com.example.capture.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.capture.capture.domain.CaptureSource;
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
// 인증은 ApiTokenFilterTest가 따로 검증한다. 여기서는 끄고 기능만 본다
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class DetailToggleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaptureRepository captureRepository;

    @Autowired
    private TodoRepository todoRepository;

    private long save(String text) throws Exception {
        String body = mockMvc.perform(post("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + text + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.id", Integer.class);
    }

    private void toggle(String path, long id, boolean value, int expected) throws Exception {
        mockMvc.perform(patch("/api/v1/" + path + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":" + value + "}"))
                .andExpect(status().is(expected));
    }

    @Test
    @DisplayName("할일을 완료하면 done과 doneAt이 채워진다")
    void 할일_완료() throws Exception {
        long id = save("우산 챙기기");

        mockMvc.perform(patch("/api/v1/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todo.done").value(true));

        assertThat(todoRepository.findById(id).orElseThrow().getDoneAt()).isNotNull();
    }

    @Test
    @DisplayName("완료를 풀면 doneAt이 비워진다")
    void 완료_해제() throws Exception {
        long id = save("우산 챙기기");
        toggle("todos", id, true, 200);
        toggle("todos", id, false, 200);

        assertThat(todoRepository.findById(id).orElseThrow().isDone()).isFalse();
        assertThat(todoRepository.findById(id).orElseThrow().getDoneAt()).isNull();
    }

    @Test
    @DisplayName("완료 체크는 분류 수정이 아니므로 source는 AUTO 그대로다")
    void 완료는_source를_안_바꾼다() throws Exception {
        long id = save("우산 챙기기");
        toggle("todos", id, true, 200);

        assertThat(captureRepository.findById(id).orElseThrow().getSource()).isEqualTo(CaptureSource.AUTO);
    }

    @Test
    @DisplayName("같은 값을 여러 번 보내도 결과가 같다")
    void 재시도_안전() throws Exception {
        long id = save("우산 챙기기");
        toggle("todos", id, true, 200);
        toggle("todos", id, true, 200);

        assertThat(todoRepository.findById(id).orElseThrow().isDone()).isTrue();
    }

    @Test
    @DisplayName("링크 읽음을 켜면 readAt이 채워지고 끄면 비워진다")
    void 링크_읽음() throws Exception {
        long id = save("https://a.com 정리글");

        mockMvc.perform(patch("/api/v1/links/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link.readAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/links/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link.readAt").doesNotExist());
    }

    @Test
    @DisplayName("할일이 아닌 것을 완료하려 하면 404")
    void 타입이_다르면_404() throws Exception {
        long id = save("점심 9000원");
        toggle("todos", id, true, 404);
    }

    @Test
    @DisplayName("value가 없으면 400")
    void 값_누락() throws Exception {
        long id = save("우산 챙기기");
        mockMvc.perform(patch("/api/v1/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("done=false 필터가 완료된 할일을 걸러낸다")
    void 완료_필터() throws Exception {
        long done = save("우산 챙기기");
        save("내일 3시 과제 제출");
        toggle("todos", done, true, 200);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/captures").param("type", "TODO").param("done", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }
}
