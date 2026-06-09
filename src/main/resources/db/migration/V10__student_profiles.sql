-- Module 5: Student Profiles
-- Student data linked to users (STUDENT role); parent linked to users (PARENT role)

CREATE TABLE student_profiles (
    id                      BIGSERIAL    PRIMARY KEY,
    user_id                 BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    parent_user_id          BIGINT       REFERENCES users(id) ON DELETE SET NULL,

    -- 1. Student Information
    student_id              VARCHAR(50),
    first_name              VARCHAR(100) NOT NULL,
    middle_name             VARCHAR(100),
    last_name               VARCHAR(100) NOT NULL,
    preferred_name          VARCHAR(100),
    gender                  VARCHAR(20),
    date_of_birth           DATE,
    nationality             VARCHAR(100),
    profile_photo_path      VARCHAR(500),

    -- 2. Contact Information
    student_email           VARCHAR(255),
    mobile_number           VARCHAR(30),
    address_line1           VARCHAR(255),
    address_line2           VARCHAR(255),
    city                    VARCHAR(100),
    state_county            VARCHAR(100),
    postal_code             VARCHAR(20),
    country                 VARCHAR(100),

    -- 3. Parent / Guardian Information
    guardian_name           VARCHAR(150),
    relationship            VARCHAR(50),
    parent_email            VARCHAR(255),
    parent_mobile           VARCHAR(30),
    alternate_contact       VARCHAR(30),
    occupation              VARCHAR(150),
    emergency_contact_number VARCHAR(30),

    -- 4. Admission Information
    admission_number        VARCHAR(50),
    admission_date          DATE,
    academic_year           VARCHAR(20),
    campus                  VARCHAR(150),
    program_course          VARCHAR(200),
    grade_year              VARCHAR(50),
    class_name              VARCHAR(50),
    section                 VARCHAR(20),
    student_status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                CHECK (student_status IN ('ACTIVE','GRADUATED','SUSPENDED','WITHDRAWN')),

    -- 5. Academic Information
    previous_school         VARCHAR(255),
    previous_qualification  VARCHAR(200),
    subjects_selected       TEXT,
    medium_of_instruction   VARCHAR(100),
    enrollment_type         VARCHAR(20) CHECK (enrollment_type IN ('FULL_TIME','PART_TIME')),

    -- 7. Medical Information
    blood_group             VARCHAR(10),
    allergies               TEXT,
    medical_conditions      TEXT,
    emergency_medical_notes TEXT,
    doctor_contact          VARCHAR(255),

    -- 8. Fee & Financial Information
    fee_category            VARCHAR(100),
    scholarship_status      VARCHAR(100),
    discount_waiver         VARCHAR(100),
    sponsor_info            TEXT,

    -- 9. Documents
    doc_birth_certificate   VARCHAR(500),
    doc_passport_id         VARCHAR(500),
    doc_academic_records    VARCHAR(500),
    doc_address_proof       VARCHAR(500),
    doc_photograph          VARCHAR(500),
    doc_guardian_id         VARCHAR(500),

    -- 10. Audit
    created_by_id           BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    last_updated_by_id      BIGINT       REFERENCES users(id) ON DELETE SET NULL,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_student_profiles_updated_at
    BEFORE UPDATE ON student_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE INDEX idx_sp_user_id        ON student_profiles(user_id);
CREATE INDEX idx_sp_parent_user_id ON student_profiles(parent_user_id);
CREATE INDEX idx_sp_status         ON student_profiles(student_status);
