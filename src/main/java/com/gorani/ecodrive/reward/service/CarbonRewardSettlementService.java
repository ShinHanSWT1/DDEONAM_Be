package com.gorani.ecodrive.reward.service;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.reward.domain.RewardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonRewardSettlementService {

    private final JdbcTemplate jdbcTemplate;
    private final RewardPointGrantService rewardPointGrantService;

    /**
     * 전달(이전 월) 탄소 절감 리워드를 일괄 지급한다.
     */
    @Transactional
    public int settlePreviousMonth() {
        YearMonth targetMonth = YearMonth.from(LocalDate.now(TimeZoneConstants.KST)).minusMonths(1);
        return settleForMonth(targetMonth);
    }

    /**
     * 지정한 월의 탄소 절감 리워드를 지급한다.
     * 월간 누적 스냅샷 특성상 차량별 "해당 월 마지막 스냅샷"만 집계한다.
     */
    @Transactional
    public int settleForMonth(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<CarbonRewardCandidate> candidates = jdbcTemplate.query(
                """
                        with ranked_snapshots as (
                            select
                                user_id,
                                user_vehicle_id,
                                reward_point,
                                row_number() over (
                                    partition by user_id, user_vehicle_id
                                    order by snapshot_date desc, id desc
                                ) as rn
                            from carbon_reduction_snapshots
                            where snapshot_date between ? and ?
                        )
                        select
                            user_id,
                            coalesce(sum(reward_point), 0) as reward_point
                        from ranked_snapshots
                        where rn = 1
                        group by user_id
                        having coalesce(sum(reward_point), 0) > 0
                        """,
                (rs, rowNum) -> new CarbonRewardCandidate(
                        rs.getLong("user_id"),
                        rs.getInt("reward_point")
                ),
                startDate,
                endDate
        );

        int settledCount = 0;
        for (CarbonRewardCandidate candidate : candidates) {
            String periodKey = month.toString();
            String rewardRef = "CARBON:" + candidate.userId() + ":" + periodKey;
            String description = month.getYear() + "년 " + month.getMonthValue() + "월 탄소 절감 리워드";

            RewardPointGrantService.RewardGrantResult result = rewardPointGrantService.grantReward(
                    candidate.userId(),
                    RewardType.CARBON,
                    rewardRef,
                    candidate.rewardPoint(),
                    startDate,
                    endDate,
                    periodKey,
                    description
            );
            if (result != RewardPointGrantService.RewardGrantResult.SKIPPED) {
                settledCount++;
            }
        }

        log.info("탄소 절감 리워드 정산 완료. month={}, 대상={}, 처리={}", month, candidates.size(), settledCount);
        return settledCount;
    }

    private record CarbonRewardCandidate(Long userId, Integer rewardPoint) {
    }
}

