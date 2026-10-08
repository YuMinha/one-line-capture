package com.example.capture.parser.rule;

import com.example.capture.parser.ParseRule;
import com.example.capture.parser.ParsedCapture;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ExpenseRule implements ParseRule {

    // '원' 앞에 붙은 수사 덩어리를 통째로 집는다. 문자 종류만 정의하고 값 계산은
    // KoreanAmount에 맡긴다. 패턴을 형식마다 늘리면 조합이 폭발한다
    // 한글 수사로 시작할 때는 앞 글자가 한글이면 안 된다. 안 그러면 '떡볶이 만원'의 '이'를
    // 2로 읽어 20000원이 된다. 아라비아 숫자는 '다이소3000원'처럼 붙여 쓰므로 막지 않는다
    private static final Pattern WITH_WON = Pattern.compile(
            "(?:₩\\s*)?((?:[0-9]|(?<![가-힣])[일이삼사오육칠팔구십백천만])[0-9,.\\s일이삼사오육칠팔구십백천만]*?)\\s*원");

    // ₩9,000 처럼 기호만 붙고 '원'이 없는 표기
    private static final Pattern CURRENCY_SIGN = Pattern.compile("₩\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)");

    // '원' 없는 맨 숫자. "점심 9000" 처럼 쓰는 사람이 많다.
    // 세 자리 이상이거나 콤마가 있어야 하고, 뒤에 시·분·월 같은 단위가 붙으면 안 된다
    private static final Pattern BARE_NUMBER = Pattern.compile(
            "(?<![0-9.,:/\\-])([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{3,})(?![0-9.,:/\\-]|\\s*[시분초월일년주개명번호층칸%])");

    // '원' 없이 단위로 끝나는 금액. "교통카드 5만", "점심 9천" 처럼 쓴다 (stack.md §4).
    // 아라비아 숫자로 시작해야 한다. 한글 수사까지 받으면 '만나기', '천천히'가 금액이 된다.
    // 단위 바로 뒤에 한글이 붙으면 '1만보', '3천명' 같은 수량이라 뺀다
    private static final Pattern UNIT_WITHOUT_WON = Pattern.compile(
            "(?<![0-9.,:/\\-])([0-9]+(?:\\.[0-9]+)?\\s*[만천](?:\\s*[0-9]+\\s*천)?)"
                    + "(?![0-9가-힣]|\\s*[시분초월일년주개명번호층칸%])");

    // '원' 없는 금액(단위 금액·맨 숫자)을 볼지 판단할 때만 쓴다. 날짜·시각 표현이 있으면 할일일 가능성이 높다
    private static final Pattern DATE_OR_TIME = Pattern.compile(
            "오늘|내일|모레|다음\\s*주|이번\\s*주|[월화수목금토일]요일|[0-9]+\\s*[시분월일년]");

    @Override
    public Optional<ParsedCapture> tryParse(String raw, LocalDateTime now) {
        return find(WITH_WON, raw)
                .or(() -> find(CURRENCY_SIGN, raw))
                .or(() -> withoutWon(raw))
                .map(found -> new ParsedCapture.Expense(
                        found.amount(),
                        // 지출일은 입력일이다. 텍스트에서 날짜를 읽는 건 v1 범위 밖 (spec.md §7)
                        merchant(raw, found.start(), found.end()), now.toLocalDate()));
    }

    private record Found(BigDecimal amount, int start, int end) {}

    // 첫 매치가 금액으로 안 읽히면 다음 매치를 본다. "3.14 원주율 9000원" 같은 경우.
    // 실패한 매치의 끝이 아니라 시작 다음 글자부터 다시 찾는다. '오이 3000원'은 '오이 3000'이
    // 통째로 실패하는데, 끝부터 찾으면 그 안의 '3000원'을 영영 못 본다
    private Optional<Found> find(Pattern pattern, String raw) {
        Matcher matcher = pattern.matcher(raw);
        int from = 0;
        while (from <= raw.length() && matcher.find(from)) {
            Optional<BigDecimal> amount = KoreanAmount.parse(matcher.group(1));
            if (amount.isPresent()) {
                return Optional.of(new Found(amount.get(), matcher.start(), matcher.end()));
            }
            from = matcher.start() + 1;
        }
        return Optional.empty();
    }

    private Optional<Found> withoutWon(String raw) {
        // "오늘 100 계단"을 100원으로 읽으면 안 된다. '원'이 없으면 근거가 약하므로 물러선다
        if (DATE_OR_TIME.matcher(raw).find()) {
            return Optional.empty();
        }
        return find(UNIT_WITHOUT_WON, raw).or(() -> find(BARE_NUMBER, raw));
    }

    private String merchant(String raw, int start, int end) {
        String rest = (raw.substring(0, start) + " " + raw.substring(end))
                .replaceAll("\\s+", " ")
                .trim();
        return rest.isEmpty() ? null : rest;
    }
}
