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

    public Todo(Capture capture, String title, LocalDateTime dueAt) {
        this.capture = capture;
        this.title = title;
        this.dueAt = dueAt;
    }
}
