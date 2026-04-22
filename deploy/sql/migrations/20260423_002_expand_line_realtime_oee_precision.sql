-- Allow percent-style OEE values like 90.0000 in dynamic simulation results
ALTER TABLE IF EXISTS line_realtime
  ALTER COLUMN oee TYPE NUMERIC(7,4);
