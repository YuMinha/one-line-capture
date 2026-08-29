package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.example.capture.capture.domain.Expense;
import com.example.capture.capture.domain.Link;
import com.example.capture.capture.domain.Todo;
import com.example.capture.parser.CaptureParser;
import com.example.capture.common.ApiException;
import com.example.capture.parser.ParsedCapture;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @Transactional
    public CaptureResponse update(Long id, CaptureUpdateRequest request) {
        Capture capture = captureRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CAPTURE_NOT_FOUND", "없는 캡처입니다"));

        CaptureType before = capture.getType();
        if (before != request.type()) {
            // 타입이 바뀌면 기존 상세 행을 지운다. 상세는 타입당 하나만 존재해야 한다
            deleteDetail(before, id);
            // 삭제를 먼저 확정한다. Hibernate가 쓰기 순서를 바꿔도 결과가 흔들리지 않게
            entityManager.flush();
        }

        // 타입이 그대로여도 사용자가 손댔으므로 source는 MANUAL이 된다 (stack.md §3.4)
        capture.reclassify(request.type());

        return switch (request.type()) {
            case EXPENSE -> {
                var detail = require(request.expense(), "expense");
                yield CaptureResponse.of(capture, expenseRepository.findById(id)
                        .map(expense -> {
                            expense.update(detail.amount(), detail.merchant(), detail.spentAt());
                            return expense;
                        })
                        .orElseGet(() -> expenseRepository.save(
                                new Expense(capture, detail.amount(), detail.merchant(), detail.spentAt()))));
            }
            case TODO -> {
                var detail = require(request.todo(), "todo");
                LocalDateTime dueAt = toStored(detail.dueAt());
                yield CaptureResponse.of(capture, todoRepository.findById(id)
                        .map(todo -> {
                            todo.update(detail.title(), dueAt);
                            return todo;
                        })
                        .orElseGet(() -> todoRepository.save(new Todo(capture, detail.title(), dueAt))));
            }
            case LINK -> {
                var detail = require(request.link(), "link");
                yield CaptureResponse.of(capture, linkRepository.findById(id)
                        .map(link -> {
                            link.update(detail.url(), detail.note());
                            return link;
                        })
                        .orElseGet(() -> linkRepository.save(
                                new Link(capture, detail.url(), detail.note()))));
            }
        };
    }

    @Transactional
    public void delete(Long id) {
        Capture capture = captureRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CAPTURE_NOT_FOUND", "없는 캡처입니다"));
        // 상세를 먼저 지운다. 영속성 컨텍스트에 남은 상세가 capture를 참조하고 있으면
        // Hibernate가 capture 삭제를 flush에 싣지 않는다
        deleteDetail(capture.getType(), id);
        captureRepository.delete(capture);
        // DB에도 ON DELETE CASCADE가 걸려 있다. 다만 영속성 컨텍스트에 남아 있는
        // 상세 엔티티는 JPA가 모르므로, 삭제를 DB에 보내고 컨텍스트를 비운다 (stack.md §2.2)
        entityManager.flush();
        entityManager.clear();
    }

    private void deleteDetail(CaptureType type, Long id) {
        switch (type) {
            case EXPENSE -> expenseRepository.deleteById(id);
            case TODO -> todoRepository.deleteById(id);
            case LINK -> linkRepository.deleteById(id);
        }
    }

    private <T> T require(T detail, String field) {
        if (detail == null) {
            throw ApiException.badRequest("DETAIL_REQUIRED", field + " 상세가 필요합니다");
        }
        return detail;
    }

    // 요청은 UTC Instant로 오고 저장은 UTC LocalDateTime이다 (stack.md §2.2)
    private LocalDateTime toStored(java.time.Instant dueAt) {
        return dueAt == null ? null : LocalDateTime.ofInstant(dueAt, ZoneOffset.UTC);
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
