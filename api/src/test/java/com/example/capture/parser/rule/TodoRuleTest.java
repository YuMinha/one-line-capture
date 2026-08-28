package com.example.capture.parser.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.capture.capture.domain.CaptureType;
import com.example.capture.parser.ParsedCapture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TodoRuleTest {

    // 2026-08-25는 화요일. 요일 케이스의 기대값이 여기서 나온다
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 10, 0);

    private final TodoRule rule = new TodoRule();

    private ParsedCapture.Todo parse(String raw) {
        return (ParsedCapture.Todo) rule.tryParse(raw, NOW).orElseThrow();
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "오늘 장보기        | 2026-08-25T09:00 | 장보기",
            "내일 과제 제출     | 2026-08-26T09:00 | 과제 제출",
            "모레 발표          | 2026-08-27T09:00 | 발표",
            "다음주 회의        | 2026-09-01T09:00 | 회의",
            "다음 주 회의       | 2026-09-01T09:00 | 회의",
    })
    @DisplayName("상대 날짜 - 오늘/내일/모레/다음주")
    void 상대_날짜(String raw, String expectedDue, String expectedTitle) {
        ParsedCapture.Todo todo = parse(raw.trim());

        assertThat(todo.dueAt()).isEqualTo(LocalDateTime.parse(expectedDue));
        assertThat(todo.title()).isEqualTo(expectedTitle);
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "9/2 팀플 발표      | 2026-09-02T09:00 | 팀플 발표",
            "9월 2일 팀플 발표  | 2026-09-02T09:00 | 팀플 발표",
            "12/31 정산         | 2026-12-31T09:00 | 정산",
            "8월 25일 오늘것    | 2026-08-25T09:00 | 오늘것",
    })
    @DisplayName("절대 날짜 - 9/2, 9월 2일")
    void 절대_날짜(String raw, String expectedDue, String expectedTitle) {
        ParsedCapture.Todo todo = parse(raw.trim());

        assertThat(todo.dueAt()).isEqualTo(LocalDateTime.parse(expectedDue));
        assertThat(todo.title()).isEqualTo(expectedTitle);
    }

    @Test
    @DisplayName("이미 지난 날짜는 내년으로 본다")
    void 지난_날짜는_내년() {
        assertThat(parse("1/5 신년계획").dueAt()).isEqualTo(LocalDateTime.of(2027, 1, 5, 9, 0));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "금요일 회식         | 2026-08-28T09:00 | 회식",
            "이번주 금요일 회식  | 2026-08-28T09:00 | 회식",
            "월요일 제출         | 2026-08-31T09:00 | 제출",
            "화요일 정기회의     | 2026-08-25T09:00 | 정기회의",
    })
    @DisplayName("요일 - 오늘 포함 가장 가까운 그 요일")
    void 요일(String raw, String expectedDue, String expectedTitle) {
        ParsedCapture.Todo todo = parse(raw.trim());

        assertThat(todo.dueAt()).isEqualTo(LocalDateTime.parse(expectedDue));
        assertThat(todo.title()).isEqualTo(expectedTitle);
    }

    @Test
    @DisplayName("날짜 표현만 있으면 제목은 원문 그대로 남는다")
    void 제목이_비면_원문() {
        assertThat(parse("내일").title()).isEqualTo("내일");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "우산 챙기기",
            "점심 9000원",
            "2/30 없는날",
            "",
    })
    @DisplayName("날짜 표현이 없거나 없는 날짜면 이 룰은 손대지 않는다")
    void 날짜_없으면_통과(String raw) {
        assertThat(rule.tryParse(raw, NOW)).isEmpty();
    }

    @Test
    @DisplayName("결과 타입은 TODO다")
    void 타입은_TODO() {
        assertThat(parse("내일 과제").type()).isEqualTo(CaptureType.TODO);
    }
}
