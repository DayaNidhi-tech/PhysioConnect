# PhysioConnect Development Progress

## Current Checkpoint

- Backend: Spring Boot + Spring Security + Spring Data JPA
- Database: MySQL with Flyway migrations through version 6
- Authentication: JWT access token and refresh-token flow implemented
- Patient registration: creates the User and Patient profile transactionally
- Email verification: implemented and tested successfully
- Appointment booking: booking, cancellation, rescheduling, and status workflows implemented
- Medical history notes: supported as appointment-context information
- Multi-location service coverage: supported through doctor-location assignments
- Razorpay: Java SDK dependency and server-side test-order flow implemented

## Development Progress — 2026-09-12

- Verified the patient email-verification flow against the running Spring Boot application.
- Verified successful patient login and JWT issuance after email verification.
- Inspected the live MySQL schema and confirmed the appointment-related tables and relationships are present.
- Identified and fixed the patient-profile creation gap in patient registration.
- Updated the registration test cleanup so Patient records are removed before User records.
- Confirmed the backend test suite passes after the registration/profile fix.
- Kept local Razorpay credentials and JWT secrets outside the repository.

## Next Development Steps

1. Populate valid development doctor, location, and service data.
2. Configure doctor availability and generate test time slots.
3. Complete end-to-end appointment booking verification.
4. Complete Razorpay Test Mode payment verification.
5. Continue remaining backend modules and frontend integration.

## Security Note

Local Razorpay credentials and JWT secrets must remain environment variables and must never be committed to the repository.
