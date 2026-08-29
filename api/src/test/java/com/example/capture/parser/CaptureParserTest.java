package com.example.capture.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import com.example.capture.parser.rule.TodoRule;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CaptureParserTest {

    private static final Instant FIXED = Instant.parse("2026-08-25T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED, ZoneOffset.UTC);

    @Test
    @DisplayName("룰이 하나도 없으면 마감 없는 TODO로 떨어진다")
    void 룰이_없으면_TODO_fallback() {
        CaptureParser parser = new CaptureParser(List.of(), CLOCK);

        ParsedCapture result = parser.parse("우산 챙기기");

        assertThat(result).isInstanceOf(ParsedCapture.Todo.class);
        ParsedCapture.Todo todo = (ParsedCapture.Todo) result;
        assertThat(todo.title()).isEqualTo("우산 챙기기");
        assertThat(todo.dueAt()).isNull();
    }

    @Test
    @DisplayName("먼저 성공한 룰을 채택하고 뒤의 룰은 보지 않는다")
    void 첫_성공_룰_채택() {
        ParseRule 항상실패 = (raw, now) -> Optional.empty();
        ParseRule 링크 = (raw, now) -> Optional.of(new ParsedCapture.Link("https://a.com", null));
        ParseRule 지출 = (raw, now) -> Optional.of(new ParsedCapture.Expense(null, "뒤에있는룰", null));

        CaptureParser parser = new CaptureParser(List.of(항상실패, 링크, 지출), CLOCK);

        assertThat(parser.parse("아무거나")).isInstanceOf(ParsedCapture.Link.class);
    }

    @Test
    @DisplayName("룰에 넘어가는 now는 Clock에서 온다")
    void now는_Clock에서_온다() {
        LocalDateTime[] seen = new LocalDateTime[1];
        ParseRule 엿보기 = (raw, now) -> {
            seen[0] = now;
            return Optional.empty();
        };

        new CaptureParser(List.of(엿보기), CLOCK).parse("아무거나");

        assertThat(seen[0]).isEqualTo(LocalDateTime.of(2026, 8, 25, 1, 0));
    }

    // 이 테스트가 잡는 버그: Clock을 systemUTC로 두면 KST 새벽에 "내일"이 하루 덜 밀리고
    // 3시가 UTC 15시(= KST 자정)로 계산돼 마감이 15시간 어긋난다 (stack.md §2.2)
    @Test
    @DisplayName("KST 새벽에 넣은 '내일 3시'는 KST 다음날 15시로, UTC로 변환돼 나온다")
    void KST_새벽의_내일_3시() {
        // 2026-08-29T16:13Z = KST 2026-08-30 01:13
        Clock kst = Clock.fixed(Instant.parse("2026-08-29T16:13:00Z"), ZoneId.of("Asia/Seoul"));
        CaptureParser parser = new CaptureParser(List.of(new TodoRule()), kst);

        ParsedCapture.Todo todo = (ParsedCapture.Todo) parser.parse("내일 3시 과제 제출");

        // 사용자 의도: KST 2026-08-31 15:00 → UTC 2026-08-31T06:00
        assertThat(todo.dueAt()).isEqualTo(LocalDateTime.of(2026, 8, 31, 6, 0));
        assertThat(todo.title()).isEqualTo("과제 제출");
    }

    @Test
    @DisplayName("마감이 없으면 변환할 것도 없다")
    void 마감_없으면_그대로() {
        Clock kst = Clock.fixed(Instant.parse("2026-08-29T16:13:00Z"), ZoneId.of("Asia/Seoul"));
        CaptureParser parser = new CaptureParser(List.of(new TodoRule()), kst);

        assertThat(((ParsedCapture.Todo) parser.parse("우산 챙기기")).dueAt()).isNull();
    }
}
