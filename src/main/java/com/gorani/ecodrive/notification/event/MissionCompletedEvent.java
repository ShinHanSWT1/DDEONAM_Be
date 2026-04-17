package com.gorani.ecodrive.notification.event;

public record MissionCompletedEvent(
        Long userId,
        String missionTitle
) {
}
