WITH seed_policies AS (
    SELECT *
    FROM (VALUES
        ('MONTHLY', 'ECO',    '월간 500km 이상 주행',      '해당 월 총 주행거리 500km 이상 달성',         'DISTANCE_KM_GTE',       500.00, 1000, 100, 1),
        ('MONTHLY', 'SAFETY', '월간 안전점수 90점 이상',   '해당 월 안전점수 90점 이상 달성',             'SAFE_SCORE_GTE',         90.00, 1100, 100, 1),
        ('MONTHLY', 'HABIT',  '월간 공회전 300분 이하',    '해당 월 총 공회전 시간을 300분 이하로 유지',  'IDLING_MINUTES_LTE',    300.00,  950,  90, 1)
    ) AS t(
        mission_type,
        category,
        title,
        description,
        target_type,
        target_value,
        reward_point,
        weight,
        cooldown_periods
    )
)
INSERT INTO mission_policies (
    mission_type,
    category,
    title,
    description,
    target_type,
    target_value,
    reward_point,
    status,
    weight,
    cooldown_periods,
    created_at,
    updated_at
)
SELECT
    s.mission_type,
    s.category,
    s.title,
    s.description,
    s.target_type,
    s.target_value,
    s.reward_point,
    'ACTIVE',
    s.weight,
    s.cooldown_periods,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_policies s
WHERE NOT EXISTS (
    SELECT 1
    FROM mission_policies mp
    WHERE mp.mission_type = s.mission_type
      AND mp.category = s.category
      AND mp.title = s.title
);
