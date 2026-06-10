-- Module 5: Extend class_schedules with full Academic Scheduler schema

ALTER TABLE class_schedules
  ADD COLUMN IF NOT EXISTS schedule_tab          VARCHAR(20)  NOT NULL DEFAULT 'CLASSES',
  ADD COLUMN IF NOT EXISTS event_title           VARCHAR(200),
  ADD COLUMN IF NOT EXISTS location              VARCHAR(200),
  ADD COLUMN IF NOT EXISTS audience              VARCHAR(20),
  ADD COLUMN IF NOT EXISTS learning_objectives   JSONB        NOT NULL DEFAULT '[]',
  ADD COLUMN IF NOT EXISTS attendance_required   BOOLEAN      NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS reminder_minutes      INTEGER,
  ADD COLUMN IF NOT EXISTS assignment_session_id BIGINT REFERENCES assignment_sessions(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS date_mode             VARCHAR(20)  NOT NULL DEFAULT 'SINGLE',
  ADD COLUMN IF NOT EXISTS multiple_dates        JSONB,
  ADD COLUMN IF NOT EXISTS status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT';

-- Add CHECK constraints
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='class_schedules' AND constraint_name='chk_schedule_tab') THEN
    ALTER TABLE class_schedules ADD CONSTRAINT chk_schedule_tab CHECK (schedule_tab IN ('CLASSES','EVENTS','HOLIDAYS','OTHERS'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='class_schedules' AND constraint_name='chk_date_mode') THEN
    ALTER TABLE class_schedules ADD CONSTRAINT chk_date_mode CHECK (date_mode IN ('SINGLE','MULTIPLE','RECURRING'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='class_schedules' AND constraint_name='chk_audience') THEN
    ALTER TABLE class_schedules ADD CONSTRAINT chk_audience CHECK (audience IS NULL OR audience IN ('ALL','TEACHERS','STUDENTS','PARENTS'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name='class_schedules' AND constraint_name='chk_status') THEN
    ALTER TABLE class_schedules ADD CONSTRAINT chk_status CHECK (status IN ('DRAFT','ACTIVE','CANCELLED','COMPLETED'));
  END IF;
END $$;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_cs_session ON class_schedules(assignment_session_id);
CREATE INDEX IF NOT EXISTS idx_cs_date    ON class_schedules(schedule_date);
CREATE INDEX IF NOT EXISTS idx_cs_teacher ON class_schedules(teacher_profile_id);
