package com.finfive.crisfin.domain.user;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force guard for login. Tracks consecutive failed attempts per email and
 * locks the account for a cool-down window after too many failures.
 *
 * <p>State is intentionally in-memory (a mock-friendly, migration-free guard): it resets on
 * restart, which is acceptable for this project. Keyed by lower-cased email so casing can't
 * bypass the limit.</p>
 */
@Service
public class LoginAttemptService {

    /** 잠금까지 허용하는 연속 실패 횟수. */
    private static final int MAX_ATTEMPTS = 5;
    /** 잠금 지속 시간. */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int count, Instant lockedUntil) {}

    /** Whether the email is currently locked out. */
    public boolean isLocked(String email) {
        Attempt a = attempts.get(key(email));
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(Instant.now());
    }

    /** Records a failed attempt; locks the account once the threshold is reached. */
    public void recordFailure(String email) {
        attempts.compute(key(email), (k, prev) -> {
            int count = (prev == null ? 0 : prev.count()) + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? Instant.now().plus(LOCK_DURATION) : null;
            return new Attempt(count, lockedUntil);
        });
    }

    /** Clears the counter after a successful login. */
    public void reset(String email) {
        attempts.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
