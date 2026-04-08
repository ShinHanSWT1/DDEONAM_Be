package com.gorani.ecodrive.driving.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DummyDrivingEventPayload(
        String eventType,
        BigDecimal eventValue,
        LocalDateTime occurredAt,
        Integer scoreDelta
) {
}
