package com.example.capture.capture.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "todo")
@Getter
public class Todo {

    @Id
    private Long captureId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "capture_id")
    private Capture capture;

    @Column(nullable = false, length = 200)
    private String title;

    // 날짜 표현을 못 찾으면 NULL. 마감 없는 할일이 정상 상태다 (spec.md §3)
    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "done_at")
    private LocalDateTime doneAt;

    protected Todo() {}

    // title은 200자, raw_text는 500자다. 원문은 capture.raw_text에 온전히 남으므로
    // 여기서 자른다. 모든 Todo 생성이 이 생성자를 지나가므로 한 곳만 막으면 된다
    private static final int TITLE_MAX = 200;

    public Todo(Capture capture, String title, LocalDateTime dueAt) {
        this.capture = capture;
        this.title = trim(title);
        this.dueAt = dueAt;
    }

    public void update(String title, LocalDateTime dueAt) {
        this.title = trim(title);
        this.dueAt = dueAt;
    }

    // 완료 체크는 분류 수정이 아니다. capture.source는 건드리지 않는다
    public void changeDone(boolean done, LocalDateTime now) {
        this.done = done;
        this.doneAt = done ? now : null;
    }

    private static String trim(String title) {
        return title.length() > TITLE_MAX ? title.substring(0, TITLE_MAX) : title;
    }
}
