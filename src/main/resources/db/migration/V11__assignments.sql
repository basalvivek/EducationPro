-- Module: Assignment Sessions — groups, student enrolments, teacher-group mappings

CREATE TABLE assignment_sessions (
    id              BIGSERIAL    PRIMARY KEY,
    course_node_id  BIGINT       NOT NULL REFERENCES course_nodes(id) ON DELETE CASCADE,
    scope_node_id   BIGINT       REFERENCES course_nodes(id) ON DELETE SET NULL,
    scope_level     VARCHAR(20)  NOT NULL CHECK (scope_level IN ('course','subject','board')),
    max_per_group   INT          NOT NULL DEFAULT 30,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                                 CHECK (status IN ('DRAFT','SAVED')),
    created_by      BIGINT       NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_assignment_sessions_updated_at
    BEFORE UPDATE ON assignment_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE INDEX idx_as_course_node ON assignment_sessions(course_node_id);
CREATE INDEX idx_as_created_by  ON assignment_sessions(created_by);

CREATE TABLE assignment_groups (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL REFERENCES assignment_sessions(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    period      VARCHAR(100)
);

CREATE INDEX idx_ag_session_id ON assignment_groups(session_id);

CREATE TABLE assignment_group_students (
    group_id           BIGINT  NOT NULL REFERENCES assignment_groups(id) ON DELETE CASCADE,
    student_profile_id BIGINT  NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, student_profile_id)
);

CREATE TABLE assignment_teacher_mappings (
    id                 BIGSERIAL  PRIMARY KEY,
    session_id         BIGINT     NOT NULL REFERENCES assignment_sessions(id) ON DELETE CASCADE,
    teacher_profile_id BIGINT     NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    group_id           BIGINT     REFERENCES assignment_groups(id) ON DELETE SET NULL,
    UNIQUE (session_id, teacher_profile_id)
);

CREATE INDEX idx_atm_session_id ON assignment_teacher_mappings(session_id);
