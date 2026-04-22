-- Fix line_realtime.weekly_demand type mismatch for dynamic simulation writes
ALTER TABLE IF EXISTS line_realtime
  ALTER COLUMN weekly_demand TYPE TEXT
  USING CASE
    WHEN weekly_demand IS NULL THEN NULL
    ELSE weekly_demand::text
  END;
