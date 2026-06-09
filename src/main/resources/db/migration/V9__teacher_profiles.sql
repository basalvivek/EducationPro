-- Module 4: Teacher Profiles
-- Extended teacher data linked to users table

CREATE TABLE teacher_profiles (
    id                      BIGSERIAL    PRIMARY KEY,
    user_id                 BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- Basic Information
    teacher_id              VARCHAR(50),
    first_name              VARCHAR(100) NOT NULL,
    middle_name             VARCHAR(100),
    last_name               VARCHAR(100) NOT NULL,
    gender                  VARCHAR(20),
    date_of_birth           DATE,
    nationality             VARCHAR(100),
    profile_photo_path      VARCHAR(500),

    -- Contact Information
    mobile_number           VARCHAR(30),
    alternate_phone         VARCHAR(30),
    address_line1           VARCHAR(255),
    address_line2           VARCHAR(255),
    city                    VARCHAR(100),
    state_county            VARCHAR(100),
    postal_code             VARCHAR(20),
    country                 VARCHAR(100),

    -- Employment Details
    employee_number         VARCHAR(50),
    joining_date            DATE,
    employment_type         VARCHAR(20)  CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT')),
    designation             VARCHAR(150),
    department              VARCHAR(150),
    subject_specialization  VARCHAR(255),
    reporting_manager       VARCHAR(150),
    employment_status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                CHECK (employment_status IN ('ACTIVE','INACTIVE','ON_LEAVE')),

    -- Academic Qualifications
    highest_qualification   VARCHAR(100),
    degree_name             VARCHAR(200),
    university              VARCHAR(255),
    graduation_year         INT,
    additional_certs        TEXT,

    -- Emergency Contact
    emergency_contact_name  VARCHAR(150),
    emergency_relationship  VARCHAR(100),
    emergency_phone         VARCHAR(30),

    -- Payroll Information
    bank_name               VARCHAR(150),
    account_number          VARCHAR(100),
    sort_code               VARCHAR(50),
    tax_id                  VARCHAR(100),
    salary_grade            VARCHAR(50),
    payment_frequency       VARCHAR(20)  CHECK (payment_frequency IN ('MONTHLY','FORTNIGHTLY','WEEKLY')),

    -- Document paths
    doc_identity_proof      VARCHAR(500),
    doc_address_proof       VARCHAR(500),
    doc_qualification       VARCHAR(500),
    doc_teaching_license    VARCHAR(500),
    doc_employment_contract VARCHAR(500),
    doc_background_check    VARCHAR(500),

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_teacher_profiles_updated_at
    BEFORE UPDATE ON teacher_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE INDEX idx_tp_user_id ON teacher_profiles(user_id);
