-- Module 5: Full schedule_recurrence table with new schema

DROP TABLE IF EXISTS schedule_recurrence CASCADE;

CREATE TABLE schedule_recurrence (
  id                    BIGSERIAL    PRIMARY KEY,
  schedule_id           BIGINT       NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
  pattern               VARCHAR(20)  NOT NULL,
  interval_value        INTEGER      NOT NULL DEFAULT 1,
  days_of_week          INTEGER[],
  end_condition         VARCHAR(20)  NOT NULL DEFAULT 'NEVER',
  end_date              DATE,
  occurrence_count      INTEGER,
  occurrences_generated INTEGER      NOT NULL DEFAULT 0,
  created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_pattern       CHECK (pattern       IN ('DAILY','WEEKLY','MONTHLY')),
  CONSTRAINT chk_end_condition CHECK (end_condition IN ('NEVER','UNTIL_DATE','COUNT')),
  CONSTRAINT uq_schedule_recurrence UNIQUE (schedule_id)
);

CREATE INDEX IF NOT EXISTS idx_sr_schedule ON schedule_recurrence(schedule_id);
