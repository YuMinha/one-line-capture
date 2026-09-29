package com.example.capture.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "link_code")
@Getter
public class LinkCode {

    // 원문 대신 해시가 PK다. DB가 새도 살아 있는 코드를 쓸 수 없다
    @Id
    @Column(name = "code_hash", length = 64)
    private String codeHash;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    // 다른 시각 컬럼처럼 UTC로 저장한다 (stack.md §2.2)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected LinkCode() {}

    public LinkCode(String codeHash, Long userId, LocalDateTime expiresAt) {
        this.codeHash = codeHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }
}
