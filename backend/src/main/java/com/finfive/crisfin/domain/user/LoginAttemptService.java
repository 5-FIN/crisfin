package com.finfive.crisfin.domain.user;

import org.springframework.scheduling.annotation.Scheduled;
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
 * bypass the limit. A scheduled sweep evicts stale entries so the map cannot grow unbounded
 * from failed attempts against random/nonexistent emails.</p>
 */
@Service
public class LoginAttemptService {

    /** 잠금까지 허용하는 연속 실패 횟수. */
    private static final int MAX_ATTEMPTS = 5;
    /** 잠금 지속 시간 (= 항목 유효 창). */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int count, Instant lastFailure, Instant lockedUntil) {}

    /** Whether the email is currently locked out. */
    public boolean isLocked(String email) {
        Attempt a = attempts.get(key(email));
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(Instant.now());
    }

    /** Records a failed attempt; locks the account once the threshold is reached. */
    public void recordFailure(String email) {
        Instant now = Instant.now();
        attempts.compute(key(email), (k, prev) -> {
            int count = (prev == null ? 0 : prev.count()) + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? now.plus(LOCK_DURATION) : null;
            return new Attempt(count, now, lockedUntil);
        });
    }

    /** Clears the counter after a successful login. */
    public void reset(String email) {
        attempts.remove(key(email));
    }

    /**
     * 마지막 실패가 잠금창(15분)보다 오래된 항목을 주기적으로 제거한다. 잠금창을 넘기면
     * 카운터·잠금이 모두 무의미해지므로 안전하게 지울 수 있고, 이로써 성공 로그인이 없는
     * (존재하지 않는/랜덤) 이메일에 대한 실패가 무한히 쌓이는 메모리 증가를 막는다.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000L)
    void evictExpired() {
        Instant cutoff = Instant.now().minus(LOCK_DURATION);
        attempts.values().removeIf(a -> a.lastFailure().isBefore(cutoff));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
