-- Module 5: Class Schedules

CREATE TABLE class_schedules (
    id                      BIGSERIAL    PRIMARY KEY,
    session_id              BIGINT       NOT NULL REFERENCES assignment_sessions(id) ON DELETE CASCADE,
    teacher_profile_id      BIGINT       NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    assignment_group_id     BIGINT       NOT NULL REFERENCES assignment_groups(id) ON DELETE CASCADE,
    subject_id              BIGINT       REFERENCES course_nodes(id),
    schedule_type           VARCHAR(50)  NOT NULL CHECK (schedule_type IN (
                                            'REGULAR_CLASS','REVISION_SESSION','EXTRA_CLASS',
                                            'PRACTICAL_SESSION','EXAM_PREPARATION','PARENT_SESSION','WORKSHOP'
                                        )),
    date_mode               VARCHAR(20)  NOT NULL CHECK (date_mode IN ('SINGLE_DAY','MULTIPLE_DAYS','RECURRING')),

    -- Single day / start date
    schedule_date           DATE         NOT NULL,
    end_date                DATE,  -- for multiple days

    -- Time
    start_time              TIME         NOT NULL,
    end_time                TIME         NOT NULL,
    duration_minutes        INT,  -- auto-calculated

    -- Lesson details
    topic                   VARCHAR(255) NOT NULL,
    description             TEXT,
    learning_objectives     TEXT,  -- JSON array of strings

    -- Resources
    classroom_id            BIGINT       REFERENCES classrooms(id),

    -- Settings
    enable_attendance       BOOLEAN      DEFAULT false,
    notify_students         BOOLEAN      DEFAULT false,
    notify_parents          BOOLEAN      DEFAULT false,
    send_reminder           BOOLEAN      DEFAULT false,

    -- Color coding
    event_color             VARCHAR(20)  DEFAULT 'blue',

    -- Audit
    created_by              BIGINT       NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by              BIGINT       REFERENCES users(id),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                 INT          DEFAULT 1
);

CREATE INDEX idx_schedules_teacher ON class_schedules(teacher_profile_id);
CREATE INDEX idx_schedules_group ON class_schedules(assignment_group_id);
CREATE INDEX idx_schedules_session ON class_schedules(session_id);
CREATE INDEX idx_schedules_date ON class_schedules(schedule_date);
CREATE INDEX idx_schedules_classroom ON class_schedules(classroom_id);

CREATE TRIGGER trg_class_schedules_updated_at
    BEFORE UPDATE ON class_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
