package com.example.capture.parser.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.capture.parser.ParsedCapture;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LinkRuleTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 10, 0);

    private final LinkRule rule = new LinkRule();

    private ParsedCapture.Link parse(String raw) {
        return (ParsedCapture.Link) rule.tryParse(raw, NOW).orElseThrow();
    }

    @ParameterizedTest(name = "[{index}] {0} → url={1}, note={2}")
    @CsvSource(delimiter = '|', value = {
            "https://a.com                          | https://a.com | ",
            "http://a.com                           | http://a.com  | ",
            "https://a.com 스프링 정리글             | https://a.com | 스프링 정리글",
            "스프링 정리글 https://a.com             | https://a.com | 스프링 정리글",
            "읽을것 https://a.com 나중에             | https://a.com | 읽을것 나중에",
            "https://a.com/post/9000                | https://a.com/post/9000 | ",
            "https://a.com?q=1&b=2#frag             | https://a.com?q=1&b=2#frag | ",
    })
    @DisplayName("URL을 뽑고 나머지는 메모가 된다")
    void URL_추출(String raw, String expectedUrl, String expectedNote) {
        ParsedCapture.Link link = parse(raw.trim());

        assertThat(link.url()).isEqualTo(expectedUrl);
        assertThat(link.note()).isEqualTo(expectedNote);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "우산 챙기기",
            "점심 9000원",
            "a.com 은 프로토콜이 없다",
            "ftp://a.com 은 대상이 아니다",
            "",
            "   ",
    })
    @DisplayName("URL이 없으면 이 룰은 손대지 않는다")
    void URL_없으면_통과(String raw) {
        assertThat(rule.tryParse(raw, NOW)).isEmpty();
    }

    @Test
    @DisplayName("URL이 여러 개면 첫 번째만 쓰고 나머지는 메모에 남는다")
    void URL_여러개() {
        ParsedCapture.Link link = parse("https://a.com https://b.com");

        assertThat(link.url()).isEqualTo("https://a.com");
        assertThat(link.note()).isEqualTo("https://b.com");
    }

    @Test
    @DisplayName("결과 타입은 LINK다")
    void 타입은_LINK() {
        assertThat(parse("https://a.com").type()).isEqualTo(com.example.capture.capture.domain.CaptureType.LINK);
    }
}
