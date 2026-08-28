package com.example.capture.parser;

import com.example.capture.capture.domain.CaptureType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// sealed로 두면 서비스에서 switch가 빠짐없이 처리됐는지를 컴파일러가 검사한다.
// 필드를 전부 nullable로 늘어놓은 DTO 하나였다면 그 검사를 사람이 대신해야 한다
public sealed interface ParsedCapture {

    CaptureType type();

    record Expense(BigDecimal amount, String merchant, LocalDate spentAt) implements ParsedCapture {
        @Override
        public CaptureType type() {
            return CaptureType.EXPENSE;
        }
    }

    record Todo(String title, LocalDateTime dueAt) implements ParsedCapture {
        @Override
        public CaptureType type() {
            return CaptureType.TODO;
        }
    }

    record Link(String url, String note) implements ParsedCapture {
        @Override
        public CaptureType type() {
            return CaptureType.LINK;
        }
    }
}
