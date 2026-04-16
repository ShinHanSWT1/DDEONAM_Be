package com.gorani.ecodrive.mission.scheduler;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.mission.domain.MissionType;
import com.gorani.ecodrive.mission.service.MissionAssignmentService;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/**
 * 주기적 미션 할당 스케줄러
 */
@Log4j2
public class MissionAssignmentScheduler {

    // 할당 대상 사용자 조회 저장소
    private final UserRepository userRepository;
    // 미션 할당 서비스
    private final MissionAssignmentService missionAssignmentService;

    /**
     * 일일 미션 배치 실행 메서드
     */
    @Scheduled(cron = "0 5 0 * * *", zone = TimeZoneConstants.ASIA_SEOUL)
    public void assignDailyMissions() {
        userRepository.findAllUserIds()
                .forEach(userId -> {
                    try {
                        missionAssignmentService.ensureAssigned(userId, MissionType.DAILY);
                    } catch (Exception e) {
                        log.error("일일 미션 할당 실패 - userId={}", userId, e);
                    }
                });
    }

    /**
     * 주간 미션 배치 실행 메서드
     */
    @Scheduled(cron = "0 10 0 * * MON", zone = TimeZoneConstants.ASIA_SEOUL)
    public void assignWeeklyMissions() {
        userRepository.findAllUserIds()
                .forEach(userId -> {
                    try {
                        missionAssignmentService.ensureAssigned(userId, MissionType.WEEKLY);
                    } catch (Exception e) {
                        log.error("주간 미션 할당 실패 - userId={}", userId, e);
                    }
                });
    }

    /**
     * 월간 미션 배치 실행 메서드
     */
    @Scheduled(cron = "0 15 0 1 * *", zone = TimeZoneConstants.ASIA_SEOUL)
    public void assignMonthlyMissions() {
        userRepository.findAllUserIds()
                .forEach(userId -> {
                    try {
                        missionAssignmentService.ensureAssigned(userId, MissionType.MONTHLY);
                    } catch (Exception e) {
                        log.error("월간 미션 할당 실패 - userId={}", userId, e);
                    }
                });
    }
}
