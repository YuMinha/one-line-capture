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

    // 오전/오후 + N시 + M분. "3시30분", "오후 3시", "15시"를 모두 잡는다
    private static final Pattern TIME =
            Pattern.compile("(오전|오후)?\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?");

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "월", DayOfWeek.MONDAY, "화", DayOfWeek.TUESDAY, "수", DayOfWeek.WEDNESDAY,
            "목", DayOfWeek.THURSDAY, "금", DayOfWeek.FRIDAY, "토", DayOfWeek.SATURDAY,
            "일", DayOfWeek.SUNDAY);

    @Override
    public Optional<ParsedCapture> tryParse(String raw, LocalDateTime now) {
        Optional<Match<LocalTime>> time = findTime(raw);
        Optional<Match<LocalDate>> date = findDate(raw, now.toLocalDate(), time);

        if (date.isEmpty() && time.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime dueAt = date
                .map(d -> d.value().atTime(time.map(Match::value).orElse(DEFAULT_TIME)))
                // 시각만 있으면 오늘. 이미 지났으면 내일로 민다
                .orElseGet(() -> onTodayOrTomorrow(time.orElseThrow().value(), now));

        return Optional.of(new ParsedCapture.Todo(titleWithout(raw, date, time), dueAt));
    }

    private record Match<T>(T value, int start, int end) {}

    private Optional<Match<LocalDate>> findDate(String raw, LocalDate today, Optional<Match<LocalTime>> time) {
        for (Pattern pattern : List.of(ABSOLUTE_KOREAN, ABSOLUTE_SLASH, RELATIVE, WEEKDAY)) {
            Matcher matcher = pattern.matcher(raw);
            while (matcher.find()) {
                // "3시30분"의 30을 9/2 같은 날짜로 오해하지 않도록 시각 구간과 겹치면 건너뛴다
                if (time.filter(t -> matcher.start() < t.end() && t.start() < matcher.end()).isPresent()) {
                    continue;
                }
                Optional<LocalDate> value = toDate(pattern, matcher, today);
                if (value.isPresent()) {
                    return Optional.of(new Match<>(value.get(), matcher.start(), matcher.end()));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Match<LocalTime>> findTime(String raw) {
        Matcher matcher = TIME.matcher(raw);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int hour = Integer.parseInt(matcher.group(2));
        int minute = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        String meridiem = matcher.group(1);
        if ("오후".equals(meridiem) && hour < 12) {
            hour += 12;
        } else if ("오전".equals(meridiem) && hour == 12) {
            hour = 0;
        } else if (meridiem == null && hour >= 1 && hour <= 11) {
            // spec.md §3: "내일 3시 과제 제출"은 15:00이다. 오전/오후를 안 쓰면 오후로 본다.
            // 아침을 뜻했다면 "오전 9시"처럼 써야 한다
            hour += 12;
        }
        if (hour > 23 || minute > 59) {
            return Optional.empty();
        }
        return Optional.of(new Match<>(LocalTime.of(hour, minute), matcher.start(), matcher.end()));
    }

    private LocalDateTime onTodayOrTomorrow(LocalTime time, LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atTime(time);
        return today.isAfter(now) ? today : today.plusDays(1);
    }

    private String titleWithout(String raw, Optional<Match<LocalDate>> date, Optional<Match<LocalTime>> time) {
        StringBuilder sb = new StringBuilder(raw);
        // 뒤에서부터 지워야 앞 구간의 인덱스가 밀리지 않는다
        java.util.stream.Stream.of(date.map(d -> new int[]{d.start(), d.end()}).orElse(null),
                        time.map(t -> new int[]{t.start(), t.end()}).orElse(null))
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> Integer.compare(b[0], a[0]))
                .forEach(range -> sb.replace(range[0], range[1], " "));

        String rest = sb.toString().replaceAll("\\s+", " ").trim();
        return rest.isEmpty() ? raw.trim() : rest;
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


}
