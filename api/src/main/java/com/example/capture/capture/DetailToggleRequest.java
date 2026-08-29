package com.example.capture.capture;

import jakarta.validation.constraints.NotNull;

// 토글이지만 뒤집기가 아니라 원하는 값을 받는다. 재시도해도 결과가 같아야 한다
public record DetailToggleRequest(@NotNull Boolean value) {
}
