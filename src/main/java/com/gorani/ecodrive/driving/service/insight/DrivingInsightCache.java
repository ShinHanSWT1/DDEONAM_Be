package com.gorani.ecodrive.driving.service.insight;

import com.gorani.ecodrive.driving.dto.query.DrivingInsightResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
public class DrivingInsightCache {

    private final Map<String, DrivingInsightResponse> cache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<DrivingInsightResponse>> inFlight = new ConcurrentHashMap<>();
    private final int maxEntries;
    private volatile LocalDate cacheDate = LocalDate.now();

    public DrivingInsightCache(
            @Value("${driving-insight.cache-max-entries:500}") int maxEntries
    ) {
        this.maxEntries = maxEntries;
    }

    public DrivingInsightResponse getOrLoad(
            String key,
            Supplier<DrivingInsightResponse> loader,
            Predicate<DrivingInsightResponse> cacheable
    ) {
        rotateIfNeeded();

        DrivingInsightResponse cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<DrivingInsightResponse> created = new CompletableFuture<>();
        CompletableFuture<DrivingInsightResponse> existing = inFlight.putIfAbsent(key, created);
        if (existing != null) {
            return existing.join();
        }

        try {
            DrivingInsightResponse loaded = loader.get();
            if (cacheable.test(loaded)) {
                enforceCapacity();
                cache.put(key, loaded);
            }
            created.complete(loaded);
            return loaded;
        } catch (RuntimeException exception) {
            created.completeExceptionally(exception);
            throw exception;
        } finally {
            inFlight.remove(key);
        }
    }

    public String key(Long userId, Long userVehicleId, String fingerprint) {
        return "driving-insight:%d:%s:%s:%s".formatted(
                userId,
                userVehicleId == null ? "all" : userVehicleId,
                LocalDate.now(),
                fingerprint
        );
    }

    private void rotateIfNeeded() {
        LocalDate today = LocalDate.now();
        if (cacheDate.equals(today)) {
            return;
        }

        synchronized (this) {
            if (cacheDate.equals(today)) {
                return;
            }
            cache.clear();
            inFlight.clear();
            cacheDate = today;
        }
    }

    private void enforceCapacity() {
        if (cache.size() < maxEntries) {
            return;
        }

        synchronized (this) {
            if (cache.size() >= maxEntries) {
                cache.clear();
            }
        }
    }
}
