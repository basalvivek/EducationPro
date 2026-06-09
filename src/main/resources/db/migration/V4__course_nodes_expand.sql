-- V4: Expand course_nodes table for v1.1 spec
-- ------------------------------------------------

-- 1. Drop and recreate question_type CHECK with new values
ALTER TABLE course_nodes DROP CONSTRAINT IF EXISTS course_nodes_question_type_check;
ALTER TABLE course_nodes ADD CONSTRAINT course_nodes_question_type_check
    CHECK (question_type IN ('MCQ_SINGLE','MCQ_MULTIPLE','TRUE_FALSE','SHORT_ANSWER','ESSAY','CODE','IMAGE_BASED'));

-- 2. Drop and recreate marks CHECK with explicit range
ALTER TABLE course_nodes DROP CONSTRAINT IF EXISTS course_nodes_marks_check;
ALTER TABLE course_nodes ADD CONSTRAINT course_nodes_marks_check
    CHECK (marks >= 1 AND marks <= 100);

-- 3. Add tagline
ALTER TABLE course_nodes ADD COLUMN tagline VARCHAR(100);

-- 4. Add complexity
ALTER TABLE course_nodes ADD COLUMN complexity VARCHAR(20)
    CHECK (complexity IN ('FOUNDATION','INTERMEDIATE','HIGHER'));

-- 5. Add explanation
ALTER TABLE course_nodes ADD COLUMN explanation TEXT;

-- 6. Add options (JSONB for MCQ choice arrays)
ALTER TABLE course_nodes ADD COLUMN options JSONB;

-- 7. Add correct_index (single-answer MCQ)
ALTER TABLE course_nodes ADD COLUMN correct_index SMALLINT;

-- 8. Add correct_indices (multi-answer MCQ, stored as JSONB array)
ALTER TABLE course_nodes ADD COLUMN correct_indices JSONB;

-- 9. Add partial_marking
ALTER TABLE course_nodes ADD COLUMN partial_marking VARCHAR(20)
    CHECK (partial_marking IN ('FULL_ONLY','PER_OPTION'));

-- 10. Add correct_answer (TRUE_FALSE)
ALTER TABLE course_nodes ADD COLUMN correct_answer VARCHAR(5)
    CHECK (correct_answer IN ('TRUE','FALSE'));

-- 11. Add model_answer (SHORT_ANSWER / ESSAY)
ALTER TABLE course_nodes ADD COLUMN model_answer TEXT;

-- 12. Add marking_scheme (ESSAY)
ALTER TABLE course_nodes ADD COLUMN marking_scheme TEXT;

-- 13. Add word_limit (SHORT_ANSWER / ESSAY)
ALTER TABLE course_nodes ADD COLUMN word_limit SMALLINT DEFAULT 0;

-- 14. Add code_language (CODE)
ALTER TABLE course_nodes ADD COLUMN code_language VARCHAR(20)
    CHECK (code_language IN ('PYTHON','JAVA','JAVASCRIPT','SQL','HTML','CSS','OTHER'));

-- 15. Add starter_code (CODE)
ALTER TABLE course_nodes ADD COLUMN starter_code TEXT;

-- 16. Add expected_output (CODE)
ALTER TABLE course_nodes ADD COLUMN expected_output TEXT;

-- 17. Add image_path (IMAGE_BASED)
ALTER TABLE course_nodes ADD COLUMN image_path VARCHAR(500);

-- 18. Add image_alt (IMAGE_BASED)
ALTER TABLE course_nodes ADD COLUMN image_alt VARCHAR(255);

-- 19. Add image_answer_type (IMAGE_BASED)
ALTER TABLE course_nodes ADD COLUMN image_answer_type VARCHAR(10)
    CHECK (image_answer_type IN ('WRITTEN','MCQ'));

-- 20. New indexes
CREATE INDEX idx_cn_q_type    ON course_nodes(question_type);
CREATE INDEX idx_cn_complexity ON course_nodes(complexity);
