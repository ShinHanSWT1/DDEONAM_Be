package com.gorani.ecodrive.driving.service.insight;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DrivingInsightGenerationLimiter {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final int maxDailyGenerationsPerUser;
    private volatile LocalDate counterDate = LocalDate.now();

    public DrivingInsightGenerationLimiter(
            @Value("${driving-insight.max-daily-generations-per-user:3}") int maxDailyGenerationsPerUser
    ) {
        this.maxDailyGenerationsPerUser = maxDailyGenerationsPerUser;
    }

    public boolean tryAcquire(Long userId) {
        rotateIfNeeded();

        String key = key(userId);
        AtomicInteger counter = counters.computeIfAbsent(key, ignored -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxDailyGenerationsPerUser;
    }

    public void release(Long userId) {
        rotateIfNeeded();

        AtomicInteger counter = counters.get(key(userId));
        if (counter == null) {
            return;
        }

        counter.updateAndGet(current -> Math.max(0, current - 1));
    }

    private void rotateIfNeeded() {
        LocalDate today = LocalDate.now();
        if (counterDate.equals(today)) {
            return;
        }

        synchronized (this) {
            if (counterDate.equals(today)) {
                return;
            }
            counters.clear();
            counterDate = today;
        }
    }

    private String key(Long userId) {
        return "%d:%s".formatted(userId, LocalDate.now());
    }
}
