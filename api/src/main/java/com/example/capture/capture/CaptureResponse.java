package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.example.capture.capture.domain.Expense;
import com.example.capture.capture.domain.Link;
import com.example.capture.capture.domain.Todo;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

// 타입에 해당하는 상세 객체 하나만 내려간다. 프론트에서 if (item.expense) 로 분기한다 (stack.md §3.2)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaptureResponse(
        Long id,
        CaptureType type,
        String rawText,
        CaptureSource source,
        Instant createdAt,
        ExpenseDetail expense,
        TodoDetail todo,
        LinkDetail link
) {
    public record ExpenseDetail(BigDecimal amount, String merchant, LocalDate spentAt) {}

    public record TodoDetail(String title, Instant dueAt, boolean done) {}

    public record LinkDetail(String url, String note, Instant readAt) {}

    public static CaptureResponse of(Capture capture, Expense expense) {
        return base(capture)
                .withExpense(new ExpenseDetail(normalize(expense.getAmount()), expense.getMerchant(), expense.getSpentAt()));
    }

    public static CaptureResponse of(Capture capture, Todo todo) {
        return base(capture)
                .withTodo(new TodoDetail(todo.getTitle(), utc(todo.getDueAt()), todo.isDone()));
    }

    public static CaptureResponse of(Capture capture, Link link) {
        return base(capture)
                .withLink(new LinkDetail(link.getUrl(), link.getNote(), utc(link.getReadAt())));
    }

    private static CaptureResponse base(Capture capture) {
        return new CaptureResponse(capture.getId(), capture.getType(), capture.getRawText(),
                capture.getSource(), utc(capture.getCreatedAt()), null, null, null);
    }

    private CaptureResponse withExpense(ExpenseDetail detail) {
        return new CaptureResponse(id, type, rawText, source, createdAt, detail, null, null);
    }

    private CaptureResponse withTodo(TodoDetail detail) {
        return new CaptureResponse(id, type, rawText, source, createdAt, null, detail, null);
    }

    private CaptureResponse withLink(LinkDetail detail) {
        return new CaptureResponse(id, type, rawText, source, createdAt, null, null, detail);
    }

    // DB는 UTC로 저장한다. 화면에서만 KST로 바꾼다 (stack.md §2.2)
    private static Instant utc(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    // DECIMAL(12,2)라 9000.00으로 읽힌다. 원화 정수만 다루므로 소수부가 없으면 떼고 내보낸다.
    // 반올림이 아니라 0만 떼는 것이라 값이 바뀌지 않는다
    private static BigDecimal normalize(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0 ? amount.setScale(0) : amount;
    }
}
