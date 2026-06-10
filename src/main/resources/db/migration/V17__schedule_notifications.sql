-- Module 5: Schedule Notifications

CREATE TABLE schedule_notifications (
    id                      BIGSERIAL    PRIMARY KEY,
    class_schedule_id       BIGINT       NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
    occurrence_id           BIGINT       REFERENCES schedule_occurrences(id) ON DELETE CASCADE,
    recipient_type          VARCHAR(20)  NOT NULL CHECK (recipient_type IN ('TEACHER','STUDENT','PARENT')),
    recipient_id            BIGINT,  -- user_id or student_profile_id or parent_id

    notification_type       VARCHAR(50)  NOT NULL CHECK (notification_type IN (
                                            'SCHEDULE_CREATED','SCHEDULE_UPDATED','SCHEDULE_REMINDER',
                                            'SCHEDULE_CANCELLED','ATTENDANCE_REQUEST'
                                        )),

    message_subject         VARCHAR(255),
    message_body            TEXT,
    notification_channel    VARCHAR(20)  DEFAULT 'EMAIL' CHECK (notification_channel IN ('EMAIL','SMS','IN_APP')),

    is_sent                 BOOLEAN      DEFAULT false,
    sent_at                 TIMESTAMPTZ,
    is_read                 BOOLEAN      DEFAULT false,
    read_at                 TIMESTAMPTZ,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_schedule ON schedule_notifications(class_schedule_id);
CREATE INDEX idx_notifications_recipient ON schedule_notifications(recipient_type, recipient_id);
CREATE INDEX idx_notifications_sent ON schedule_notifications(is_sent);
CREATE INDEX idx_notifications_read ON schedule_notifications(is_read);
