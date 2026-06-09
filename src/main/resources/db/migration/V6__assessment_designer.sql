-- Module 3: Assessment Designer
-- Adds question metadata fields and exam tables

-- ── Question metadata for filtering ─────────────────────────────────────────
ALTER TABLE course_nodes ADD COLUMN IF NOT EXISTS class_name  VARCHAR(100);
ALTER TABLE course_nodes ADD COLUMN IF NOT EXISTS subject     VARCHAR(100);
ALTER TABLE course_nodes ADD COLUMN IF NOT EXISTS exam_board  VARCHAR(100);
ALTER TABLE course_nodes ADD COLUMN IF NOT EXISTS topic       VARCHAR(150);
ALTER TABLE course_nodes ADD COLUMN IF NOT EXISTS sub_topic   VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_cn_class     ON course_nodes(class_name)  WHERE class_name  IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_cn_subject   ON course_nodes(subject)     WHERE subject     IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_cn_examboard ON course_nodes(exam_board)  WHERE exam_board  IS NOT NULL;

-- ── Exams ────────────────────────────────────────────────────────────────────
CREATE TABLE exams (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    description        TEXT,
    time_limit_minutes INT          NOT NULL DEFAULT 60,
    total_marks        INT          NOT NULL DEFAULT 0,
    pass_mark          INT,
    shuffle_questions  BOOLEAN      NOT NULL DEFAULT FALSE,
    shuffle_options    BOOLEAN      NOT NULL DEFAULT FALSE,
    status             VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                           CHECK (status IN ('DRAFT','APPROVED')),
    created_by         BIGINT       NOT NULL REFERENCES users(id),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_exams_updated_at
    BEFORE UPDATE ON exams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ── Exam–question join ───────────────────────────────────────────────────────
CREATE TABLE exam_questions (
    id             BIGSERIAL PRIMARY KEY,
    exam_id        BIGINT NOT NULL REFERENCES exams(id)        ON DELETE CASCADE,
    question_id    BIGINT NOT NULL REFERENCES course_nodes(id) ON DELETE CASCADE,
    position       INT    NOT NULL DEFAULT 0,
    marks_override INT,
    CONSTRAINT uq_exam_question UNIQUE (exam_id, question_id)
);

CREATE INDEX idx_eq_exam     ON exam_questions(exam_id);
CREATE INDEX idx_eq_question ON exam_questions(question_id);
