package com.example.TestAPI.Service.RateLimiter;

import com.example.TestAPI.exception.BusinessException;
import com.example.TestAPI.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ForgotPasswordRateLimiter {

    private final Map<String, Instant> requests = new ConcurrentHashMap<>();
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    public void check(String email) {
        String key = email.toLowerCase().trim();
        Instant last = requests.get(key);
        Instant now = Instant.now();

        if (last != null && now.isBefore(last.plus(COOLDOWN))) {
            long remaining = Duration.between(now, last.plus(COOLDOWN)).toSeconds();
            throw new BusinessException(
                    "Trop de demandes. Réessayez dans " + remaining + " secondes.",
                    ErrorCode.TOO_MANY_REQUESTS);
        }

        requests.put(key, now);
    }

    public void clear(String email) {
        requests.remove(email.toLowerCase().trim());
    }
}