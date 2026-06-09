CREATE TABLE course_nodes (
    id            BIGSERIAL    PRIMARY KEY,
    parent_id     BIGINT       REFERENCES course_nodes(id) ON DELETE CASCADE,
    type          VARCHAR(20)  NOT NULL CHECK (type IN ('NODE','QUESTION')),
    title         VARCHAR(150) NOT NULL,
    description   TEXT,
    question_text TEXT,
    question_type VARCHAR(20)  CHECK (question_type IN ('MCQ','TRUE_FALSE','SHORT','ESSAY')),
    marks         SMALLINT     DEFAULT 1,
    sort_order    INT          NOT NULL DEFAULT 0,
    created_by    BIGINT       NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_question_has_no_children
        CHECK (type != 'QUESTION' OR parent_id IS NOT NULL)
);

CREATE INDEX idx_cn_parent ON course_nodes(parent_id);
CREATE INDEX idx_cn_type   ON course_nodes(type);

CREATE TRIGGER trg_cn_updated_at
    BEFORE UPDATE ON course_nodes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
