package com.gorani.ecodrive.mission.repository;

import com.gorani.ecodrive.mission.domain.MissionType;
import com.gorani.ecodrive.mission.domain.MissionStatus;
import com.gorani.ecodrive.mission.domain.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용자 미션 조회 저장소
 */
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    /**
     * 기간 내 미션 할당 존재 여부 조회 메서드
     */
    @Query("""
            select (count(um) > 0)
            from UserMission um
            where um.user.id = :userId
              and um.missionTypeSnapshot = :missionType
              and um.periodStartDate = :periodStartDate
            """)
    boolean existsAssignedInPeriod(
            @Param("userId") Long userId,
            @Param("missionType") MissionType missionType,
            @Param("periodStartDate") LocalDate periodStartDate
    );

    /**
     * 기간 내 할당 미션 목록 조회 메서드
     */
    @Query("""
            select um
            from UserMission um
            where um.user.id = :userId
              and um.missionTypeSnapshot = :missionType
              and um.periodStartDate = :periodStartDate
            order by um.slotNo asc
            """)
    List<UserMission> findAssignedMissions(
            @Param("userId") Long userId,
            @Param("missionType") MissionType missionType,
            @Param("periodStartDate") LocalDate periodStartDate
    );

    @Query("""
            select um
            from UserMission um
            where um.status = :status
              and um.rewardedAt is null
              and um.periodEndDate < :baseDate
            order by um.id asc
            """)
    List<UserMission> findRewardTargets(
            @Param("status") MissionStatus status,
            @Param("baseDate") LocalDate baseDate
    );
}
