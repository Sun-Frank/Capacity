package com.capics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginThrottleService {

    private static final long DEFAULT_WINDOW_MS = Duration.ofMinutes(10).toMillis();
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final long windowMs;
    private final int maxAttempts;
    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public LoginThrottleService(
            @Value("${auth.login.throttle.window-ms:600000}") long windowMs,
            @Value("${auth.login.throttle.max-attempts:5}") int maxAttempts
    ) {
        this.windowMs = windowMs > 0 ? windowMs : DEFAULT_WINDOW_MS;
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : DEFAULT_MAX_ATTEMPTS;
    }

    public boolean isBlocked(String username) {
        AttemptRecord record = attempts.get(normalize(username));
        if (record == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - record.windowStartedAt > windowMs) {
            attempts.remove(normalize(username));
            return false;
        }
        return record.failureCount >= maxAttempts;
    }

    public long retryAfterSeconds(String username) {
        AttemptRecord record = attempts.get(normalize(username));
        if (record == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long leftMs = windowMs - (now - record.windowStartedAt);
        return Math.max(0, (leftMs + 999) / 1000);
    }

    public void onFailure(String username) {
        String key = normalize(username);
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, v) -> {
            if (v == null || now - v.windowStartedAt > windowMs) {
                return new AttemptRecord(1, now);
            }
            return new AttemptRecord(v.failureCount + 1, v.windowStartedAt);
        });
    }

    public void onSuccess(String username) {
        attempts.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static class AttemptRecord {
        private final int failureCount;
        private final long windowStartedAt;

        private AttemptRecord(int failureCount, long windowStartedAt) {
            this.failureCount = failureCount;
            this.windowStartedAt = windowStartedAt;
        }
    }
}
