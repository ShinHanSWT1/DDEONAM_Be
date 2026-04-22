package com.gorani.ecodrive.mission.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mission_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * 미션 정책 템플릿 엔티티
 */
public class MissionPolicy {

    // 정책 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 미션 주기 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 20)
    private MissionType missionType;

    // 미션 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MissionCategory category;

    // 정책 제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 정책 설명
    @Column(name = "description", length = 255)
    private String description;

    // 목표 판정 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private MissionTargetType targetType;

    // 목표 기준값
    @Column(name = "target_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetValue;

    // 보상 포인트
    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;

    // 정책 활성 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionPolicyStatus status;

    // 추출 가중치
    @Column(name = "weight", nullable = false)
    private Integer weight;

    // 재할당 제한 기간 수
    @Column(name = "cooldown_periods", nullable = false)
    private Integer cooldownPeriods;

    // 생성 시각
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 수정 시각
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
