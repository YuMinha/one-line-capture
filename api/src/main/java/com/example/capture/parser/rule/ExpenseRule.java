package com.example.capture.parser.rule;

import com.example.capture.parser.ParseRule;
import com.example.capture.parser.ParsedCapture;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ExpenseRule implements ParseRule {

    // 만원/천원을 먼저 본다. "1만원"을 일반 패턴이 먼저 집으면 1원이 된다
    private static final List<Unit> UNITS = List.of(
            new Unit(Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*만\\s*원"), BigDecimal.valueOf(10000)),
            new Unit(Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*천\\s*원"), BigDecimal.valueOf(1000)),
            new Unit(Pattern.compile("(?<![\\d.])(만)\\s*원"), BigDecimal.valueOf(10000)),
            new Unit(Pattern.compile("(?<![\\d.])(천)\\s*원"), BigDecimal.valueOf(1000)),
            new Unit(Pattern.compile("(\\d{1,3}(?:,\\d{3})+|\\d+)\\s*원"), BigDecimal.ONE)
    );

    private record Unit(Pattern pattern, BigDecimal multiplier) {}

    @Override
    public Optional<ParsedCapture> tryParse(String raw, LocalDateTime now) {
        for (Unit unit : UNITS) {
            Matcher matcher = unit.pattern().matcher(raw);
            if (!matcher.find()) {
                continue;
            }
            BigDecimal amount = toAmount(matcher.group(1)).multiply(unit.multiplier());
            String merchant = removeRange(raw, matcher.start(), matcher.end());
            // 지출일은 입력일이다. 텍스트에서 날짜를 읽는 건 v1 범위 밖 (spec.md §7)
            return Optional.of(new ParsedCapture.Expense(amount, merchant, now.toLocalDate()));
        }
        return Optional.empty();
    }

    // "만원"/"천원"처럼 숫자가 없으면 group(1)이 단위 글자 자체다
    private BigDecimal toAmount(String captured) {
        if ("만".equals(captured) || "천".equals(captured)) {
            return BigDecimal.ONE;
        }
        return new BigDecimal(captured.replace(",", ""));
    }

    private String removeRange(String raw, int start, int end) {
        String rest = (raw.substring(0, start) + " " + raw.substring(end))
                .replaceAll("\\s+", " ")
                .trim();
        return rest.isEmpty() ? null : rest;
    }
}
