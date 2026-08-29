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
@Table(name = "link")
@Getter
public class Link {

    @Id
    private Long captureId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "capture_id")
    private Capture capture;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 300)
    private String note;

    // NULL이면 안 읽음
    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Link() {}

    // note는 300자, raw_text는 500자다. Todo.title과 같은 이유로 여기서 자른다
    private static final int NOTE_MAX = 300;

    public Link(Capture capture, String url, String note) {
        this.capture = capture;
        this.url = url;
        this.note = trim(note);
    }

    public void update(String url, String note) {
        this.url = url;
        this.note = trim(note);
    }

    private static String trim(String note) {
        return note != null && note.length() > NOTE_MAX ? note.substring(0, NOTE_MAX) : note;
    }
}
