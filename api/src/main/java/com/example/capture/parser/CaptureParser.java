package com.example.capture.parser;

import java.time.Clock;
import java.time.LocalDateTime;
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
        LocalDateTime now = LocalDateTime.now(clock);
        return rules.stream()
                .map(rule -> rule.tryParse(rawText, now))
                .flatMap(Optional::stream)
                .findFirst()
                // 아무 룰에도 안 걸리면 할일로 떨어진다. 미분류함을 두지 않기로 했다 (spec.md §6)
                .orElseGet(() -> new ParsedCapture.Todo(rawText, null));
    }
}
