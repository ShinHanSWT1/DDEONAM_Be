package com.gorani.ecodrive.driving.dto.ingestion;

public record DummyDrivingAutomationResult(
        DummyDrivingGenerationResult generation,
        DummyDrivingRefreshResult refresh
) {
}
