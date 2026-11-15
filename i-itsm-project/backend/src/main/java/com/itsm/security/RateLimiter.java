package com.itsm.security;

import com.itsm.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Allows only a few calls per user per time window (kept in memory). */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> calls = new HashMap<>();

    public synchronized void check(String key, int maxCalls, long windowMillis) {
        long now = System.currentTimeMillis();
        Deque<Long> q = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() > windowMillis) q.pollFirst();
        if (q.size() >= maxCalls) {
            throw new TooManyRequestsException("Too many requests. Please wait a minute and try again.");
        }
        q.addLast(now);
    }
}