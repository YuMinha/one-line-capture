package com.example.capture.user;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// 토큰과 달리 비밀번호는 사람이 고른 값이라 사전 공격이 된다. 그래서 여기서만 일부러 느린 해시를 쓴다.
// JDK 내장 PBKDF2라 새 의존성이 없다 (stack.md §5)
public final class Passwords {

    // 1GB·1/8 코어 서버에서 로그인 한 번이 체감되지 않을 선. 반복수가 저장 형식에 들어 있으므로
    // 나중에 올려도 기존 해시는 그대로 검증된다
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    // 없는 아이디로 로그인할 때도 해시를 한 번 계산하는 데 쓴다. 안 그러면 응답 속도로 가입 여부가 드러난다
    static final String DUMMY_HASH = hash("dummy-password-for-timing");

    private Passwords() {}

    public static String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + ENCODER.encodeToString(salt) + "$"
                + ENCODER.encodeToString(derive(raw, salt, ITERATIONS));
    }

    public static boolean matches(String raw, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        byte[] expected = DECODER.decode(parts[3]);
        byte[] actual = derive(raw, DECODER.decode(parts[2]), Integer.parseInt(parts[1]));
        // equals는 다른 바이트에서 바로 멈춰 비교 시간으로 해시를 한 글자씩 맞혀볼 수 있다
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String raw, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // 모든 JVM은 PBKDF2WithHmacSHA256을 제공한다. 여기 오면 JVM이 망가진 것이다
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
