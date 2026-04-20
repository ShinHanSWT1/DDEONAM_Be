package com.gorani.ecodrive.driving.service.insight;

import com.gorani.ecodrive.driving.dto.query.DrivingInsightResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DrivingInsightCache {

    private final Map<String, DrivingInsightResponse> cache = new ConcurrentHashMap<>();

    public DrivingInsightResponse get(String key) {
        return cache.get(key);
    }

    public void put(String key, DrivingInsightResponse response) {
        cache.put(key, response);
    }

    public String key(Long userId, Long userVehicleId, String fingerprint) {
        return "driving-insight:%d:%s:%s:%s".formatted(
                userId,
                userVehicleId == null ? "all" : userVehicleId,
                LocalDate.now(),
                fingerprint
        );
    }
}
