package com.example.capture.parser.rule;

import com.example.capture.parser.ParseRule;
import com.example.capture.parser.ParsedCapture;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// 링크 판정이 가장 확실하므로 맨 앞. URL 안에는 숫자가 흔해서(.../post/9000)
// ExpenseRule이 먼저 오면 링크가 지출로 분류된다 (stack.md §4)
@Component
@Order(1)
public class LinkRule implements ParseRule {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    @Override
    public Optional<ParsedCapture> tryParse(String raw, LocalDateTime now) {
        Matcher matcher = URL.matcher(raw);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String url = matcher.group();
        // URL 앞뒤에 남은 텍스트가 메모다. 가운데가 비면 토막이 붙으므로 공백으로 잇는다
        String note = (raw.substring(0, matcher.start()) + " " + raw.substring(matcher.end()))
                .replaceAll("\\s+", " ")
                .trim();

        return Optional.of(new ParsedCapture.Link(url, note.isEmpty() ? null : note));
    }
}
