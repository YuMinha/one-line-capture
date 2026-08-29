package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.Expense;
import com.example.capture.capture.domain.Link;
import com.example.capture.capture.domain.Todo;
import com.example.capture.parser.CaptureParser;
import com.example.capture.parser.ParsedCapture;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureService {

    private final CaptureRepository captureRepository;
    private final ExpenseRepository expenseRepository;
    private final TodoRepository todoRepository;
    private final LinkRepository linkRepository;
    private final CaptureParser captureParser;
    private final EntityManager entityManager;

    @Transactional
    public CaptureResponse create(String text) {
        String rawText = text.trim();
        ParsedCapture parsed = captureParser.parse(rawText);

        Capture capture = captureRepository.saveAndFlush(
                new Capture(rawText, parsed.type(), CaptureSource.AUTO));
        // created_at은 DB가 채우므로 다시 읽지 않으면 null이다
        entityManager.refresh(capture);

        // sealed라 타입을 빠뜨리면 컴파일이 안 된다. default가 필요 없는 이유다
        return switch (parsed) {
            case ParsedCapture.Expense e -> CaptureResponse.of(capture,
                    expenseRepository.save(new Expense(capture, e.amount(), e.merchant(), e.spentAt())));
            case ParsedCapture.Todo t -> CaptureResponse.of(capture,
                    todoRepository.save(new Todo(capture, t.title(), t.dueAt())));
            case ParsedCapture.Link l -> CaptureResponse.of(capture,
                    linkRepository.save(new Link(capture, l.url(), l.note())));
        };
    }
}
