package com.gorani.ecodrive.driving.dto.ingestion;

import java.time.LocalDateTime;
import java.util.List;

public record DummyDrivingBatch(
        String batchId,
        LocalDateTime generatedAt,
        List<DummyDrivingSessionPayload> sessions
) {
}
