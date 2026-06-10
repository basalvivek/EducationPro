-- Module 5: Schedule indexes and triggers

-- Composite index for calendar queries
CREATE INDEX IF NOT EXISTS idx_so_schedule_date
  ON schedule_occurrences(schedule_id, occurrence_date);

-- Partial index for conflict checks (only active schedules)
CREATE INDEX IF NOT EXISTS idx_cs_active
  ON class_schedules(teacher_profile_id, schedule_date)
  WHERE status != 'CANCELLED';

-- Dedup index for conflicts
CREATE INDEX IF NOT EXISTS idx_sc_pair
  ON schedule_conflicts(schedule_id_1, schedule_id_2);
