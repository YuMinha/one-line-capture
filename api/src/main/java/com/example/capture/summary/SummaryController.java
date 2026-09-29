package com.example.capture.summary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import com.example.capture.common.ApiException;
import com.example.capture.common.ApiTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;
    private final Clock clock;

    @GetMapping("/expenses")
    public ExpenseSummaryResponse expenses(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId, @RequestParam(required = false) String month) {
        return summaryService.ofMonth(userId, parse(month));
    }

    private YearMonth parse(String month) {
        // 기본값은 이번 달이다. spent_at이 KST 날짜이므로 "이번 달"도 KST 기준이어야 한다
        if (month == null || month.isBlank()) {
            return YearMonth.from(LocalDate.now(clock));
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("INVALID_MONTH", "month는 2026-08 형식이어야 합니다");
        }
    }
}
