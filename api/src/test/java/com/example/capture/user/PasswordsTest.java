package com.example.capture.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordsTest {

    @Test
    @DisplayName("원문이 저장 형식에 드러나지 않고, 같은 비밀번호라도 솔트가 달라 해시가 매번 다르다")
    void 해시는_원문을_숨기고_매번_다르다() {
        String first = Passwords.hash("correct horse");
        String second = Passwords.hash("correct horse");

        assertThat(first).startsWith("pbkdf2$").doesNotContain("correct horse");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("맞는 비밀번호만 통과한다")
    void 맞는_비밀번호만_통과() {
        String stored = Passwords.hash("correct horse");

        assertThat(Passwords.matches("correct horse", stored)).isTrue();
        assertThat(Passwords.matches("correct hors", stored)).isFalse();
        assertThat(Passwords.matches("Correct horse", stored)).isFalse();
    }

    @Test
    @DisplayName("저장 형식이 깨졌으면 예외 없이 false")
    void 깨진_형식() {
        assertThat(Passwords.matches("x", "")).isFalse();
        assertThat(Passwords.matches("x", "bcrypt$1$a$b")).isFalse();
    }

    @Test
    @DisplayName("반복수를 나중에 바꿔도 예전 해시는 저장된 반복수로 검증된다")
    void 반복수가_저장_형식에_있다() {
        String stored = Passwords.hash("pw12345678");
        String lowered = stored.replaceFirst("\\$\\d+\\$", "\\$1000\\$");

        assertThat(Passwords.matches("pw12345678", stored)).isTrue();
        // 반복수를 바꾸면 다른 키가 나온다 = 반복수가 실제로 검증에 쓰인다
        assertThat(Passwords.matches("pw12345678", lowered)).isFalse();
    }
}
