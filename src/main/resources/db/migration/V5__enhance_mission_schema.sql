ALTER TABLE mission_policies
    ADD COLUMN IF NOT EXISTS weight INTEGER NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS cooldown_periods INTEGER NOT NULL DEFAULT 1;

ALTER TABLE user_missions
    ADD COLUMN IF NOT EXISTS target_type_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS target_value_snapshot NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS reward_point_snapshot INTEGER,
    ADD COLUMN IF NOT EXISTS mission_type_snapshot VARCHAR(20),
    ADD COLUMN IF NOT EXISTS category_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS slot_no SMALLINT;

UPDATE user_missions um
SET target_type_snapshot = mp.target_type,
    target_value_snapshot = mp.target_value,
    reward_point_snapshot = mp.reward_point,
    mission_type_snapshot = mp.mission_type,
    category_snapshot = mp.category
FROM mission_policies mp
WHERE um.mission_policy_id = mp.id
  AND (
    um.target_type_snapshot IS NULL OR
    um.target_value_snapshot IS NULL OR
    um.reward_point_snapshot IS NULL OR
    um.mission_type_snapshot IS NULL OR
    um.category_snapshot IS NULL
    );

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, mission_type_snapshot, period_start_date
               ORDER BY created_at, id
               ) AS rn
    FROM user_missions
)
UPDATE user_missions um
SET slot_no = r.rn
FROM ranked r
WHERE um.id = r.id
  AND um.slot_no IS NULL;

ALTER TABLE user_missions
    ALTER COLUMN target_type_snapshot SET NOT NULL,
    ALTER COLUMN target_value_snapshot SET NOT NULL,
    ALTER COLUMN reward_point_snapshot SET NOT NULL,
    ALTER COLUMN mission_type_snapshot SET NOT NULL,
    ALTER COLUMN category_snapshot SET NOT NULL,
    ALTER COLUMN slot_no SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_missions_period_slot
    ON user_missions (user_id, mission_type_snapshot, period_start_date, slot_no);

CREATE INDEX IF NOT EXISTS idx_user_missions_user_status_period
    ON user_missions (user_id, status, period_start_date, period_end_date);

CREATE INDEX IF NOT EXISTS idx_user_missions_policy
    ON user_missions (mission_policy_id);
