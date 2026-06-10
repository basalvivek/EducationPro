-- Module 5: Schedule Conflict Detection

CREATE TABLE schedule_conflicts (
    id                      BIGSERIAL    PRIMARY KEY,
    class_schedule_id       BIGINT       NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
    conflict_type           VARCHAR(50)  NOT NULL CHECK (conflict_type IN (
                                            'TEACHER_CONFLICT','GROUP_CONFLICT','CLASSROOM_CONFLICT'
                                        )),
    conflict_description    TEXT         NOT NULL,
    conflicting_schedule_id BIGINT       REFERENCES class_schedules(id),
    severity                VARCHAR(20)  DEFAULT 'WARNING' CHECK (severity IN ('INFO','WARNING','ERROR')),
    is_resolved             BOOLEAN      DEFAULT false,
    resolution_note         TEXT,
    detected_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at             TIMESTAMPTZ
);

CREATE INDEX idx_conflicts_schedule ON schedule_conflicts(class_schedule_id);
CREATE INDEX idx_conflicts_type ON schedule_conflicts(conflict_type);
CREATE INDEX idx_conflicts_resolved ON schedule_conflicts(is_resolved);
