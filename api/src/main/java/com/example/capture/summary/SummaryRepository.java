package com.example.capture.summary;

import com.example.capture.capture.domain.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SummaryRepository extends Repository<Expense, Long> {

    // DATE_FORMAT(spent_at, '%Y-%m') = '2026-08' 으로 쓰면 컬럼에 함수가 걸려
    // idx_expense_spent_at을 못 탄다. 범위 조건으로 써야 인덱스를 탄다 (stack.md §3.5)
    @Query(value = """
            SELECT e.spent_at   AS spentDate,
                   SUM(e.amount) AS totalAmount,
                   COUNT(*)      AS entryCount
            FROM expense e
            WHERE e.spent_at >= :monthStart
              AND e.spent_at <  :nextMonthStart
            GROUP BY e.spent_at
            ORDER BY e.spent_at
            """, nativeQuery = true)
    List<DailyTotal> findDailyTotals(@Param("monthStart") LocalDate monthStart,
                                     @Param("nextMonthStart") LocalDate nextMonthStart);
}
