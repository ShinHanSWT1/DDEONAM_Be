ALTER TABLE users
    ADD COLUMN IF NOT EXISTS representative_user_vehicle_id BIGINT;

UPDATE users u
SET representative_user_vehicle_id = uv.id
FROM (
         SELECT DISTINCT ON (user_id)
             user_id,
             id
         FROM user_vehicles
         ORDER BY user_id, registered_at DESC, id DESC
     ) uv
WHERE u.id = uv.user_id
  AND u.representative_user_vehicle_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'users'
          AND constraint_name = 'fk_users_representative_user_vehicle'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_representative_user_vehicle
                FOREIGN KEY (representative_user_vehicle_id) REFERENCES user_vehicles (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_representative_user_vehicle_id
    ON users (representative_user_vehicle_id);

ALTER TABLE driving_score_snapshots
    ADD COLUMN IF NOT EXISTS user_vehicle_id BIGINT;

WITH selected_user_vehicles AS (
    SELECT DISTINCT ON (user_id)
        user_id,
        id
    FROM user_vehicles
    ORDER BY user_id, registered_at DESC, id DESC
)
UPDATE driving_score_snapshots dss
SET user_vehicle_id = suv.id
FROM selected_user_vehicles suv
WHERE dss.user_id = suv.user_id
  AND dss.user_vehicle_id IS NULL;

ALTER TABLE carbon_reduction_snapshots
    ADD COLUMN IF NOT EXISTS user_vehicle_id BIGINT;

WITH selected_user_vehicles AS (
    SELECT DISTINCT ON (user_id)
        user_id,
        id
    FROM user_vehicles
    ORDER BY user_id, registered_at DESC, id DESC
)
UPDATE carbon_reduction_snapshots crs
SET user_vehicle_id = suv.id
FROM selected_user_vehicles suv
WHERE crs.user_id = suv.user_id
  AND crs.user_vehicle_id IS NULL;

ALTER TABLE driving_score_snapshots
    ALTER COLUMN user_vehicle_id SET NOT NULL;

ALTER TABLE carbon_reduction_snapshots
    ALTER COLUMN user_vehicle_id SET NOT NULL;

WITH ranked_score_snapshots AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY user_vehicle_id, snapshot_date
            ORDER BY created_at DESC NULLS LAST, id DESC
        ) AS row_num
    FROM driving_score_snapshots
)
DELETE FROM driving_score_change_logs dcl
USING ranked_score_snapshots ranked
WHERE dcl.snapshot_id = ranked.id
  AND ranked.row_num > 1;

WITH ranked_score_snapshots AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY user_vehicle_id, snapshot_date
            ORDER BY created_at DESC NULLS LAST, id DESC
        ) AS row_num
    FROM driving_score_snapshots
)
DELETE FROM driving_score_snapshots dss
USING ranked_score_snapshots ranked
WHERE dss.id = ranked.id
  AND ranked.row_num > 1;

WITH ranked_carbon_snapshots AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY user_vehicle_id, snapshot_date
            ORDER BY created_at DESC NULLS LAST, id DESC
        ) AS row_num
    FROM carbon_reduction_snapshots
)
DELETE FROM carbon_reduction_snapshots crs
USING ranked_carbon_snapshots ranked
WHERE crs.id = ranked.id
  AND ranked.row_num > 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'driving_score_snapshots'
          AND constraint_name = 'fk_driving_score_snapshots_user_vehicle'
    ) THEN
        ALTER TABLE driving_score_snapshots
            ADD CONSTRAINT fk_driving_score_snapshots_user_vehicle
                FOREIGN KEY (user_vehicle_id) REFERENCES user_vehicles (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'carbon_reduction_snapshots'
          AND constraint_name = 'fk_carbon_reduction_snapshots_user_vehicle'
    ) THEN
        ALTER TABLE carbon_reduction_snapshots
            ADD CONSTRAINT fk_carbon_reduction_snapshots_user_vehicle
                FOREIGN KEY (user_vehicle_id) REFERENCES user_vehicles (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_driving_score_snapshots_vehicle_date
    ON driving_score_snapshots (user_vehicle_id, snapshot_date);

CREATE UNIQUE INDEX IF NOT EXISTS ux_carbon_reduction_snapshots_vehicle_date
    ON carbon_reduction_snapshots (user_vehicle_id, snapshot_date);

CREATE INDEX IF NOT EXISTS idx_driving_score_snapshots_user_vehicle_date
    ON driving_score_snapshots (user_id, user_vehicle_id, snapshot_date DESC);

CREATE INDEX IF NOT EXISTS idx_carbon_reduction_snapshots_user_vehicle_date
    ON carbon_reduction_snapshots (user_id, user_vehicle_id, snapshot_date DESC);
