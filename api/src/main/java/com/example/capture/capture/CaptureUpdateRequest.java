package com.example.capture.capture;

import com.example.capture.capture.domain.CaptureType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

// rawText는 받지 않는다. 원문은 절대 바뀌지 않는다 (stack.md §3.4)
public record CaptureUpdateRequest(
        @NotNull CaptureType type,
        @Valid ExpenseDetail expense,
        @Valid TodoDetail todo,
        @Valid LinkDetail link
) {
    public record ExpenseDetail(
            @NotNull @PositiveOrZero BigDecimal amount,
            @Size(max = 100) String merchant,
            @NotNull LocalDate spentAt
    ) {}

    public record TodoDetail(
            @NotBlank @Size(max = 200) String title,
            Instant dueAt
    ) {}

    public record LinkDetail(
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 300) String note
    ) {}
}
