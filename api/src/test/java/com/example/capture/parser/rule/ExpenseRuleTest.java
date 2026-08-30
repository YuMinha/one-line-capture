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
            // 원 + 아라비아 숫자
            "점심 9000원        | 9000   | 점심",
            "스벅 5,500원       | 5500   | 스벅",
            "커피 4,000원       | 4000   | 커피",
            "책 12,345원 구매   | 12345  | 책 구매",
            "9000 원            | 9000   | ",
            "0원 무료샘플       | 0      | 무료샘플",
            // 원 + 단위
            "택시 1만원         | 10000  | 택시",
            "간식 5천원         | 5000   | 간식",
            "1.5만원 저녁       | 15000  | 저녁",
            "월세 12만원        | 120000 | 월세",
            "만원               | 10000  | ",
            "천원               | 1000   | ",
            "택시 1 만 원       | 10000  | 택시",
            // 복합 단위
            "저녁 1만 5천원     | 15000  | 저녁",
            "저녁 1만5천원      | 15000  | 저녁",
            "회식 3만 2천원     | 32000  | 회식",
            // 한글 수사
            "점심 오백원        | 500    | 점심",
            "간식 오천원        | 5000   | 간식",
            "저녁 만 오천원     | 15000  | 저녁",
            "저녁 만오천원      | 15000  | 저녁",
            "월세 삼십만원      | 300000 | 월세",
            "회식 이만 삼천원   | 23000  | 회식",
            "커피 사천오백원    | 4500   | 커피",
            "주차 백원          | 100    | 주차",
            "택시 십만원        | 100000 | 택시",
            // 통화 기호
            "점심 ₩9,000        | 9000   | 점심",
            "점심 ₩ 9000        | 9000   | 점심",
            "₩12000 택시        | 12000  | 택시",
            // 원 없이 숫자만
            "점심 9,000         | 9000   | 점심",
            "점심 9000          | 9000   | 점심",
            "택시 12000         | 12000  | 택시",
            "스벅 5,500         | 5500   | 스벅",
    })
    @DisplayName("사람들이 실제로 쓰는 금액 표기를 읽는다")
    void 금액_파싱(String raw, String expectedAmount, String expectedMerchant) {
        ParsedCapture.Expense expense = parse(raw.trim());

        assertThat(expense.amount()).isEqualByComparingTo(expectedAmount);
        assertThat(expense.merchant()).isEqualTo(expectedMerchant);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "우산 챙기기",
            "원피스 사기",
            "",
            // 날짜·시각 표현이 있으면 맨 숫자를 금액으로 보지 않는다
            "내일 3시 과제 제출",
            "9/2 팀플 발표",
            "3시30분 통화",
            "다음주 회의",
            "오늘 100 계단 오르기",
            "금요일 회식 예약",
            // 단위가 붙은 숫자는 금액이 아니다
            "회의실 3층 예약",
            "10번 버스 타기",
            "2026년 계획 세우기",
            "사과 500개 주문",
            "손님 20명 예약",
            // 두 자리 이하 맨 숫자는 근거가 약해 건드리지 않는다
            "방 12 청소",
    })
    @DisplayName("금액 표현이 아니면 이 룰은 손대지 않는다")
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
