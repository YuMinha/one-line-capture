package com.example.capture.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 에러 코드와 상태를 예외가 직접 들고 다닌다. 핸들러가 예외 종류마다
// 분기하지 않아도 되고, 새 에러를 추가할 때 고칠 곳이 한 군데도 없다
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
