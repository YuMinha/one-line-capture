package com.example.capture.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "app_user")
@Getter
public class AppUser {

    // V5 마이그레이션이 만든 주인 행. 기존 기록이 전부 여기 붙어 있다
    public static final long OWNER_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", length = 20, unique = true)
    private String loginId;

    @Column(length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public AppUser() {}

    public boolean isRegistered() {
        return loginId != null;
    }

    // 새 사용자를 만들지 않고 지금 사용자에 붙인다. 게스트로 쓴 기록이 그대로 계정 것이 된다 (stack.md §5)
    public void register(String loginId, String email, String passwordHash) {
        this.loginId = loginId;
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
