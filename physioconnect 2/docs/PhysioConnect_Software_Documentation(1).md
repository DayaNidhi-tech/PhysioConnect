# PhysioConnect
### Physiotherapy Appointment Booking & Clinic Management System
**Software Requirements & Design Documentation**

---

|  |  |
|---|---|
| **Document Title** | PhysioConnect – Software Requirements & Design Specification |
| **Document Type** | Software Requirements Specification (SRS) / Design Document |
| **Version** | 1.0 |
| **Status** | Draft for Review |
| **Prepared By** | Software Architecture & Engineering Team |
| **Classification** | Confidential – Client Delivery / Internal Engineering |
| **Standard Reference** | IEEE 830-1998 (SRS), IEEE 1016-2009 (SDD) |

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Purpose](#2-purpose)
3. [Scope](#3-scope)
4. [Functional Requirements](#4-functional-requirements)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [System Architecture](#6-system-architecture)
7. [Module Description](#7-module-description)
8. [Database Design](#8-database-design)
9. [API Design](#9-api-design)
10. [Security](#10-security)
11. [Future Enhancements](#11-future-enhancements)
12. [Conclusion](#12-conclusion)

---

## 1. Introduction

PhysioConnect is a production-grade, full-stack web application designed to digitize and streamline the end-to-end operations of a physiotherapy multi-location service platform. The system connects three primary stakeholders — **Patients**, **Doctors (Physiotherapists)**, and **Administrators** — through a unified digital platform that manages appointment booking, doctor availability, multi-location service operations, secure online payments, and post-visit engagement (reviews and ratings).

The system is architected as a **decoupled client-server application**, with a React.js single-page application (SPA) consuming a RESTful API exposed by a Spring Boot backend. This separation of concerns enables independent scaling, testing, and deployment of the frontend and backend layers, which is standard practice for enterprise-grade SaaS platforms.

This document serves as the authoritative Software Requirements and Design Specification (SRS/SDD) for PhysioConnect. It is intended to be read by:

- **Engineering teams** (frontend, backend, QA, DevOps) for implementation reference
- **Product/Business stakeholders** for feature validation
- **Client-facing teams** for delivery and handover documentation
- **New engineers** onboarding onto the project

---

## 2. Purpose

The purpose of this document is to:

- Define the complete functional and non-functional requirements of the PhysioConnect platform.
- Describe the system architecture, technology stack, and design rationale.
- Specify the database schema, entity relationships, and data constraints.
- Define the REST API contract between frontend and backend.
- Document the security model, including authentication, authorization, and data protection mechanisms.
- Serve as a single source of truth for current and future development, minimizing ambiguity during implementation, code review, and QA sign-off.
- Provide a reference artifact suitable for client delivery, technical due diligence, and GitHub repository documentation.

---

## 3. Scope

### 3.1 In Scope

PhysioConnect covers the complete digital patient journey and clinic administration workflow, including:

- Patient-facing appointment discovery, booking, rescheduling, and cancellation
- Doctor-facing availability, schedule, and patient management
- Admin-facing operational oversight across doctors, patients, services, locations, and payments
- Secure online payment processing via Razorpay
- Automated transactional email notifications
- Multi-location service support
- Reviews and ratings for completed appointments
- Role-based dashboards (Patient, Doctor, Admin)
- Analytics and reporting for administrative decision-making

### 3.2 Out of Scope (Phase 1)

The following are explicitly excluded from the initial production release and are candidates for future phases (see [Section 11](#11-future-enhancements)):

- Native mobile applications (iOS/Android)
- Telemedicine / video consultation
- Insurance claim processing and integration
- Multi-language (i18n) support
- AI-based physiotherapy exercise recommendation engine
- Wearable device / IoT integration for rehabilitation tracking

### 3.3 Intended Users

| Role | Description |
|---|---|
| **Patient** | End user booking appointments, managing their healthcare interactions, and making payments |
| **Doctor** | Physiotherapist managing availability, appointments, and patient interactions |
| **Admin** | Clinic/platform administrator managing global operations, staff, and reporting |

---

## 4. Functional Requirements

Functional requirements are grouped by module and tagged with a unique ID for traceability (`FR-<Module>-<Number>`).

### 4.1 Authentication & User Management

| ID | Requirement |
|---|---|
| FR-AUTH-01 | The system shall allow patients to register using email, phone number, and password. |
| FR-AUTH-02 | The system shall allow login via email and password, issuing a JWT access token and refresh token on success. |
| FR-AUTH-03 | The system shall support role-based access control (RBAC) for `PATIENT`, `DOCTOR`, and `ADMIN` roles. |
| FR-AUTH-04 | The system shall allow password reset via a time-bound, single-use email link (OTP or token-based). |
| FR-AUTH-05 | The system shall allow Admins to create Doctor accounts (Doctors shall not self-register). |
| FR-AUTH-06 | The system shall enforce email verification before a patient account is fully activated. |
| FR-AUTH-07 | The system shall invalidate JWT tokens on logout and support refresh-token rotation. |

### V1 Location Model

For V1, a **location** represents a geographic service location/area where physiotherapy can be provided. It is not a separate clinic-branch ownership model. A doctor may be configured for multiple service locations, and an appointment records the selected service location. The system does not assume that a doctor belongs to only one clinic branch.

### 4.2 Patient Booking Flow

| ID | Requirement |
|---|---|
| FR-BOOK-01 | The system shall allow patients to browse available services (e.g., Sports Physiotherapy, Orthopedic Rehab, Neuro Physiotherapy). |
| FR-BOOK-02 | The system shall allow patients to select a service location from a list of active locations. |
| FR-BOOK-03 | The system shall display doctors filtered by selected service and location, with ratings and experience shown. |
| FR-BOOK-04 | The system shall allow patients to select a date and view only real-time available time slots for the chosen doctor. |
| FR-BOOK-05 | The system shall lock a selected time slot for a configurable hold window (e.g., 5 minutes) to prevent double-booking during payment. |
| FR-BOOK-06 | The system shall capture patient details (name, age, gender, contact, reason for visit, medical history notes) at booking time. |

| FR-BOOK-06A | Medical history notes captured during booking shall be stored with the appointment/visit context and treated as sensitive patient information. |
| FR-BOOK-06B | Access to medical history notes shall be restricted to the patient who supplied them, the doctor assigned to the appointment, and authorized Admin users. |
| FR-BOOK-06C | V1 medical-history support is limited to basic notes relevant to the appointment and does not constitute a full EMR/EHR. |
|  | **Medical-history API exposure (documentation decision DD-05):** Medical-history notes are stored with appointments and are sensitive; they are exposed only via the authorized appointment-detail response `GET /api/v1/appointments/{appointmentId}`. Only the note-supplying `PATIENT` (for their own appointment), the assigned `DOCTOR` (for appointments assigned to that doctor), and authorized `ADMIN` users may receive the medical-history content in that endpoint's response. Medical-history must not be exposed through public doctor listing, booking/search endpoints, or any separate public medical-history endpoints. |
| FR-BOOK-07 | The system shall display an appointment summary (doctor, date, time, location, service, fee) prior to payment. |
| FR-BOOK-08 | The system shall process payment via Razorpay before confirming the appointment. |
| FR-BOOK-09 | The system shall generate a unique appointment reference number upon successful booking. |
| FR-BOOK-10 | The system shall send an email confirmation with appointment details and an invoice/receipt upon successful booking. |
| FR-BOOK-11 | The system shall release a held slot automatically if payment is not completed within the hold window. |

### 4.3 Appointment Management

| ID | Requirement |
|---|---|
| FR-APPT-01 | The system shall allow patients to view upcoming and completed appointments. |
| FR-APPT-02 | The system shall allow patients to reschedule an appointment up to a configurable cutoff (e.g., 6 hours before the appointment), subject to doctor slot availability. |
| FR-APPT-03 | The system shall allow patients to cancel an appointment, applying a refund policy based on cancellation timing. |
| FR-APPT-04 | The system shall allow doctors to view, accept, or mark appointments as completed/no-show. |
| FR-APPT-05 | The system shall allow admins to view, filter, and override any appointment in the system. |
| FR-APPT-06 | The system shall send email notifications on booking, rescheduling, cancellation, and reminder (e.g., 24 hours prior). |
| FR-APPT-07 | The system shall maintain a full audit trail (status history) for every appointment. |

### 4.4 Doctor Availability & Calendar Management

| ID | Requirement |
|---|---|
| FR-DOC-01 | The system shall allow doctors to define weekly recurring working hours per service location they support. |
| FR-DOC-02 | The system shall allow doctors to define break times within working hours (e.g., lunch break). |
| FR-DOC-03 | The system shall allow doctors to mark holidays/leave, blocking slot generation for those dates. |
| FR-DOC-04 | The system shall auto-generate bookable time slots based on working hours, break times, holidays, and configurable slot duration (e.g., 30 minutes). |
| FR-DOC-05 | The system shall provide doctors a calendar view (day/week/month) of all appointments. |
| FR-DOC-06 | The system shall allow doctors to view patient details and appointment history for their own patients only. |

### 4.5 Payments

| ID | Requirement |
|---|---|
| FR-PAY-01 | The system shall integrate with Razorpay for order creation, payment capture, and signature verification. |
| FR-PAY-02 | The system shall persist payment status (`PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`) linked to each appointment. |
| FR-PAY-03 | The system shall generate a downloadable invoice (PDF) for each successful payment. |
| FR-PAY-04 | The system shall support refund initiation via Razorpay Refunds API for eligible cancellations. |
| FR-PAY-05 | The system shall reconcile payment webhooks from Razorpay to guard against client-side tampering. |
| FR-PAY-06 | The system shall allow admins to view all platform transactions with filters (date range, doctor, location, status). |

### 4.6 Reviews & Ratings

| ID | Requirement |
|---|---|
| FR-REV-01 | The system shall allow patients to submit a rating (1–5) and text review only after a completed appointment. |
| FR-REV-02 | The system shall display aggregated doctor ratings on doctor profile/listing pages. |
| FR-REV-03 | The system shall allow admins to moderate (hide/remove) inappropriate reviews. |

### 4.7 Admin Management

| ID | Requirement |
|---|---|
| FR-ADM-01 | The system shall allow admins to perform CRUD operations on Doctors, Services, and Locations. |
| FR-ADM-02 | The system shall allow admins to activate/deactivate Patient or Doctor accounts. |
| FR-ADM-03 | The system shall provide dashboards for appointment volume, revenue, doctor performance, and location performance. |
| FR-ADM-04 | The system shall allow admins to export reports (CSV/PDF) for a selected date range. |

**Canonical Admin API (documentation decision DD-02 / DD-03)**

The project standardizes the Admin Doctor management and activation APIs under the canonical base path `/api/v1/admin`. The canonical Admin endpoints for Doctor management are:

- GET    /api/v1/admin/doctors
- POST   /api/v1/admin/doctors
- GET    /api/v1/admin/doctors/{doctorId}
- PUT    /api/v1/admin/doctors/{doctorId}

Admin activation/deactivation of user accounts uses the unified user activation endpoints:

- PATCH  /api/v1/admin/users/{userId}/activate
- PATCH  /api/v1/admin/users/{userId}/deactivate

Notes and constraints (documentation decisions):
- Only authenticated `ADMIN` actors may call these Admin APIs.
- Doctor accounts created via the Admin Doctor creation path are `INACTIVE` by default and require an explicit Admin activation before the Doctor may log in (see DD-04).
- Request/response DTOs for these endpoints must be drawn only from the existing domain model fields; do not introduce undocumented Doctor fields in requests or responses. Validation and status behaviors should follow the project's standard response envelope and error mapping (see DD-06).
- Pagination/filtering for collection endpoints is not defined here; if required, it will be specified in a future API contract extension.

### 4.8 Notifications

| ID | Requirement |
|---|---|
| FR-NOTIF-01 | The system shall send transactional emails for: registration confirmation, booking confirmation, reschedule, cancellation, payment receipt, and appointment reminders. |
| FR-NOTIF-02 | Email delivery shall be handled asynchronously via a message queue to avoid blocking the request thread. |

---

## 5. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Performance** | 95th percentile API response time shall be under 400ms for read operations and under 800ms for write operations under normal load (up to 500 concurrent users). |
| **Scalability** | The backend shall be stateless (JWT-based) to support horizontal scaling behind a load balancer. |
| **Availability** | The system shall target 99.5% uptime for production environments, excluding scheduled maintenance windows. |
| **Reliability** | Slot booking shall be atomic and concurrency-safe (pessimistic locking or optimistic versioning) to prevent double-booking under concurrent requests. |
| **Security** | All traffic shall be served over HTTPS/TLS 1.2+. Passwords shall be hashed using BCrypt. See [Section 10](#10-security) for full details. |
| **Usability** | The UI shall be responsive (mobile, tablet, desktop) and conform to WCAG 2.1 AA accessibility guidelines where practical. |
| **Maintainability** | Backend code shall follow a layered architecture (Controller → Service → Repository) with clear separation of concerns and >70% unit test coverage on service-layer business logic. |
| **Portability** | The application shall be containerized (Docker) to ensure consistent behavior across development, staging, and production environments. |
| **Auditability** | All appointment status changes, payment transactions, and admin actions shall be logged with timestamp and actor identity. |
| **Compliance** | The system shall follow data minimization principles for storing patient medical information and shall support data deletion requests in line with applicable data protection regulations (e.g., DPDP Act / GDPR-aligned practices). |
| **Data Backup** | The production MySQL database shall be backed up daily with a minimum 30-day retention policy. |
| **Localization Ready** | Though single-language at launch, all user-facing strings shall be externalized (not hardcoded) to support future i18n. |

---

## 6. System Architecture

### 6.1 Architectural Style

PhysioConnect follows a **three-tier client-server architecture** with a clear separation between the presentation layer, application/business layer, and data layer. The backend follows a **layered monolithic architecture** (Controller–Service–Repository) — the recommended industry-standard starting point for a system of this scope, offering simpler deployment and operational overhead than microservices while still allowing future extraction of bounded contexts (e.g., Payments, Notifications) into independent services if scale demands it.

### 6.2 High-Level Architecture Diagram

```
                                   ┌────────────────────────────┐
                                   │        Client Layer         │
                                   │  React.js (Vite) + Tailwind │
                                   │  React Router | Axios       │
                                   │  Framer Motion               │
                                   └──────────────┬───────────────┘
                                                  │ HTTPS (REST/JSON)
                                                  ▼
                                   ┌────────────────────────────┐
                                   │        API Gateway /        │
                                   │        Load Balancer        │
                                   │      (Nginx / Cloud LB)     │
                                   └──────────────┬───────────────┘
                                                  ▼
                     ┌───────────────────────────────────────────────────┐
                     │                 Application Layer                  │
                     │                 Spring Boot Backend                │
                     │  ┌───────────────┐  ┌───────────────┐              │
                     │  │  Controllers   │→ │   Services     │             │
                     │  │  (REST API)    │  │ (Business Logic)│            │
                     │  └───────────────┘  └───────┬───────┘              │
                     │                              ▼                      │
                     │                    ┌───────────────────┐            │
                     │                    │  Repositories      │           │
                     │                    │  (Spring Data JPA) │           │
                     │                    └─────────┬─────────┘            │
                     │  Cross-cutting: Spring Security (JWT), Validation,  │
                     │  Exception Handling, Logging, Scheduler (Slot Gen)  │
                     └───────────────┬─────────────────────┬───────────────┘
                                     ▼                     ▼
                     ┌───────────────────────┐   ┌─────────────────────────┐
                     │      MySQL Database    │   │   External Integrations │
                     │  (Users, Appointments,  │   │  - Razorpay (Payments)  │
                     │   Payments, etc.)       │   │  - SMTP/Email Provider  │
                     └───────────────────────┘   │  - Cloud Storage (Docs) │
                                                   └─────────────────────────┘
```

### 6.3 Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| Frontend Framework | React.js (Vite) | Fast HMR dev experience, optimized production builds, industry-standard SPA framework |
| Styling | Tailwind CSS | Utility-first CSS for rapid, consistent UI development |
| Routing | React Router | Client-side routing for SPA navigation |
| HTTP Client | Axios | Interceptor support for JWT attachment and centralized error handling |
| Animation | Framer Motion | Declarative, performant UI transitions |
| Backend Framework | Spring Boot | Production-grade, convention-over-configuration Java framework with a mature ecosystem |
| Security | Spring Security + JWT | Stateless, scalable authentication and method-level authorization |
| ORM | Spring Data JPA + Hibernate | Reduces boilerplate; provides transaction management and entity relationship mapping |
| Database | MySQL | ACID-compliant relational database well suited to appointment/transactional data with strong consistency needs |
| Payments | Razorpay | Leading India-focused payment gateway with strong webhook and refund support |
| Email | JavaMailSender / SMTP (e.g., SendGrid, Amazon SES) | Reliable transactional email delivery |
| Deployment | Docker, Nginx | Environment consistency and reverse-proxy/SSL termination |

### 6.4 Deployment Architecture (Recommended)

```
Internet
   │
   ▼
[ Nginx Reverse Proxy + SSL (Let's Encrypt) ]
   │
   ├── /api/*  ──►  [ Spring Boot App (Docker container) ]  ──►  [ MySQL (managed RDS/Cloud SQL) ]
   │
   └── /*      ──►  [ React Static Build (served via Nginx/CDN) ]

Background:
   [ Spring Scheduler ] ──► Slot generation, appointment reminders, hold-timeout release
   [ Message Queue (optional: RabbitMQ) ] ──► Async email dispatch
```

For production, it is recommended to host the frontend build as static assets via a CDN (e.g., Cloudflare, AWS CloudFront) and the backend as a containerized service on a managed platform (e.g., AWS ECS, Azure App Service, or a VPS with Docker Compose), with MySQL hosted as a managed database service for automated backups and failover.

---

## 7. Module Description

### 7.1 Patient Module

**Patient Dashboard:**
- Upcoming Appointments
- Completed Appointments
- Cancel Appointment
- Reschedule Appointment
- Payment History
- Download Invoice
- Profile Management

**Responsibilities:** Manages the patient-facing booking journey and self-service appointment lifecycle management, described in the flow below.

**Patient Booking Flow:**

```
 Visit Website
      │
      ▼
 Choose Service
      │
      ▼
 Choose Clinic Location
      │
      ▼
 Choose Doctor
      │
      ▼
 Select Date
      │
      ▼
 View Available Time Slots
      │
      ▼
 Enter Patient Details
      │
      ▼
 Appointment Summary
      │
      ▼
 Online Payment (Razorpay)
      │
      ▼
 Appointment Confirmation
      │
      ▼
 Email Notification
```

### 7.2 Doctor Module

**Doctor Dashboard:**
- Dashboard Overview (appointment counts, today's schedule, earnings snapshot)
- Calendar (day/week/month view of appointments)
- Manage Availability (weekly recurring slots per location)
- Manage Timings (slot duration, buffer time between appointments)
- Holidays (leave/blocked-date management)
- Break Time (daily recurring or one-off breaks)
- View Patients (patient list with appointment history)
- Payments (earnings, payout history)
- Reviews (view ratings/feedback received)
- Settings (profile, specialization, consultation fee)

### 7.3 Admin Module

**Admin Dashboard:**
- Doctor Management (onboarding, profile approval, deactivation)
- Patient Management (view, deactivate, support access)
- Appointment Management (global view, override, dispute resolution)
- Service Management (define services, pricing, duration)
- Location Management (service locations, address, operating hours)
- Payment Management (transactions, refunds, reconciliation)
- Reports (exportable operational reports)
- Analytics (revenue trends, doctor utilization, location performance, cancellation rates)

### 7.4 Shared/Core Modules

| Module | Description |
|---|---|
| **Auth Module** | Registration, login, JWT issuance/refresh, password reset, role-based route guarding |
| **Notification Module** | Templated transactional emails triggered by domain events (booking, cancellation, reminders) |
| **Payment Module** | Razorpay order creation, payment verification, refund processing, invoice generation |
| **Slot Engine** | Computes real-time available slots from doctor availability, breaks, holidays, and existing bookings |
| **Review Module** | Post-appointment rating/review capture and aggregation |
| **Audit Module** | Tracks status transitions and admin actions for compliance and support |

---

## 8. Database Design

### 8.1 Entity-Relationship Overview

```
   Users (1) ────────────< Patients (0..1)
   Users (1) ────────────< Doctors  (0..1)

   Doctors (1) ───────────< DoctorAvailability
   Doctors (1) ───────────< DoctorHolidays
   Doctors (1) ───────────< DoctorBreakTimes
   Doctors (M) ───────────< Locations (via DoctorLocation, M:N)

   Locations (1) ─────────< TimeSlots
   Doctors (1)  ─────────< TimeSlots
   Services (1) ──────────< Appointments

   Patients (1) ───────────< Appointments
   Doctors (1)  ───────────< Appointments
   Locations (1) ──────────< Appointments
   TimeSlots (1) ─────────── Appointments (1:1 at booking time)

   Appointments (1) ──────── Payments (1:1)
   Appointments (1) ──────── Reviews (0..1)
```

### 8.2 Core Tables

**Users**

| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | PK, AUTO_INCREMENT |
| full_name | VARCHAR(150) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| phone | VARCHAR(20) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| role | ENUM('PATIENT','DOCTOR','ADMIN') | NOT NULL |
| is_active | BOOLEAN | DEFAULT TRUE |
| is_email_verified | BOOLEAN | DEFAULT FALSE |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP |

**Patients**

| Column | Type | Constraints |
|---|---|---|
| patient_id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → Users.user_id, UNIQUE, NOT NULL |
| date_of_birth | DATE | NULLABLE |
| gender | ENUM('MALE','FEMALE','OTHER') | NULLABLE |
| address | VARCHAR(255) | NULLABLE |
| emergency_contact | VARCHAR(20) | NULLABLE |

**Doctors**

| Column | Type | Constraints |
|---|---|---|
| doctor_id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → Users.user_id, UNIQUE, NOT NULL |
| specialization | VARCHAR(150) | NOT NULL |
| qualification | VARCHAR(255) | NOT NULL |
| experience_years | INT | DEFAULT 0 |
| consultation_fee | DECIMAL(10,2) | NOT NULL |
| bio | TEXT | NULLABLE |
| average_rating | DECIMAL(2,1) | DEFAULT 0.0 |
| is_approved | BOOLEAN | DEFAULT FALSE |

**Locations**

| Column | Type | Constraints |
|---|---|---|
| location_id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(150) | NOT NULL |
| address_line | VARCHAR(255) | NOT NULL |
| city | VARCHAR(100) | NOT NULL |
| state | VARCHAR(100) | NOT NULL |
| pincode | VARCHAR(10) | NOT NULL |
| opening_time | TIME | NOT NULL |
| closing_time | TIME | NOT NULL |
| is_active | BOOLEAN | DEFAULT TRUE |

**Services**

| Column | Type | Constraints |
|---|---|---|
| service_id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(150) | NOT NULL |
| description | TEXT | NULLABLE |
| default_duration_minutes | INT | NOT NULL, DEFAULT 30 |
| base_price | DECIMAL(10,2) | NOT NULL |
| is_active | BOOLEAN | DEFAULT TRUE |

**DoctorAvailability**

| Column | Type | Constraints |
|---|---|---|
| availability_id | BIGINT | PK, AUTO_INCREMENT |
| doctor_id | BIGINT | FK → Doctors.doctor_id |
| location_id | BIGINT | FK → Locations.location_id |
| day_of_week | ENUM('MON'...'SUN') | NOT NULL |
| start_time | TIME | NOT NULL |
| end_time | TIME | NOT NULL |
| slot_duration_minutes | INT | DEFAULT 30 |

**DoctorBreakTimes**

| Column | Type | Constraints |
|---|---|---|
| break_id | BIGINT | PK, AUTO_INCREMENT |
| doctor_id | BIGINT | FK → Doctors.doctor_id |
| day_of_week | ENUM('MON'...'SUN') | NOT NULL |
| start_time | TIME | NOT NULL |
| end_time | TIME | NOT NULL |

**DoctorHolidays**

| Column | Type | Constraints |
|---|---|---|
| holiday_id | BIGINT | PK, AUTO_INCREMENT |
| doctor_id | BIGINT | FK → Doctors.doctor_id |
| holiday_date | DATE | NOT NULL |
| reason | VARCHAR(255) | NULLABLE |

**TimeSlots**

| Column | Type | Constraints |
|---|---|---|
| slot_id | BIGINT | PK, AUTO_INCREMENT |
| doctor_id | BIGINT | FK → Doctors.doctor_id |
| location_id | BIGINT | FK → Locations.location_id |
| slot_date | DATE | NOT NULL |
| start_time | TIME | NOT NULL |
| end_time | TIME | NOT NULL |
| status | ENUM('AVAILABLE','HELD','BOOKED','BLOCKED') | DEFAULT 'AVAILABLE' |
| held_until | TIMESTAMP | NULLABLE |
| version | INT | DEFAULT 0 (optimistic locking) |

> **Design Note:** A UNIQUE constraint on `(doctor_id, slot_date, start_time)` prevents duplicate slot generation, and the `version` column (Hibernate `@Version`) enables optimistic locking to guarantee atomic slot booking under concurrent requests, satisfying FR-BOOK-05 and the concurrency non-functional requirement.

**Appointments**

| Column | Type | Constraints |
|---|---|---|
| appointment_id | BIGINT | PK, AUTO_INCREMENT |
| reference_no | VARCHAR(30) | NOT NULL, UNIQUE |
| patient_id | BIGINT | FK → Patients.patient_id |
| doctor_id | BIGINT | FK → Doctors.doctor_id |
| location_id | BIGINT | FK → Locations.location_id |
| service_id | BIGINT | FK → Services.service_id |
| slot_id | BIGINT | FK → TimeSlots.slot_id, UNIQUE |
| status | ENUM('PENDING_PAYMENT','CONFIRMED','COMPLETED','CANCELLED','RESCHEDULED','NO_SHOW') | DEFAULT 'PENDING_PAYMENT' |
| reason_for_visit | TEXT | NULLABLE |
| amount | DECIMAL(10,2) | NOT NULL |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP |

**Payments**

| Column | Type | Constraints |
|---|---|---|
| payment_id | BIGINT | PK, AUTO_INCREMENT |
| appointment_id | BIGINT | FK → Appointments.appointment_id, UNIQUE |
| razorpay_order_id | VARCHAR(100) | NOT NULL |
| razorpay_payment_id | VARCHAR(100) | NULLABLE |
| razorpay_signature | VARCHAR(255) | NULLABLE |
| amount | DECIMAL(10,2) | NOT NULL |
| currency | VARCHAR(10) | DEFAULT 'INR' |
| status | ENUM('PENDING','SUCCESS','FAILED','REFUNDED') | DEFAULT 'PENDING' |
| invoice_url | VARCHAR(255) | NULLABLE |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

**Reviews**

| Column | Type | Constraints |
|---|---|---|
| review_id | BIGINT | PK, AUTO_INCREMENT |
| appointment_id | BIGINT | FK → Appointments.appointment_id, UNIQUE |
| patient_id | BIGINT | FK → Patients.patient_id |
| doctor_id | BIGINT | FK → Doctors.doctor_id |
| rating | TINYINT | NOT NULL, CHECK (rating BETWEEN 1 AND 5) |
| comment | TEXT | NULLABLE |
| is_visible | BOOLEAN | DEFAULT TRUE |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### 8.3 Normalization & Indexing Notes

- Schema is normalized to **3NF**; role-specific patient/doctor attributes are separated from the core `Users` table to avoid nullable-heavy wide tables.
- Recommended indexes: composite index on `TimeSlots(doctor_id, slot_date, status)` for fast availability lookups; index on `Appointments(patient_id, status)` and `Appointments(doctor_id, status)` for dashboard queries; index on `Payments(status)` for admin reporting.
- Soft deletes (`is_active` flags) are preferred over hard deletes for Doctors, Services, and Locations to preserve historical appointment integrity.

---

## 9. API Design

The API follows **REST conventions**, uses **JSON** as the payload format, and is versioned under `/api/v1`. All protected endpoints require a `Bearer <JWT>` token in the `Authorization` header.

### 9.1 API Design Principles

- Resource-oriented URLs (nouns, not verbs)
- Standard HTTP methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`
- Consistent JSON envelope for success and error responses
- Pagination via `page` and `size` query parameters for list endpoints
- Standard HTTP status codes (200, 201, 204, 400, 401, 403, 404, 409, 422, 500)

**Standard Success Response:**
```json
{
  "success": true,
  "data": { },
  "message": "Request successful"
}
```

**Standard Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "SLOT_ALREADY_BOOKED",
    "message": "The selected time slot is no longer available."
  }
}
```

### 9.2 Endpoint Summary

**Authentication**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Patient self-registration | Public |
| POST | `/api/v1/auth/login` | Login, returns JWT + refresh token | Public |
| POST | `/api/v1/auth/refresh-token` | Issue new access token | Public (valid refresh token) |
| POST | `/api/v1/auth/logout` | Invalidate refresh token | Authenticated |
| POST | `/api/v1/auth/forgot-password` | Trigger reset email | Public |
| POST | `/api/v1/auth/reset-password` | Reset password via token | Public |

**Services & Locations**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/v1/services` | List active services | Public |
| GET | `/api/v1/locations` | List active service locations | Public |
| POST | `/api/v1/admin/services` | Create service | Admin |
| PUT | `/api/v1/admin/services/{id}` | Update service | Admin |
| POST | `/api/v1/admin/locations` | Create location | Admin |

**Doctors**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/v1/doctors` | List doctors (filter by service, location) | Public |
| GET | `/api/v1/doctors/{id}` | Doctor profile detail | Public |
| GET | `/api/v1/doctors/{id}/slots?date=YYYY-MM-DD&locationId=` | Available time slots for a date | Public |
| PUT | `/api/v1/doctors/me/availability` | Set weekly availability | Doctor |
| PUT | `/api/v1/doctors/me/breaks` | Set break times | Doctor |
| POST | `/api/v1/doctors/me/holidays` | Add holiday | Doctor |
| GET | `/api/v1/doctors/me/appointments` | Doctor's appointment list | Doctor |
| GET | `/api/v1/doctors/me/patients` | Doctor's patient list | Doctor |

**Appointments**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/appointments` | Create appointment (holds slot, pending payment) | Patient |
| GET | `/api/v1/appointments/me` | Patient's appointments (query `status=upcoming\|completed`) | Patient |
| GET | `/api/v1/appointments/{id}` | Appointment detail | Owner / Doctor / Admin |
| PATCH | `/api/v1/appointments/{id}/reschedule` | Reschedule to a new slot | Patient |
| PATCH | `/api/v1/appointments/{id}/cancel` | Cancel appointment | Patient |
| PATCH | `/api/v1/appointments/{id}/status` | Update status (e.g., COMPLETED, NO_SHOW) | Doctor / Admin |
| GET | `/api/v1/admin/appointments` | Global appointment search/filter | Admin |

**Payments**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/payments/create-order` | Create Razorpay order for an appointment | Patient |
| POST | `/api/v1/payments/verify` | Verify Razorpay signature and confirm appointment | Patient |
| POST | `/api/v1/payments/webhook` | Razorpay server-to-server webhook | Public (signature-verified) |
| GET | `/api/v1/payments/me` | Patient's payment history | Patient |
| GET | `/api/v1/payments/{id}/invoice` | Download invoice PDF | Owner / Admin |
| POST | `/api/v1/admin/payments/{id}/refund` | Initiate refund | Admin |

**Reviews**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/reviews` | Submit review for a completed appointment | Patient |
| GET | `/api/v1/doctors/{id}/reviews` | List reviews for a doctor | Public |
| DELETE | `/api/v1/admin/reviews/{id}` | Moderate/remove review | Admin |

**Admin Analytics**

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/v1/admin/analytics/overview` | Revenue, appointments, active doctors summary | Admin |
| GET | `/api/v1/admin/analytics/revenue?from=&to=` | Revenue trend | Admin |
| GET | `/api/v1/admin/reports/export?type=appointments&format=csv` | Export reports | Admin |

### 9.3 Sample Request/Response

**POST `/api/v1/appointments`**
```json
// Request
{
  "doctorId": 12,
  "locationId": 3,
  "serviceId": 2,
  "slotId": 4521,
  "reasonForVisit": "Lower back pain, 2 weeks duration"
}

// Response 201 Created
{
  "success": true,
  "data": {
    "appointmentId": 8891,
    "referenceNo": "PHY-20260725-8891",
    "status": "PENDING_PAYMENT",
    "amount": 800.00,
    "holdExpiresAt": "2026-07-25T10:35:00Z"
  },
  "message": "Slot held. Complete payment to confirm appointment."
}
```

---

## 10. Security

### 10.1 Authentication & Authorization

- **JWT-based stateless authentication**: On successful login, the server issues a short-lived **access token** (e.g., 15 minutes) and a longer-lived **refresh token** (e.g., 7 days, stored securely and rotated on use).
- **Role-Based Access Control (RBAC)** enforced via Spring Security method-level annotations (`@PreAuthorize`) mapped to `PATIENT`, `DOCTOR`, and `ADMIN` roles.
- Refresh tokens are stored server-side (or as HttpOnly, Secure, SameSite=Strict cookies) to mitigate XSS-based token theft; access tokens are kept in memory on the frontend, not in `localStorage`, to reduce XSS exposure.

### 10.2 Data Protection

- Passwords hashed with **BCrypt** (cost factor ≥ 10); plaintext passwords are never logged or stored.
- All data in transit is encrypted via **TLS 1.2+**; HTTP is redirected to HTTPS at the reverse proxy.
- Sensitive fields (e.g., payment identifiers) are never returned in full in API responses beyond what is operationally necessary.
- Database credentials, JWT secrets, and Razorpay API keys are managed via environment variables / a secrets manager (e.g., AWS Secrets Manager, HashiCorp Vault) — never committed to source control.

### 10.3 Application-Level Security Controls

| Control | Implementation |
|---|---|
| Input Validation | Bean Validation (`jakarta.validation`) on all DTOs; server-side validation regardless of client-side checks |
| SQL Injection Prevention | Parameterized queries via Spring Data JPA/Hibernate (no raw string concatenation) |
| XSS Prevention | React's default output escaping; sanitization of any rendered HTML/user-generated content |
| CSRF | Not applicable to stateless JWT APIs consumed by SPA with `Authorization` header; CSRF protection enabled if cookie-based auth is used for any endpoint |
| CORS | Explicit allow-list of frontend origins configured in Spring Security |
| Rate Limiting | Applied at the reverse proxy / API gateway layer on auth and payment endpoints to mitigate brute-force and abuse |
| Payment Integrity | Razorpay payment signature verified server-side (HMAC SHA256) before confirming any appointment; webhook signature also verified independently |
| Audit Logging | All status-changing actions (booking, cancellation, refund, admin overrides) logged with actor ID, timestamp, and IP address |
| Least Privilege | Database service account scoped to only the application schema, with no superuser privileges |
| Dependency Hygiene | Regular dependency vulnerability scanning (e.g., OWASP Dependency-Check, `npm audit`) as part of CI pipeline |

### 10.4 Compliance Considerations

Since PhysioConnect handles health-adjacent personal data (reason for visit, medical history notes), the following practices are recommended as industry best practice:

- Data minimization — only clinically necessary information is collected.
- Access to patient medical notes restricted to the assigned doctor and authorized admins only, enforced at the service layer (not just UI hiding).
- Support for data subject rights (access, correction, deletion) in line with applicable regional data protection law (e.g., India's DPDP Act, and GDPR-aligned principles if serving EU users).

---

## 11. Future Enhancements

| Enhancement | Description |
|---|---|
| Telemedicine Integration | In-app video consultation for follow-up sessions |
| Native Mobile Apps | React Native or Flutter apps for Patient and Doctor roles |
| AI-Based Exercise Plans | Personalized rehabilitation exercise recommendations based on diagnosis |
| Wearable Integration | Sync recovery progress from wearable devices |
| Insurance Integration | Claims submission and cashless treatment support |
| Multi-language Support | i18n for regional language accessibility |
| WhatsApp Notifications | Appointment reminders via WhatsApp Business API alongside email |
| Doctor Payout Automation | Automated periodic payouts to doctors via Razorpay Route |
| Advanced Analytics | Predictive analytics for no-show risk and demand forecasting per location |
| Microservices Migration | Extraction of Payment and Notification modules into independent services if scale requires it |

---

## 12. Conclusion

PhysioConnect is architected as a secure, scalable, and maintainable full-stack platform that digitizes the complete physiotherapy clinic workflow — from patient discovery and booking through payment, treatment, and post-visit feedback. By adopting a layered Spring Boot backend, a stateless JWT security model, a normalized MySQL schema with concurrency-safe slot management, and a modern React SPA frontend, the system is positioned to meet production-grade reliability, security, and performance expectations while remaining extensible for future enhancements such as telemedicine and mobile applications.

This document should be treated as a living artifact, updated alongside the codebase as requirements evolve, and used as the baseline reference for engineering, QA, and client sign-off.

---

*Document prepared by the Software Architecture & Engineering Team — PhysioConnect Project.*
