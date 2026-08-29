package com.example.capture.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// 에러 응답은 어떤 경로로 나가든 { "error": { "code", "message" } } 하나뿐이다.
// 프론트가 분기할 모양이 하나면 에러 처리가 한 곳으로 모인다 (stack.md §3.2)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    // ?type=BOGUS 처럼 enum·숫자 변환이 실패하는 경우
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadParameter(Exception e) {
        return ErrorResponse.of("INVALID_PARAMETER", "요청 파라미터가 올바르지 않습니다");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NoResourceFoundException e) {
        return ErrorResponse.of("NOT_FOUND", "없는 경로입니다");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethod(HttpRequestMethodNotSupportedException e) {
        return ErrorResponse.of("METHOD_NOT_ALLOWED", "허용되지 않은 메서드입니다");
    }

    // 마지막 그물. 스택트레이스는 로그에만 남기고 사용자에게는 내보내지 않는다
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ErrorResponse.of("INTERNAL_ERROR", "서버에서 문제가 발생했습니다");
    }
}
