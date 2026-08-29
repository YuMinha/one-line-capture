package com.example.capture.capture.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "capture")
@Getter
public class Capture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_text", nullable = false, length = 500)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaptureType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaptureSource source;

    // 두 시각은 DB의 DEFAULT/ON UPDATE가 채운다. 앱과 DB가 각자 시각을 쓰면
    // 진실이 두 개가 되므로 쓰기 권한을 DB 한쪽에만 준다
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Capture() {}

    public Capture(String rawText, CaptureType type, CaptureSource source) {
        this.rawText = rawText;
        this.type = type;
        this.source = source;
    }

    // rawText는 절대 바뀌지 않는다. 파서를 고친 뒤 과거 데이터를 다시 파싱해볼 수 있어야 한다
    // (stack.md §2.2). 사용자가 손댔다는 사실은 source에 남는다 (§3.4)
    public void reclassify(CaptureType type) {
        this.type = type;
        this.source = CaptureSource.MANUAL;
    }
}
