package com.finfive.crisfin.domain.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoginAttemptService} — 브루트포스 잠금(5회 실패 → 잠금).
 */
class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void freshEmail_isNotLocked() {
        assertThat(service.isLocked("new@test.com")).isFalse();
    }

    @Test
    void locksOnlyAfterFifthFailure() {
        String email = "brute@test.com";
        for (int i = 0; i < 4; i++) {
            service.recordFailure(email);
            assertThat(service.isLocked(email)).as("실패 %d회 후엔 아직 잠기지 않아야 함", i + 1).isFalse();
        }
        service.recordFailure(email); // 5번째
        assertThat(service.isLocked(email)).as("5회 실패 후 잠겨야 함").isTrue();
    }

    @Test
    void resetClearsTheCounter() {
        String email = "reset@test.com";
        for (int i = 0; i < 5; i++) service.recordFailure(email);
        assertThat(service.isLocked(email)).isTrue();

        service.reset(email);
        assertThat(service.isLocked(email)).isFalse();
    }

    @Test
    void isCaseInsensitive_soCasingCannotBypass() {
        // 대소문자를 바꿔도 같은 카운터를 공유해야 우회가 불가능하다.
        for (int i = 0; i < 5; i++) service.recordFailure("User@Test.com");
        assertThat(service.isLocked("user@test.com")).isTrue();
        assertThat(service.isLocked("USER@TEST.COM")).isTrue();
    }

    @Test
    void nullEmail_doesNotThrow() {
        service.recordFailure(null);
        assertThat(service.isLocked(null)).isFalse(); // 1회로는 잠기지 않음
    }
}
