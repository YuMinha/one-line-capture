package com.example.capture.summary;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse ofMonth(YearMonth month) {
        List<ExpenseSummaryResponse.Daily> daily = summaryRepository
                .findDailyTotals(month.atDay(1), month.plusMonths(1).atDay(1))
                .stream()
                .map(row -> new ExpenseSummaryResponse.Daily(
                        row.getSpentDate(), normalize(row.getTotalAmount()), row.getEntryCount()))
                .toList();

        // 총액과 건수는 일별 결과를 더해서 낸다. 집계 쿼리를 한 번 더 날릴 이유가 없다
        BigDecimal total = daily.stream()
                .map(ExpenseSummaryResponse.Daily::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = daily.stream().mapToLong(ExpenseSummaryResponse.Daily::count).sum();

        return new ExpenseSummaryResponse(month.toString(), normalize(total), count, daily);
    }

    // DECIMAL(12,2)라 9000.00으로 온다. 원화 정수만 다루므로 0만 뗀다
    private BigDecimal normalize(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0 ? amount.setScale(0) : amount;
    }
}
