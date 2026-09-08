-- PhysioConnect initial schema (MySQL 8)
-- Matches the JPA entities in com.physioconnect.entity

CREATE TABLE users (
    user_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(150)  NOT NULL,
    email              VARCHAR(150)  NOT NULL UNIQUE,
    phone              VARCHAR(20)   NOT NULL UNIQUE,
    password_hash      VARCHAR(255)  NOT NULL,
    role               ENUM('PATIENT','DOCTOR','ADMIN') NOT NULL,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE patients (
    patient_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NOT NULL UNIQUE,
    date_of_birth      DATE NULL,
    gender             ENUM('MALE','FEMALE','OTHER') NULL,
    address            VARCHAR(255) NULL,
    emergency_contact  VARCHAR(20) NULL,
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE doctors (
    doctor_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NOT NULL UNIQUE,
    specialization     VARCHAR(150) NOT NULL,
    qualification      VARCHAR(255) NOT NULL,
    experience_years   INT DEFAULT 0,
    consultation_fee   DECIMAL(10,2) NOT NULL,
    bio                TEXT NULL,
    average_rating     DECIMAL(2,1) DEFAULT 0.0,
    is_approved        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE locations (
    location_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    address_line        VARCHAR(255) NOT NULL,
    city                 VARCHAR(100) NOT NULL,
    state                VARCHAR(100) NOT NULL,
    pincode              VARCHAR(10) NOT NULL,
    opening_time         TIME NOT NULL,
    closing_time         TIME NOT NULL,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE services (
    service_id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                       VARCHAR(150) NOT NULL,
    description                TEXT NULL,
    default_duration_minutes   INT NOT NULL DEFAULT 30,
    base_price                 DECIMAL(10,2) NOT NULL,
    is_active                  BOOLEAN NOT NULL DEFAULT TRUE
);

-- M:N join table between doctors and locations
CREATE TABLE doctor_locations (
    doctor_id    BIGINT NOT NULL,
    location_id  BIGINT NOT NULL,
    PRIMARY KEY (doctor_id, location_id),
    CONSTRAINT fk_dl_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_dl_location FOREIGN KEY (location_id) REFERENCES locations(location_id)
);

CREATE TABLE doctor_availability (
    availability_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id               BIGINT NOT NULL,
    location_id              BIGINT NOT NULL,
    day_of_week              ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time                TIME NOT NULL,
    end_time                  TIME NOT NULL,
    slot_duration_minutes     INT DEFAULT 30,
    CONSTRAINT fk_avail_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_avail_location FOREIGN KEY (location_id) REFERENCES locations(location_id)
);

CREATE TABLE doctor_break_times (
    break_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id       BIGINT NOT NULL,
    day_of_week      ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time        TIME NOT NULL,
    end_time           TIME NOT NULL,
    CONSTRAINT fk_break_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id)
);

CREATE TABLE doctor_holidays (
    holiday_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id       BIGINT NOT NULL,
    holiday_date     DATE NOT NULL,
    reason            VARCHAR(255) NULL,
    CONSTRAINT fk_holiday_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id)
);

CREATE TABLE time_slots (
    slot_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id       BIGINT NOT NULL,
    location_id      BIGINT NOT NULL,
    slot_date         DATE NOT NULL,
    start_time         TIME NOT NULL,
    end_time            TIME NOT NULL,
    status               ENUM('AVAILABLE','HELD','BOOKED','BLOCKED') NOT NULL DEFAULT 'AVAILABLE',
    held_until            TIMESTAMP NULL,
    version                INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_slot_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_slot_location FOREIGN KEY (location_id) REFERENCES locations(location_id),
    CONSTRAINT uk_slot_doctor_date_start UNIQUE (doctor_id, slot_date, start_time)
);

CREATE TABLE appointments (
    appointment_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no        VARCHAR(30) NOT NULL UNIQUE,
    patient_id            BIGINT NOT NULL,
    doctor_id              BIGINT NOT NULL,
    location_id             BIGINT NOT NULL,
    service_id                BIGINT NOT NULL,
    slot_id                     BIGINT NOT NULL UNIQUE,
    status                        ENUM('PENDING_PAYMENT','CONFIRMED','COMPLETED','CANCELLED','RESCHEDULED','NO_SHOW') NOT NULL DEFAULT 'PENDING_PAYMENT',
    reason_for_visit                TEXT NULL,
    amount                            DECIMAL(10,2) NOT NULL,
    created_at                         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                          TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_appt_location FOREIGN KEY (location_id) REFERENCES locations(location_id),
    CONSTRAINT fk_appt_service FOREIGN KEY (service_id) REFERENCES services(service_id),
    CONSTRAINT fk_appt_slot FOREIGN KEY (slot_id) REFERENCES time_slots(slot_id)
);

CREATE TABLE payments (
    payment_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id          BIGINT NOT NULL UNIQUE,
    razorpay_order_id         VARCHAR(100) NOT NULL,
    razorpay_payment_id        VARCHAR(100) NULL,
    razorpay_signature           VARCHAR(255) NULL,
    amount                          DECIMAL(10,2) NOT NULL,
    currency                          VARCHAR(10) NOT NULL DEFAULT 'INR',
    status                              ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    invoice_url                          VARCHAR(255) NULL,
    created_at                             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
);

CREATE TABLE reviews (
    review_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id       BIGINT NOT NULL UNIQUE,
    patient_id             BIGINT NOT NULL,
    doctor_id                BIGINT NOT NULL,
    rating                     TINYINT NOT NULL,
    comment                      TEXT NULL,
    is_visible                    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    CONSTRAINT fk_review_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_review_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Indexes for common query paths
CREATE INDEX idx_timeslots_doctor_date_status ON time_slots (doctor_id, slot_date, status);
CREATE INDEX idx_appointments_patient_status ON appointments (patient_id, status);
CREATE INDEX idx_appointments_doctor_status ON appointments (doctor_id, status);
CREATE INDEX idx_payments_status ON payments (status);
