-- Module 5: Classroom & Equipment Management

CREATE TABLE classrooms (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    room_code       VARCHAR(50),
    floor           INT,
    capacity        INT,
    facilities      TEXT,
    is_active       BOOLEAN      DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE equipment (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    equipment_code  VARCHAR(50),
    category        VARCHAR(50),
    is_active       BOOLEAN      DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE classroom_equipment (
    classroom_id    BIGINT  NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    equipment_id    BIGINT  NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    PRIMARY KEY (classroom_id, equipment_id)
);

CREATE INDEX idx_classrooms_active ON classrooms(is_active);
CREATE INDEX idx_equipment_active ON equipment(is_active);
