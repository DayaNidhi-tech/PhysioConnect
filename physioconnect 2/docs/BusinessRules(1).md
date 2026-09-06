# PhysioConnect — Business Rules (v1)

These rules are binding constraints the system must enforce. Each rule maps to
a future DB constraint, service-layer check, or validation — referenced by ID
in later design/dev docs.

## Booking Rules

- **BR-001**: A doctor's day is divided into fixed-duration slots (default:
  30 minutes, configurable by Admin per service type).
- **BR-002**: Two appointments cannot occupy the same doctor + time-slot
  (no double-booking). Enforced at DB level (unique constraint) AND service
  level (pre-check).
- **BR-003**: A patient cannot book a new appointment with the same doctor
  while they already have an active (PENDING or CONFIRMED) appointment with
  that doctor in the future.
- **BR-004**: Appointments can only be booked into slots marked "Available"
  by the doctor. Doctors can block dates/times (leave).
- **BR-005**: Booking requires the patient to be authenticated (no guest
  booking in v1).

## Cancellation Rules

- **BR-006**: A patient may cancel a CONFIRMED or PENDING appointment up to
  **4 hours** before the scheduled time. (Confirmed.)
- **BR-007**: Cancellations inside the cutoff window require Admin override
  or are simply disallowed via UI (patient must call clinic) — decide during
  design phase.
- **BR-008**: A cancelled slot immediately becomes "Available" again for
  other patients.

## Rescheduling Rules

- **BR-009**: Rescheduling is treated as: cancel existing slot + book new
  slot, subject to the same cutoff rule as BR-006.
- **BR-010**: Reschedule count is capped at **2 reschedules per appointment**.
  On the 3rd reschedule attempt, the system shall block the action and
  direct the patient to cancel and rebook, or contact the clinic. (Confirmed.)

## Payment Rules

- **BR-011**: Payment status (`UNPAID`, `PAID`, `REFUND_PENDING`,
  `REFUNDED`) is tracked independently of booking status
  (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`).
- **BR-012**: If a patient pays online (Razorpay) and then cancels within
  the allowed window, a refund is initiated (manual Admin trigger in v1,
  automated in v2).
- **BR-013**: "Pay at Clinic" bookings are marked `UNPAID` until Admin
  manually marks them `PAID` post-visit.

## No-Show Rules

- **BR-014**: Admin (or Doctor) can mark a CONFIRMED appointment as
  `NO_SHOW` after the scheduled time has passed.
- **BR-015**: No-show appointments do not trigger automatic refunds.

## Review Rules

- **BR-016**: A patient can only review a doctor after an appointment is
  marked `COMPLETED`.
- **BR-017**: One review per completed appointment (not one per doctor —
  a patient can review the same doctor again after another completed visit).

## Notification Rules

- **BR-018**: Email triggers: booking confirmed, booking cancelled,
  appointment reminder (24h before), payment success/failure.

## Location & Medical History Rules

- **BR-019**: A bookable appointment location must be an active service location supported by the selected doctor. The selected `locationId` must match the doctor's configured service-area/location coverage and the slot being booked.
- **BR-020**: Medical-history notes captured during booking are sensitive patient information. Access is restricted to the patient who supplied the information, the doctor assigned to the appointment, and authorized Admin users.
- **BR-021**: Medical-history notes are basic appointment-context information only. PhysioConnect v1 does not implement a full EMR/EHR, prescription management, laboratory records, imaging records, or other clinical-record subsystems.

---

## ⚠️ Open Items Requiring Team Decision Before Design Phase

- [x] Exact cancellation cutoff window (BR-006) — **Resolved: 4 hours**
- [x] Reschedule cap (BR-010) — **Resolved: capped at 2**
- [ ] Who can override late cancellations — Admin only? (still open, low
  priority — can be resolved during Phase 2 design without blocking DB/API work)

- [x] Location model for v1 — resolved: locations represent service locations/areas, not a network of clinic branches; doctors may be configured for multiple service locations.
- [x] Medical-history storage boundary — resolved for v1: basic medical-history notes are captured with the appointment/booking context; this is not a full EMR/EHR.
