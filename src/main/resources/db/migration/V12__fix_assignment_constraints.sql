-- Fix constraint to allow same teacher in multiple groups per session
-- Change from UNIQUE(session_id, teacher_profile_id)
-- To UNIQUE(session_id, teacher_profile_id, group_id)

ALTER TABLE assignment_teacher_mappings
DROP CONSTRAINT assignment_teacher_mappings_session_id_teacher_profile_id_key;

ALTER TABLE assignment_teacher_mappings
ADD CONSTRAINT assignment_teacher_mappings_session_teacher_group_key
UNIQUE (session_id, teacher_profile_id, group_id);
