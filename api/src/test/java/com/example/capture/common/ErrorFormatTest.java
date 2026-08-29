package com.example.capture.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// 어떤 에러든 같은 모양으로 나가야 프론트가 한 곳에서만 처리한다
@SpringBootTest
// 인증은 ApiTokenFilterTest가 따로 검증한다. 여기서는 끄고 기능만 본다
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ErrorFormatTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("없는 경로도 error 포맷으로 답한다")
    void 없는_경로() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    @DisplayName("허용되지 않은 메서드도 error 포맷이다")
    void 잘못된_메서드() throws Exception {
        mockMvc.perform(put("/api/v1/captures")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("알 수 없는 타입 파라미터는 400 error 포맷이다")
    void 잘못된_파라미터() throws Exception {
        mockMvc.perform(get("/api/v1/captures").param("type", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("cursor에 숫자가 아닌 값이 오면 400이다")
    void 잘못된_커서() throws Exception {
        mockMvc.perform(get("/api/v1/captures").param("cursor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("헬스체크는 인증 없이도 열려 있다")
    void 헬스체크() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
