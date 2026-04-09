WITH seed_policies AS (
    SELECT *
    FROM (VALUES
        ('DAILY', 'ECO',    '오늘 20km 이상 주행',         '하루 총 주행거리 20km 이상 달성',           'DISTANCE_KM_GTE',      20.00,  80, 100, 1),
        ('DAILY', 'ECO',    '공회전 15분 이하 유지',       '하루 공회전 시간 15분 이하 달성',           'IDLING_MINUTES_LTE',   15.00,  70,  90, 1),
        ('DAILY', 'SAFETY', '급가속 3회 이하 주행',        '하루 급가속 횟수 3회 이하 달성',            'HARD_ACCEL_COUNT_LTE',  3.00,  90, 110, 1),
        ('DAILY', 'SAFETY', '급감속 3회 이하 주행',        '하루 급감속 횟수 3회 이하 달성',            'HARD_BRAKE_COUNT_LTE',  3.00,  90, 110, 1),
        ('DAILY', 'SAFETY', '안전점수 85점 이상 달성',     '일일 안전점수 85점 이상 달성',              'SAFE_SCORE_GTE',       85.00, 100, 100, 1),
        ('DAILY', 'HABIT',  '야간운전 비율 20% 이하 유지', '하루 야간운전 비율 20% 이하 달성',          'NIGHT_DRIVE_RATIO_LTE',20.00,  70,  80, 1),
        ('WEEKLY','ECO',    '주간 120km 이상 주행',        '주간 총 주행거리 120km 이상 달성',          'DISTANCE_KM_GTE',     120.00, 300, 100, 1),
        ('WEEKLY','SAFETY', '주간 급가속 15회 이하',       '주간 급가속 횟수 15회 이하 달성',           'HARD_ACCEL_COUNT_LTE', 15.00, 320, 100, 1),
        ('WEEKLY','SAFETY', '주간 안전점수 88점 이상',     '주간 안전점수 88점 이상 달성',              'SAFE_SCORE_GTE',       88.00, 350, 100, 1),
        ('WEEKLY','HABIT',  '무급가감속 5일 이상 달성',    '주간 기준 급가속/급감속 없는 날 5일 이상',  'NO_HARD_EVENT_DAYS_GTE', 5.00, 280, 90, 1)
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
