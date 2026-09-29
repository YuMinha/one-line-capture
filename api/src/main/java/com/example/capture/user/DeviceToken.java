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
@Table(name = "device_token")
@Getter
public class DeviceToken {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Capture와 같은 이유로 연관 대신 id만 든다. 필터가 필요한 건 이 숫자 하나다
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "from_env", nullable = false)
    private boolean fromEnv;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DeviceToken() {}

    public DeviceToken(Long userId, String tokenHash, boolean fromEnv) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.fromEnv = fromEnv;
    }

    public void changeTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    // 256비트 난수라 사전 공격이 불가능하다. 그래서 비밀번호와 달리 느린 해시가 필요 없다 (stack.md §2.2)
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashOf(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 모든 JVM은 SHA-256을 반드시 제공한다. 여기 오면 JVM이 망가진 것이다
            throw new IllegalStateException(e);
        }
    }
}
