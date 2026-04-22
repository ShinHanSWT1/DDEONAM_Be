package com.gorani.ecodrive.mission.domain;

import com.gorani.ecodrive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * 사용자별 미션 인스턴스 엔티티
 */
public class UserMission {

    // 사용자 미션 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 미션 소유 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 원본 정책 템플릿
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_policy_id", nullable = false)
    private MissionPolicy missionPolicy;

    // 미션 기간 시작일
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    // 미션 기간 종료일
    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    // 누적 진행 값
    @Column(name = "current_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentValue;

    // 진행률 값
    @Column(name = "progress_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressRate;

    // 미션 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status;

    // 보상 지급 시각
    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    // 할당 시각
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 할당 시점 목표 판정 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type_snapshot", nullable = false, length = 30)
    private MissionTargetType targetTypeSnapshot;

    // 할당 시점 목표 기준값
    @Column(name = "target_value_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetValueSnapshot;

    // 할당 시점 보상 포인트
    @Column(name = "reward_point_snapshot", nullable = false)
    private Integer rewardPointSnapshot;

    // 할당 시점 미션 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type_snapshot", nullable = false, length = 20)
    private MissionType missionTypeSnapshot;

    // 할당 시점 미션 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "category_snapshot", nullable = false, length = 30)
    private MissionCategory categorySnapshot;

    // 동일 기간 내 미션 슬롯 번호
    @Column(name = "slot_no", nullable = false)
    private Short slotNo;

    /**
     * 정책 기반 사용자 미션 생성 메서드
     */
    public static UserMission assign(
            User user,
            MissionPolicy policy,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            short slotNo,
            LocalDateTime now
    ) {
        UserMission userMission = new UserMission();
        userMission.user = user;
        userMission.missionPolicy = policy;
        userMission.periodStartDate = periodStartDate;
        userMission.periodEndDate = periodEndDate;
        userMission.currentValue = BigDecimal.ZERO;
        userMission.progressRate = BigDecimal.ZERO;
        userMission.status = MissionStatus.IN_PROGRESS;
        userMission.createdAt = now;
        userMission.targetTypeSnapshot = policy.getTargetType();
        userMission.targetValueSnapshot = policy.getTargetValue();
        userMission.rewardPointSnapshot = policy.getRewardPoint();
        userMission.missionTypeSnapshot = policy.getMissionType();
        userMission.categorySnapshot = policy.getCategory();
        userMission.slotNo = slotNo;
        return userMission;
    }

    public void applyProgress(BigDecimal currentValue, BigDecimal progressRate, boolean achieved) {
        // 만료된 미션은 상태/진행도 변경 대상에서 제외한다.
        if (this.status == MissionStatus.EXPIRED) {
            return;
        }
        this.currentValue = currentValue;
        this.progressRate = progressRate;
        if (achieved && this.status == MissionStatus.IN_PROGRESS) {
            this.status = MissionStatus.COMPLETED;
        }
    }

    public void markRewarded(LocalDateTime rewardedAt) {
        if (this.rewardedAt == null) {
            this.rewardedAt = rewardedAt;
        }
    }
}
