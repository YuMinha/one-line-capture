package com.example.capture.parser;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ParseRule {

    // now를 인자로 받는다. 룰 안에서 LocalDateTime.now()를 부르면 "내일 3시"를
    // 테스트할 수 없다 (stack.md §4)
    Optional<ParsedCapture> tryParse(String raw, LocalDateTime now);
}
