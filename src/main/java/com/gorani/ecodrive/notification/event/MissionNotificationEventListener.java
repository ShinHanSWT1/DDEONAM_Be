package com.gorani.ecodrive.notification.event;

import com.gorani.ecodrive.notification.domain.NotificationType;
import com.gorani.ecodrive.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionNotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handle(MissionCompletedEvent event) {
        log.info("미션 달성 알림 이벤트 수신. userId={}, mission={}", event.userId(), event.missionTitle());
        try {
            notificationService.save(
                    event.userId(),
                    NotificationType.MISSION_COMPLETED,
                    "미션 달성!",
                    "'" + event.missionTitle() + "' 미션을 달성했습니다."
            );
        } catch (Exception e) {
            log.error("미션 달성 알림 저장 실패. userId={}", event.userId(), e);
        }
    }
}
