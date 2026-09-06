# PhysioConnect — Immediate Implementation Notes

## Location model

V1 location support means geographic **service locations/areas**, not multiple clinic branches. The current repository already contains:

- `doctor_locations` many-to-many relationship
- `doctor_availability.location_id`
- `time_slots.location_id`
- `appointments.location_id`

Therefore, do not replace this with a single `doctor.clinic_id` model.

At booking time the backend must validate:

1. selected location is active;
2. doctor serves the selected location;
3. selected slot belongs to the same doctor and location;
4. the appointment stores the selected location.

## Medical history

V1 requires basic medical-history notes captured at booking. Store them in `appointments.medical_history_notes` so they remain associated with the visit context.

Security:

- Patient can access their own notes.
- Assigned doctor can access notes for that doctor's appointments.
- Authorized Admin can access notes according to the existing admin/support authorization model.
- Never log note contents.
- Do not introduce EMR/EHR features.

## Git

Do not rewrite the existing `main` history. The repository already contains the preserved commits `96ba952` and `78bf303`. Make the next change on a short-lived feature branch, then self-review before merging.
