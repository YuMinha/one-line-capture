package com.example.capture.parser.rule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

// "만 오천", "1만5천", "9,000", "1.5만", "오백"을 모두 같은 방법으로 읽는다.
// 패턴을 하나씩 늘리면 조합이 폭발한다. 수사(數詞)를 왼쪽부터 읽는 파서 하나면 끝난다
final class KoreanAmount {

    private static final Map<Character, Integer> DIGITS = Map.of(
            '일', 1, '이', 2, '삼', 3, '사', 4, '오', 5,
            '육', 6, '칠', 7, '팔', 8, '구', 9);

    private static final Map<Character, Integer> SMALL_UNITS = Map.of(
            '십', 10, '백', 100, '천', 1000);

    private static final char MAN = '만';

    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);

    private KoreanAmount() {}

    static Optional<BigDecimal> parse(String token) {
        String text = token.replaceAll("[\\s,]", "");
        if (text.isEmpty()) {
            return Optional.empty();
        }

        if (text.matches("[0-9]+(\\.[0-9]+)?")) {
            return Optional.of(new BigDecimal(text));
        }

        // 1.5만 / 1.5천 처럼 소수에 단위가 붙는 경우. 수사 파서는 소수를 다루지 않는다
        if (text.matches("[0-9]+\\.[0-9]+[만천백십]")) {
            char unit = text.charAt(text.length() - 1);
            BigDecimal base = new BigDecimal(text.substring(0, text.length() - 1));
            return Optional.of(base.multiply(unitValue(unit)));
        }

        return parseSino(text);
    }

    private static BigDecimal unitValue(char unit) {
        return unit == MAN ? TEN_THOUSAND : BigDecimal.valueOf(SMALL_UNITS.get(unit));
    }

    // 왼쪽부터 읽으며 만 단위마다 구간을 닫는다.
    //   1만5천 → (1)만 닫음 10000, 그 뒤 5천 5000 → 15000
    //   만오천 → 숫자 없는 '만'은 1만으로 읽는다 → 10000 + 5000
    private static Optional<BigDecimal> parseSino(String text) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal section = BigDecimal.ZERO;
        BigDecimal digit = null;
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isDigit(c)) {
                number.append(c);
                continue;
            }
            if (number.length() > 0) {
                digit = new BigDecimal(number.toString());
                number.setLength(0);
            }

            if (DIGITS.containsKey(c)) {
                // '오오' 처럼 숫자가 연달아 오면 수사가 아니다
                if (digit != null) {
                    return Optional.empty();
                }
                digit = BigDecimal.valueOf(DIGITS.get(c));
            } else if (SMALL_UNITS.containsKey(c)) {
                section = section.add(orOne(digit).multiply(BigDecimal.valueOf(SMALL_UNITS.get(c))));
                digit = null;
            } else if (c == MAN) {
                section = section.add(orZero(digit));
                total = total.add(orOne(section).multiply(TEN_THOUSAND));
                section = BigDecimal.ZERO;
                digit = null;
            } else {
                return Optional.empty();
            }
        }

        if (number.length() > 0) {
            digit = new BigDecimal(number.toString());
        }
        total = total.add(section).add(orZero(digit));

        return total.signum() > 0 ? Optional.of(total) : Optional.empty();
    }

    private static BigDecimal orOne(BigDecimal value) {
        return value == null || value.signum() == 0 ? BigDecimal.ONE : value;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
