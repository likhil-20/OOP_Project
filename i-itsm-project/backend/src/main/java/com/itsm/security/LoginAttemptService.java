package com.itsm.security;

import com.itsm.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts failed logins per email. After maxAttempts failures the email is locked
 * for lockDuration. A successful login resets the counter.
 * (Kept in memory: restarting the backend clears all locks.)
 */
@Component
public class LoginAttemptService {

    private static class Record {
        int failures = 0;
        LocalDateTime lockedUntil = null;
    }

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, Record> attempts = new HashMap<>();

    // Used by Spring: 5 attempts, 15 minute lock
    @Autowired
    public LoginAttemptService() {
        this(5, Duration.ofMinutes(15));
    }

    // Used by the tests so they can use small numbers
    public LoginAttemptService(int maxAttempts, Duration lockDuration) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    public synchronized void checkNotLocked(String key) {
        Record r = attempts.get(key);
        if (r == null || r.lockedUntil == null) return;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(r.lockedUntil)) {
            long minutes = Duration.between(now, r.lockedUntil).toMinutes() + 1;
            throw new TooManyRequestsException(
                "Too many failed login attempts. Try again in " + minutes + " minute(s).");
        }
        attempts.remove(key); // lock has expired, start fresh
    }

    public synchronized void recordFailure(String key) {
        Record r = attempts.computeIfAbsent(key, k -> new Record());
        r.failures++;
        if (r.failures >= maxAttempts) {
            r.lockedUntil = LocalDateTime.now().plus(lockDuration);
        }
    }

    public synchronized void recordSuccess(String key) {
        attempts.remove(key);
    }
}