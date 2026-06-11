-- Module 5: EVENTS/HOLIDAYS/OTHERS schedule tabs have no teacher or group.
-- V14 created these columns NOT NULL for the classes-only design.

ALTER TABLE class_schedules ALTER COLUMN teacher_profile_id DROP NOT NULL;
ALTER TABLE class_schedules ALTER COLUMN assignment_group_id DROP NOT NULL;
