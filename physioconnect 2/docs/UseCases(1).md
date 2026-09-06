# PhysioConnect — Use Cases (v1)

Format: **UC-ID | Actor | Goal | Preconditions | Main Flow | Alternate/Error Flow**

---

## Patient Use Cases

### UC-P01: Patient Registration
- **Actor**: Patient (unauthenticated)
- **Goal**: Create an account to book appointments
- **Preconditions**: None
- **Main Flow**: Enter name, email, phone, password → system validates →
  account created → JWT issued → redirected to dashboard
- **Alternate**: Email already exists → show error, suggest login

### UC-P02: Patient Login
- **Actor**: Patient
- **Main Flow**: Enter email/password → validate → issue JWT → redirect to dashboard
- **Alternate**: Invalid credentials → error message; account locked after N
  failed attempts (security decision for Phase 2)

### UC-P03: Browse Doctors / Services
- **Actor**: Patient
- **Main Flow**: Choose a service/location → view doctors who serve the selected location and service → view doctor profile, ratings, and available slots

### UC-P04: Book Appointment
- **Actor**: Patient
- **Preconditions**: Logged in, selected location is active, doctor serves the location, and doctor has an available slot
- **Main Flow**: Select service → select service location → select doctor → select available slot → enter/confirm patient details including reason for visit and basic medical-history notes → choose payment method (Razorpay / Pay at Clinic) → confirm → appointment created as PENDING or CONFIRMED (per BR + payment result)
- **Alternate**: Slot taken between selection and confirmation → show error,
  refresh slots (race condition — must handle server-side, see BR-002)

### UC-P05: Cancel Appointment
- **Actor**: Patient
- **Preconditions**: Appointment is PENDING/CONFIRMED, outside cutoff window (BR-006)
- **Main Flow**: Select appointment → cancel → confirm → slot released,
  refund initiated if applicable (BR-012)
- **Alternate**: Inside cutoff window → cancellation blocked, show contact-clinic message

### UC-P06: Reschedule Appointment
- **Actor**: Patient
- **Main Flow**: Select appointment → choose new available slot → confirm
  → old slot released, new slot booked (per BR-009)
- **Alternate**: Appointment has already been rescheduled 2 times (BR-010)
  → reschedule blocked, prompt patient to cancel/rebook or contact clinic

### UC-P07: View Appointment History
- **Actor**: Patient
- **Main Flow**: View list of past/upcoming appointments with status, payment, doctor

### UC-P09: Provide Medical History / Visit Notes
- **Actor**: Patient
- **Preconditions**: Logged in and booking an appointment
- **Main Flow**: Enter basic medical-history notes relevant to the visit → review before submission → submit with the booking
- **Alternate**: Missing/invalid input → show validation error; unauthorized access to another patient’s notes is rejected server-side

### UC-P08: Leave a Review
- **Actor**: Patient
- **Preconditions**: Appointment status = COMPLETED (BR-016)
- **Main Flow**: Select completed appointment → rate (1-5) + comment → submit

---

## Doctor Use Cases

### UC-D01: Doctor Login
- **Actor**: Doctor (account created by Admin)
- **Main Flow**: Login with Admin-provisioned credentials → JWT issued

### UC-D02: Manage Availability
- **Actor**: Doctor
- **Main Flow**: Configure supported service locations → set weekly working hours per supported location → set slot duration → block specific dates/times (leave) → system regenerates available slots

### UC-D03: View Appointments / Calendar
- **Actor**: Doctor
- **Main Flow**: View upcoming appointments in list/calendar view, filter by date

### UC-D04: Manage Patient Visit
- **Actor**: Doctor
- **Main Flow**: View medical-history notes for assigned appointments → mark appointment COMPLETED or NO_SHOW after scheduled time (BR-014)

### UC-D05: View Reviews
- **Actor**: Doctor
- **Main Flow**: View own ratings/reviews from patients

---

## Admin Use Cases

### UC-A01: Admin Login
- **Actor**: Admin
- **Main Flow**: Login with elevated-privilege credentials

### UC-A02: Manage Doctors
- **Actor**: Admin
- **Main Flow**: Create/edit/deactivate doctor accounts, assign specialties/services, and configure the service locations each doctor supports

### UC-A03: Manage Patients
- **Actor**: Admin
- **Main Flow**: View/search patients, deactivate accounts if needed (abuse, etc.)

### UC-A04: Manage Services
- **Actor**: Admin
- **Main Flow**: Create/edit services (name, duration, price)

### UC-A05: Manage Payments
- **Actor**: Admin
- **Main Flow**: View payment records, mark "Pay at Clinic" as PAID, trigger
  manual refunds (BR-012)

### UC-A07: Manage Service Locations
- **Actor**: Admin
- **Main Flow**: Create, edit, activate/deactivate service locations and maintain their address/service-area information

### UC-A06: View Analytics
- **Actor**: Admin
- **Main Flow**: View dashboard: total appointments, revenue, no-show rate,
  top-rated doctors, date-range filters

---

## Use Case Coverage Check

Every role in PROJECT_CONTEXT.md is covered. Each UC above will map to:
- One or more REST API endpoints (Phase 2: System Design)
- One or more DB entities/relations (Phase 2: DB Design)
- One or more test cases (Phase 5: Testing)
