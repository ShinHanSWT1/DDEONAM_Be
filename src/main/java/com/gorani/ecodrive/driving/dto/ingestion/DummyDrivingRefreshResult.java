package com.gorani.ecodrive.driving.dto.ingestion;

import java.util.List;

public record DummyDrivingRefreshResult(
        int processedBatches,
        int insertedSessions,
        int insertedEvents,
        int updatedUsers,
        int failedFiles,
        List<String> batchIds
) {
}
