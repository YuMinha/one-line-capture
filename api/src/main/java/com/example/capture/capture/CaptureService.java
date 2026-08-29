package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.example.capture.capture.domain.Expense;
import com.example.capture.capture.domain.Link;
import com.example.capture.capture.domain.Todo;
import com.example.capture.parser.CaptureParser;
import com.example.capture.parser.ParsedCapture;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private static final int MAX_SIZE = 50;

    @Transactional(readOnly = true)
    public CaptureListResponse list(CaptureType type, Long cursor, int size, Boolean done) {
        int limit = Math.min(Math.max(size, 1), MAX_SIZE);
        // 한 건 더 읽어서 다음 페이지가 있는지 본다. COUNT 쿼리를 따로 날리지 않아도 된다
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<CaptureResponse> found = fetch(type, cursor, done, pageable);
        boolean hasNext = found.size() > limit;
        List<CaptureResponse> items = hasNext ? found.subList(0, limit) : found;

        return new CaptureListResponse(items, hasNext ? items.get(items.size() - 1).id() : null, hasNext);
    }

    private List<CaptureResponse> fetch(CaptureType type, Long cursor, Boolean done, Pageable pageable) {
        if (type == null) {
            return captureRepository.findPage(cursor, pageable).stream()
                    .map(CaptureResponse::summary).toList();
        }
        return switch (type) {
            case EXPENSE -> expenseRepository.findPage(cursor, pageable).stream()
                    .map(e -> CaptureResponse.of(e.getCapture(), e)).toList();
            case TODO -> todoRepository.findPage(cursor, done, pageable).stream()
                    .map(t -> CaptureResponse.of(t.getCapture(), t)).toList();
            case LINK -> linkRepository.findPage(cursor, pageable).stream()
                    .map(l -> CaptureResponse.of(l.getCapture(), l)).toList();
        };
    }

    // 저장하지 않는다. 파서를 고칠 때 결과만 빠르게 확인하는 용도 (stack.md §3.1)
    public CaptureResponse preview(String text) {
        String rawText = text.trim();
        return CaptureResponse.preview(rawText, captureParser.parse(rawText));
    }

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
