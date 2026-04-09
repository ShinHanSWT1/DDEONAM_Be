package com.gorani.ecodrive.mission.service;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.mission.domain.MissionPolicy;
import com.gorani.ecodrive.mission.domain.MissionPolicyStatus;
import com.gorani.ecodrive.mission.domain.MissionType;
import com.gorani.ecodrive.mission.domain.UserMission;
import com.gorani.ecodrive.mission.repository.MissionPolicyRepository;
import com.gorani.ecodrive.mission.repository.UserMissionRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * 미션 할당 전용 서비스
 */
@Service
@RequiredArgsConstructor
public class MissionAssignmentService {

    // 미션 타입별 기본 할당 슬롯 수
    private static final Map<MissionType, Integer> REQUIRED_SLOT_COUNT = Map.of(
            MissionType.DAILY, 2,
            MissionType.WEEKLY, 2,
            MissionType.MONTHLY, 2
    );

    // 사용자 미션 저장소
    private final UserMissionRepository userMissionRepository;
    // 사용자 저장소
    private final UserRepository userRepository;
    // 미션 정책 저장소
    private final MissionPolicyRepository missionPolicyRepository;

    /**
     * 기간 미할당 시 신규 미션 할당 메서드
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureAssigned(Long userId, MissionType missionType) {
        LocalDate today = LocalDate.now(TimeZoneConstants.KST);
        MissionPeriodSupport.MissionPeriod period = MissionPeriodSupport.resolvePeriod(missionType, today);

        if (userMissionRepository.existsAssignedInPeriod(userId, missionType, period.startDate())) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int requiredCount = REQUIRED_SLOT_COUNT.getOrDefault(missionType, 0);
        if (requiredCount <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<MissionPolicy> activePolicies = missionPolicyRepository.findAllByMissionTypeAndStatusOrderByIdAsc(
                missionType,
                MissionPolicyStatus.ACTIVE
        );

        if (activePolicies.size() < requiredCount) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long seed = Objects.hash(userId, missionType.name(), period.startDate());
        List<MissionPolicy> selectedPolicies = selectPoliciesDeterministically(activePolicies, requiredCount, seed);

        LocalDateTime now = LocalDateTime.now(TimeZoneConstants.KST);
        List<UserMission> assignments = new ArrayList<>(selectedPolicies.size());
        for (int i = 0; i < selectedPolicies.size(); i++) {
            assignments.add(UserMission.assign(
                    user,
                    selectedPolicies.get(i),
                    period.startDate(),
                    period.endDate(),
                    (short) (i + 1),
                    now
            ));
        }
        try {
            userMissionRepository.saveAll(assignments);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 선점 성공 케이스 처리
            if (userMissionRepository.existsAssignedInPeriod(userId, missionType, period.startDate())) {
                return;
            }
            throw e;
        }
    }

    /**
     * seed 기반 가중 랜덤 정책 선택 메서드
     */
    private List<MissionPolicy> selectPoliciesDeterministically(
            List<MissionPolicy> candidates,
            int requiredCount,
            long seed
    ) {
        List<MissionPolicy> pool = new ArrayList<>(candidates);
        List<MissionPolicy> selected = new ArrayList<>(requiredCount);
        Random random = new Random(seed);

        while (selected.size() < requiredCount && !pool.isEmpty()) {
            int selectedIndex = pickWeightedIndex(pool, random);
            selected.add(pool.remove(selectedIndex));
        }
        return selected;
    }

    /**
     * weight 합 기준 단일 인덱스 선택 메서드
     */
    private int pickWeightedIndex(List<MissionPolicy> policies, Random random) {
        int totalWeight = policies.stream()
                .map(MissionPolicy::getWeight)
                .filter(Objects::nonNull)
                .mapToInt(weight -> Math.max(weight, 1))
                .sum();

        int randomValue = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < policies.size(); i++) {
            int weight = policies.get(i).getWeight() == null ? 1 : Math.max(policies.get(i).getWeight(), 1);
            cumulative += weight;
            if (randomValue < cumulative) {
                return i;
            }
        }
        return policies.size() - 1;
    }
}
