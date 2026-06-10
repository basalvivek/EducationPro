-- Module 5: Equipment assigned to schedules

CREATE TABLE schedule_equipment (
    class_schedule_id   BIGINT  NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
    equipment_id        BIGINT  NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    PRIMARY KEY (class_schedule_id, equipment_id)
);

CREATE INDEX idx_schedule_equipment ON schedule_equipment(class_schedule_id);
