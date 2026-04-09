package com.gorani.ecodrive.mission.repository;

import com.gorani.ecodrive.mission.domain.MissionPolicy;
import com.gorani.ecodrive.mission.domain.MissionPolicyStatus;
import com.gorani.ecodrive.mission.domain.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 미션 정책 조회 저장소
 */
public interface MissionPolicyRepository extends JpaRepository<MissionPolicy, Long> {
    /**
     * 타입/상태 기준 정책 목록 조회 메서드
     */
    List<MissionPolicy> findAllByMissionTypeAndStatus(MissionType missionType, MissionPolicyStatus status);
}
