-- Module 5: Align legacy V14/V15 schema with current JPA entities.
-- V19 used ADD COLUMN IF NOT EXISTS, which silently no-ops for columns that
-- already existed in V14 with a different type or CHECK values.

-- 1) learning_objectives: V14 created it as TEXT, entity maps jsonb
ALTER TABLE class_schedules
  ALTER COLUMN learning_objectives TYPE JSONB
    USING CASE
      WHEN learning_objectives IS NULL OR learning_objectives = '' THEN '[]'::jsonb
      ELSE learning_objectives::jsonb
    END;
ALTER TABLE class_schedules
  ALTER COLUMN learning_objectives SET DEFAULT '[]'::jsonb;
UPDATE class_schedules SET learning_objectives = '[]'::jsonb WHERE learning_objectives IS NULL;
ALTER TABLE class_schedules
  ALTER COLUMN learning_objectives SET NOT NULL;

-- 2) date_mode: V14 inline CHECK used SINGLE_DAY/MULTIPLE_DAYS; the DateMode
--    enum and V19's chk_date_mode use SINGLE/MULTIPLE
ALTER TABLE class_schedules DROP CONSTRAINT IF EXISTS class_schedules_date_mode_check;
UPDATE class_schedules SET date_mode = 'SINGLE'   WHERE date_mode = 'SINGLE_DAY';
UPDATE class_schedules SET date_mode = 'MULTIPLE' WHERE date_mode = 'MULTIPLE_DAYS';

-- 3) schedule_type: V14 CHECK values don't match the ScheduleType enum,
--    and the entity allows NULL (non-class tabs have no schedule type)
ALTER TABLE class_schedules DROP CONSTRAINT IF EXISTS class_schedules_schedule_type_check;
ALTER TABLE class_schedules ALTER COLUMN schedule_type DROP NOT NULL;
UPDATE class_schedules SET schedule_type = CASE schedule_type
    WHEN 'REGULAR_CLASS'     THEN 'REGULAR'
    WHEN 'REVISION_SESSION'  THEN 'REVISION'
    WHEN 'EXTRA_CLASS'       THEN 'EXTRA'
    WHEN 'PRACTICAL_SESSION' THEN 'PRACTICAL'
    WHEN 'EXAM_PREPARATION'  THEN 'EXAM_PREP'
    WHEN 'PARENT_SESSION'    THEN 'PARENT'
    ELSE schedule_type
  END;
ALTER TABLE class_schedules ADD CONSTRAINT chk_schedule_type
  CHECK (schedule_type IS NULL OR schedule_type IN
         ('REGULAR','REVISION','EXTRA','PRACTICAL','EXAM_PREP','PARENT','WORKSHOP'));

-- 4) topic: entity allows NULL (EVENTS/HOLIDAYS/OTHERS tabs have no topic)
ALTER TABLE class_schedules ALTER COLUMN topic DROP NOT NULL;

-- 5) session_id: legacy V14 column not mapped by the entity
--    (superseded by assignment_session_id added in V19)
ALTER TABLE class_schedules ALTER COLUMN session_id DROP NOT NULL;

-- 6) schedule_occurrences.status: V15 CHECK used SCHEDULED/RESCHEDULED;
--    entity uses ScheduleStatus (DRAFT/ACTIVE/CANCELLED/COMPLETED)
ALTER TABLE schedule_occurrences DROP CONSTRAINT IF EXISTS schedule_occurrences_status_check;
UPDATE schedule_occurrences SET status = 'ACTIVE'
  WHERE status IN ('SCHEDULED','RESCHEDULED') OR status IS NULL;
ALTER TABLE schedule_occurrences ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE schedule_occurrences ALTER COLUMN status SET NOT NULL;
ALTER TABLE schedule_occurrences ADD CONSTRAINT chk_occurrence_status
  CHECK (status IN ('DRAFT','ACTIVE','CANCELLED','COMPLETED'));
