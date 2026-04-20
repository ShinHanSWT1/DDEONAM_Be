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

    public DrivingInsightGenerationLimiter(
            @Value("${driving-insight.max-daily-generations-per-user:3}") int maxDailyGenerationsPerUser
    ) {
        this.maxDailyGenerationsPerUser = maxDailyGenerationsPerUser;
    }

    public boolean tryAcquire(Long userId) {
        String key = "%d:%s".formatted(userId, LocalDate.now());
        AtomicInteger counter = counters.computeIfAbsent(key, ignored -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxDailyGenerationsPerUser;
    }
}
