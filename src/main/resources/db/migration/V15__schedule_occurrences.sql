-- Module 5: Schedule Occurrences (for recurring schedules)

CREATE TABLE schedule_occurrences (
    id                  BIGSERIAL    PRIMARY KEY,
    class_schedule_id   BIGINT       NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
    occurrence_date     DATE         NOT NULL,
    start_time          TIME         NOT NULL,
    end_time            TIME         NOT NULL,
    status              VARCHAR(20)  DEFAULT 'SCHEDULED' CHECK (status IN (
                                        'SCHEDULED','CANCELLED','COMPLETED','RESCHEDULED'
                                    )),
    cancellation_reason TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Recurrence settings
CREATE TABLE schedule_recurrence (
    id                  BIGSERIAL    PRIMARY KEY,
    class_schedule_id   BIGINT       NOT NULL UNIQUE REFERENCES class_schedules(id) ON DELETE CASCADE,
    frequency           VARCHAR(20)  NOT NULL CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','CUSTOM')),

    -- Weekly recurrence
    monday              BOOLEAN      DEFAULT false,
    tuesday             BOOLEAN      DEFAULT false,
    wednesday           BOOLEAN      DEFAULT false,
    thursday            BOOLEAN      DEFAULT false,
    friday              BOOLEAN      DEFAULT false,
    saturday            BOOLEAN      DEFAULT false,
    sunday              BOOLEAN      DEFAULT false,

    -- End condition
    end_condition       VARCHAR(20)  CHECK (end_condition IN ('NEVER_ENDS','UNTIL_DATE','NUM_OCCURRENCES')),
    end_date            DATE,
    num_occurrences     INT,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_occurrences_schedule ON schedule_occurrences(class_schedule_id);
CREATE INDEX idx_occurrences_date ON schedule_occurrences(occurrence_date);
CREATE INDEX idx_occurrences_status ON schedule_occurrences(status);

CREATE TRIGGER trg_schedule_occurrences_updated_at
    BEFORE UPDATE ON schedule_occurrences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_schedule_recurrence_updated_at
    BEFORE UPDATE ON schedule_recurrence
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
