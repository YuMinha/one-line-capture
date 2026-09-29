package com.example.capture.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.Getter;

@Entity
@Table(name = "app_user")
@Getter
public class AppUser {

    // V5 마이그레이션이 만든 주인 행. 기존 기록이 전부 여기 붙어 있다
    public static final long OWNER_ID = 1L;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AppUser() {}

    public AppUser(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public void changeTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    // 256비트 난수라 사전 공격이 불가능하다. 그래서 BCrypt 같은 느린 해시가 필요 없다 (stack.md §2.2)
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashOf(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 모든 JVM은 SHA-256을 반드시 제공한다. 여기 오면 JVM이 망가진 것이다
            throw new IllegalStateException(e);
        }
    }
}
