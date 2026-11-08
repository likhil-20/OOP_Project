package com.itsm.security;

import com.itsm.exception.TooManyRequestsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    @Test
    @DisplayName("TC-05: Fewer failures than the limit does not lock the account")
    void belowLimitNotLocked() {
        LoginAttemptService s = new LoginAttemptService(3, Duration.ofMinutes(15));
        s.recordFailure("a@x.com");
        s.recordFailure("a@x.com");
        assertDoesNotThrow(() -> s.checkNotLocked("a@x.com"));
    }

    @Test
    @DisplayName("TC-06: Reaching the failure limit locks the account")
    void reachingLimitLocks() {
        LoginAttemptService s = new LoginAttemptService(3, Duration.ofMinutes(15));
        s.recordFailure("a@x.com");
        s.recordFailure("a@x.com");
        s.recordFailure("a@x.com");
        assertThrows(TooManyRequestsException.class, () -> s.checkNotLocked("a@x.com"));
    }

    @Test
    @DisplayName("TC-07: A successful login resets the failure counter")
    void successResetsCounter() {
        LoginAttemptService s = new LoginAttemptService(3, Duration.ofMinutes(15));
        s.recordFailure("a@x.com");
        s.recordFailure("a@x.com");
        s.recordSuccess("a@x.com");
        s.recordFailure("a@x.com");
        s.recordFailure("a@x.com");
        assertDoesNotThrow(() -> s.checkNotLocked("a@x.com"));
    }

    @Test
    @DisplayName("TC-08: A lock expires after the lock duration")
    void lockExpires() {
        LoginAttemptService s = new LoginAttemptService(1, Duration.ZERO);
        s.recordFailure("a@x.com");
        assertDoesNotThrow(() -> s.checkNotLocked("a@x.com"));
    }

    @Test
    @DisplayName("TC-09: Locking one email does not affect another email")
    void locksArePerEmail() {
        LoginAttemptService s = new LoginAttemptService(1, Duration.ofMinutes(15));
        s.recordFailure("a@x.com");
        assertThrows(TooManyRequestsException.class, () -> s.checkNotLocked("a@x.com"));
        assertDoesNotThrow(() -> s.checkNotLocked("b@x.com"));
    }
}