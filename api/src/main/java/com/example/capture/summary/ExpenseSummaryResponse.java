package com.example.capture.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(
        String month,
        BigDecimal totalAmount,
        long count,
        List<Daily> dailyTotals
) {
    public record Daily(LocalDate date, BigDecimal amount, long count) {}
}
