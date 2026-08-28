package com.example.capture.parser.rule;

import com.example.capture.parser.ParseRule;
import com.example.capture.parser.ParsedCapture;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class TodoRule implements ParseRule {

    // 날짜만 있고 시각이 없으면 그날 09:00. 이 기본값은 문서에 적고 테스트로 고정한다 (stack.md §4)
    private static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

    private static final Pattern ABSOLUTE_KOREAN = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern ABSOLUTE_SLASH = Pattern.compile("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?!\\d)");
    private static final Pattern RELATIVE = Pattern.compile("(오늘|내일|모레|다음\\s*주)");
    private static final Pattern WEEKDAY = Pattern.compile("(?:이번\\s*주\\s*)?([월화수목금토일])요일");

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "월", DayOfWeek.MONDAY, "화", DayOfWeek.TUESDAY, "수", DayOfWeek.WEDNESDAY,
            "목", DayOfWeek.THURSDAY, "금", DayOfWeek.FRIDAY, "토", DayOfWeek.SATURDAY,
            "일", DayOfWeek.SUNDAY);

    @Override
    public Optional<ParsedCapture> tryParse(String raw, LocalDateTime now) {
        for (Pattern pattern : List.of(ABSOLUTE_KOREAN, ABSOLUTE_SLASH, RELATIVE, WEEKDAY)) {
            Matcher matcher = pattern.matcher(raw);
            if (!matcher.find()) {
                continue;
            }
            Optional<LocalDate> date = toDate(pattern, matcher, now.toLocalDate());
            if (date.isEmpty()) {
                continue;
            }
            return Optional.of(new ParsedCapture.Todo(
                    title(raw, matcher.start(), matcher.end()),
                    date.get().atTime(DEFAULT_TIME)));
        }
        return Optional.empty();
    }

    private Optional<LocalDate> toDate(Pattern pattern, Matcher matcher, LocalDate today) {
        if (pattern == ABSOLUTE_KOREAN || pattern == ABSOLUTE_SLASH) {
            return absolute(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), today);
        }
        if (pattern == RELATIVE) {
            return Optional.of(relative(matcher.group(1), today));
        }
        return Optional.of(nextWeekday(DAYS.get(matcher.group(1)), today));
    }

    // 2/30 같은 없는 날짜는 이 룰이 손대지 않고 다음 룰/fallback으로 넘긴다
    private Optional<LocalDate> absolute(int month, int day, LocalDate today) {
        try {
            LocalDate candidate = LocalDate.of(today.getYear(), month, day);
            // 이미 지난 날짜면 내년으로 본다. 12월에 "1/5"라고 쓰면 내년 1월이다
            return Optional.of(candidate.isBefore(today) ? candidate.plusYears(1) : candidate);
        } catch (java.time.DateTimeException e) {
            return Optional.empty();
        }
    }

    private LocalDate relative(String word, LocalDate today) {
        return switch (word.replaceAll("\\s+", "")) {
            case "오늘" -> today;
            case "내일" -> today.plusDays(1);
            case "모레" -> today.plusDays(2);
            default -> today.plusDays(7);
        };
    }

    // 오늘 포함 가장 가까운 그 요일. "이번주 금요일"과 "금요일"을 구분하지 않는다
    private LocalDate nextWeekday(DayOfWeek target, LocalDate today) {
        int gap = (target.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        return today.plusDays(gap);
    }

    // 날짜 표현을 지우면 제목이 빈 문자열이 될 수 있다. title은 NOT NULL이라 원문으로 되돌린다
    private String title(String raw, int start, int end) {
        String rest = (raw.substring(0, start) + " " + raw.substring(end))
                .replaceAll("\\s+", " ")
                .trim();
        return rest.isEmpty() ? raw.trim() : rest;
    }
}
