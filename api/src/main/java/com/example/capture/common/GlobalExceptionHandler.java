package com.example.capture.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// T3.9에서 에러 응답 포맷을 전부 통일한다. 지금은 요청 검증만 (stack.md §3.2)
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(String code, String message) {}

    public record ErrorResponse(ErrorBody error) {
        static ErrorResponse of(String code, String message) {
            return new ErrorResponse(new ErrorBody(code, message));
        }
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> "Size".equals(fieldError.getCode())
                        ? ErrorResponse.of("TEXT_TOO_LONG", "text는 500자를 넘을 수 없습니다")
                        : ErrorResponse.of("TEXT_REQUIRED", "text는 비어 있을 수 없습니다"))
                .orElseGet(() -> ErrorResponse.of("INVALID_REQUEST", "요청이 올바르지 않습니다"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException e) {
        return ErrorResponse.of("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다");
    }
}
