-- Expand exams.status to include PENDING_APPROVAL for teacher-submitted exams
DO $$
DECLARE
    cname TEXT;
BEGIN
    SELECT conname INTO cname
    FROM pg_constraint
    WHERE conrelid = 'exams'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%status%';
    IF cname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE exams DROP CONSTRAINT %I', cname);
    END IF;
END$$;

ALTER TABLE exams ADD CONSTRAINT exams_status_check
    CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED'));
