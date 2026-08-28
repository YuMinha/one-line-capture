package com.example.capture.parser.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.capture.capture.domain.CaptureType;
import com.example.capture.parser.ParsedCapture;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExpenseRuleTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 10, 0);

    private final ExpenseRule rule = new ExpenseRule();

    private ParsedCapture.Expense parse(String raw) {
        return (ParsedCapture.Expense) rule.tryParse(raw, NOW).orElseThrow();
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}원, 항목={2}")
    @CsvSource(delimiter = '|', value = {
            "점심 9000원      | 9000  | 점심",
            "스벅 5,500원     | 5500  | 스벅",
            "택시 1만원       | 10000 | 택시",
            "간식 5천원       | 5000  | 간식",
            "1.5만원 저녁     | 15000 | 저녁",
            "커피 4,000원     | 4000  | 커피",
            "9000원           | 9000  | ",
            "만원             | 10000 | ",
            "천원             | 1000  | ",
            "택시 1 만 원     | 10000 | 택시",
            "0원 무료샘플     | 0     | 무료샘플",
            "책 12,345원 구매 | 12345 | 책 구매",
    })
    @DisplayName("금액 패턴 4종을 인식하고 나머지를 항목으로 남긴다")
    void 금액_파싱(String raw, String expectedAmount, String expectedMerchant) {
        ParsedCapture.Expense expense = parse(raw.trim());

        assertThat(expense.amount()).isEqualByComparingTo(expectedAmount);
        assertThat(expense.merchant()).isEqualTo(expectedMerchant);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "우산 챙기기",
            "내일 3시 과제 제출",
            "9000",
            "원피스 사기",
            "",
    })
    @DisplayName("금액 표현이 없으면 이 룰은 손대지 않는다")
    void 금액_없으면_통과(String raw) {
        assertThat(rule.tryParse(raw, NOW)).isEmpty();
    }

    @Test
    @DisplayName("지출일은 입력일이다")
    void 지출일은_입력일() {
        assertThat(parse("점심 9000원").spentAt()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    @DisplayName("결과 타입은 EXPENSE다")
    void 타입은_EXPENSE() {
        assertThat(parse("점심 9000원").type()).isEqualTo(CaptureType.EXPENSE);
    }
}
