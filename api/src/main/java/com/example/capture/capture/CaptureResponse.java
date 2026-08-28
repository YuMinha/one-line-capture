package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.example.capture.capture.domain.Todo;
import java.time.Instant;
import java.time.ZoneOffset;

public record CaptureResponse(
        Long id,
        CaptureType type,
        String rawText,
        CaptureSource source,
        Instant createdAt,
        TodoDetail todo
) {
    public record TodoDetail(String title, Instant dueAt, boolean done) {}

    // DB는 UTC로 저장하므로 UTC로 읽어 Instant로 올린다. 화면에서만 KST로 바꾼다 (stack.md §2.2)
    public static CaptureResponse of(Capture capture, Todo todo) {
        return new CaptureResponse(
                capture.getId(),
                capture.getType(),
                capture.getRawText(),
                capture.getSource(),
                capture.getCreatedAt().toInstant(ZoneOffset.UTC),
                new TodoDetail(
                        todo.getTitle(),
                        todo.getDueAt() == null ? null : todo.getDueAt().toInstant(ZoneOffset.UTC),
                        todo.isDone()
                )
        );
    }
}
