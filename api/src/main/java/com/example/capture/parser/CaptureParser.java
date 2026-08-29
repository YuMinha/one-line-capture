package com.example.capture.parser;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CaptureParser {

    private final List<ParseRule> rules;
    private final Clock clock;

    // 주입되는 List는 @Order 순으로 정렬된다. URL 안에는 숫자가 흔해서
    // ExpenseRule이 앞에 오면 링크가 지출로 분류된다 (stack.md §4)
    public CaptureParser(List<ParseRule> rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    public ParsedCapture parse(String rawText) {
        // 룰은 사용자 타임존의 벽시계로 계산한다. "내일 3시"의 3시는 KST 3시다
        LocalDateTime now = LocalDateTime.now(clock);

        ParsedCapture parsed = rules.stream()
                .map(rule -> rule.tryParse(rawText, now))
                .flatMap(Optional::stream)
                .findFirst()
                // 아무 룰에도 안 걸리면 할일로 떨어진다. 미분류함을 두지 않기로 했다 (spec.md §6)
                .orElseGet(() -> new ParsedCapture.Todo(rawText, null));

        return toUtc(parsed);
    }

    // 벽시계 → UTC 변환을 여기 한 곳에서만 한다. 서비스와 preview가 각자 변환하면
    // 반드시 한쪽을 빠뜨린다 (stack.md §2.2)
    private ParsedCapture toUtc(ParsedCapture parsed) {
        if (parsed instanceof ParsedCapture.Todo todo && todo.dueAt() != null) {
            return new ParsedCapture.Todo(todo.title(), todo.dueAt()
                    .atZone(clock.getZone())
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime());
        }
        // 지출의 spent_at은 날짜 컬럼이다. "오늘 쓴 돈"의 오늘은 KST 기준이 맞으므로 변환하지 않는다
        return parsed;
    }
}
