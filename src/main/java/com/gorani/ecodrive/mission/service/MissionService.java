package com.gorani.ecodrive.mission.service;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.mission.domain.MissionType;
import com.gorani.ecodrive.mission.dto.MissionView;
import com.gorani.ecodrive.mission.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 미션 조회 전용 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    // 사용자 미션 조회 저장소
    private final UserMissionRepository userMissionRepository;
    // 미션 할당 전용 서비스
    private final MissionAssignmentService missionAssignmentService;

    /**
     * 일일 미션 조회 메서드
     */
    public List<MissionView> getDailyMissions(Long userId) {
        return getMissions(userId, MissionType.DAILY);
    }

    /**
     * 주간 미션 조회 메서드
     */
    public List<MissionView> getWeeklyMissions(Long userId) {
        return getMissions(userId, MissionType.WEEKLY);
    }

    /**
     * 월간 미션 조회 메서드
     */
    public List<MissionView> getMonthlyMissions(Long userId) {
        return getMissions(userId, MissionType.MONTHLY);
    }

    /**
     * 조회 시 미할당 미션 보정 후 목록 반환 메서드
     */
    private List<MissionView> getMissions(Long userId, MissionType missionType) {
        LocalDate today = LocalDate.now(TimeZoneConstants.KST);
        MissionPeriodSupport.MissionPeriod period = MissionPeriodSupport.resolvePeriod(missionType, today);

        // REQUIRES_NEW 트랜잭션으로 할당 처리
        missionAssignmentService.ensureAssigned(userId, missionType);

        return userMissionRepository.findAssignedMissions(userId, missionType, period.startDate())
                .stream()
                .map(userMission -> new MissionView(
                        userMission.getId(),
                        userMission.getMissionPolicy().getId(),
                        userMission.getMissionPolicy().getTitle(),
                        userMission.getMissionPolicy().getDescription(),
                        userMission.getMissionTypeSnapshot(),
                        userMission.getCategorySnapshot(),
                        userMission.getTargetTypeSnapshot(),
                        userMission.getTargetValueSnapshot(),
                        userMission.getCurrentValue(),
                        userMission.getProgressRate(),
                        userMission.getRewardPointSnapshot(),
                        userMission.getStatus(),
                        userMission.getSlotNo(),
                        userMission.getPeriodStartDate(),
                        userMission.getPeriodEndDate(),
                        userMission.getRewardedAt()
                ))
                .toList();
    }
}
