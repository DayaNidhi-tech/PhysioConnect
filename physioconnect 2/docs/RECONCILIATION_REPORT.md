# PhysioConnect — Final Reconciliation Report (Current Baseline)

## 1. Authoritative Decisions

- Medical history / medical notes are **V1 functionality**.
- Multi-location means **multiple geographic service locations/areas**, not multiple clinic branches.
- A doctor may be configured to serve multiple locations.
- Daya Nidhi is the sole developer/project lead.

## 2. Repository Verification

The current public repository already contains a `doctor_locations` many-to-many relationship and location-specific doctor availability, so the codebase is already structurally aligned with doctors serving multiple locations. The appointment entity already stores `location_id` and `reason_for_visit`.

The existing database migration also contains `doctor_locations`, `doctor_availability.location_id`, `time_slots.location_id`, and `appointments.location_id`. Therefore, multi-location does not require replacing the existing location architecture.

## 3. Required Documentation Corrections

| Area | Finding | Resolution |
|---|---|---|
| Multi-location | Several documents described V1 as single-clinic | Replace with geographic service-location model |
| Doctor-location | Earlier audit treated multi-location doctor assignment as unresolved | Repository already uses many-to-many `doctor_locations`; requirement is now explicit |
| Availability | Location context is required | Existing schema already has `doctor_availability.location_id` and `time_slots.location_id` |
| Appointment | Appointment already stores `location_id` | Retain and validate selected location against doctor coverage |
| Medical history | No persistence field existed | Add `medical_history_notes` to appointment/booking context |
| Medical privacy | Generic privacy rules existed | Explicitly include medical-history notes in authorization/logging/test rules |

## 4. Medical-History Design Boundary

V1 stores **basic medical-history notes relevant to the appointment**. This is not a full clinical-record subsystem. No prescriptions, diagnoses, laboratory records, imaging, treatment-plan management, or EMR workflow is introduced by this reconciliation.

Recommended persistence boundary: `appointments.medical_history_notes`, because the existing requirement captures the information at booking time and the appointment is the relevant visit context.

## 5. Coding Changes Prepared

- Add `medical_history_notes` to `Appointment`.
- Add a forward-only Flyway migration for the new nullable TEXT column.
- Keep existing multi-location tables/relationships.
- Enforce location consistency in the booking Service: selected location must be active, doctor must serve it, and the selected slot must belong to that doctor/location.
- Never log medical-history note content.

## 6. Remaining Source-Level Verification

The separate legacy files `DDD.md`, `API_SPEC.md`, and `UX_SPEC.md` are not present as standalone current files in the working attachment set available for direct editing here. Their required changes are nevertheless explicitly captured by this report and must be synchronized before release. The repository's actual schema/code was checked where accessible and confirms the multi-location foundation.

## 7. Development Status

**Documentation baseline: READY TO IMPLEMENT THE NEXT BACKEND SLICE, but not a final release sign-off.**

The remaining API/UI documentation synchronization for medical-history fields must be reflected in the standalone documents before those layers are implemented.

## 8. First Implementation Slice

1. `Appointment.java` — add `medicalHistoryNotes`.
2. `V3__add_medical_history_notes.sql` — add nullable `TEXT` column.
3. Appointment request/response DTOs — add the field only where the API contract requires it.
4. Booking Service — persist it and enforce location/doctor/slot consistency.
5. Security tests — verify patient/assigned-doctor/admin access and cross-patient denial.
