package com.physioconnect.service;

import com.physioconnect.dto.AppointmentResponse;
import com.physioconnect.dto.CreateAppointmentRequest;
import com.physioconnect.dto.RescheduleAppointmentRequest;
import com.physioconnect.dto.UpdateAppointmentStatusRequest;
import com.physioconnect.entity.Appointment;
import com.physioconnect.entity.Doctor;
import com.physioconnect.entity.Location;
import com.physioconnect.entity.Patient;
import com.physioconnect.entity.PhysioService;
import com.physioconnect.entity.TimeSlot;
import com.physioconnect.entity.enums.AppointmentStatus;
import com.physioconnect.entity.enums.SlotStatus;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.AppointmentRepository;
import com.physioconnect.repository.DoctorRepository;
import com.physioconnect.repository.LocationRepository;
import com.physioconnect.repository.PatientRepository;
import com.physioconnect.repository.PhysioServiceRepository;
import com.physioconnect.repository.TimeSlotRepository;
import com.physioconnect.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final long HOLD_MINUTES = 10;
    private static final long CANCELLATION_CUTOFF_HOURS = 4;
    private static final int MAX_RESCHEDULES = 2;

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final LocationRepository locationRepository;
    private final PhysioServiceRepository physioServiceRepository;
    private final TimeSlotRepository timeSlotRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            LocationRepository locationRepository,
            PhysioServiceRepository physioServiceRepository,
            TimeSlotRepository timeSlotRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.locationRepository = locationRepository;
        this.physioServiceRepository = physioServiceRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public AppointmentResponse createAppointment(
            UserPrincipal principal,
            CreateAppointmentRequest request
    ) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!Boolean.TRUE.equals(doctor.getUser().getIsActive())
                || !Boolean.TRUE.equals(doctor.getIsApproved())) {
            throw new BadRequestException("Doctor is not active or approved");
        }

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Service location not found"));

        if (!Boolean.TRUE.equals(location.getIsActive())) {
            throw new BadRequestException("Service location is inactive");
        }

        if (doctor.getLocations() == null || !doctor.getLocations().contains(location)) {
            throw new BadRequestException("Doctor is not assigned to the selected service location");
        }

        PhysioService service = physioServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new BadRequestException("Service is inactive");
        }

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found"));

        if (!slot.getDoctor().getId().equals(doctor.getId())
                || !slot.getLocation().getId().equals(location.getId())) {
            throw new BadRequestException("Selected slot does not match the doctor and location");
        }

        LocalDateTime slotDateTime = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        LocalDateTime now = LocalDateTime.now();

        if (!slotDateTime.isAfter(now)) {
            throw new BadRequestException("Selected time slot has already started or passed");
        }

        if (slot.getStatus() == SlotStatus.HELD
                && slot.getHeldUntil() != null
                && slot.getHeldUntil().isBefore(now)) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setHeldUntil(null);
        }

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new BadRequestException("The selected time slot is no longer available");
        }

        List<Appointment> activeAppointments = appointmentRepository
                .findByPatientIdAndDoctorIdAndStatusIn(
                        patient.getId(),
                        doctor.getId(),
                        List.of(AppointmentStatus.PENDING_PAYMENT, AppointmentStatus.CONFIRMED)
                );

        boolean conflictingAppointment = activeAppointments.stream()
                .anyMatch(appointment -> {
                    LocalTime startTime = appointment.getSlot().getStartTime();
                    LocalDateTime appointmentDateTime = LocalDateTime.of(
                            appointment.getSlot().getSlotDate(),
                            startTime
                    );
                    return appointmentDateTime.isAfter(now)
                            && appointmentDateTime.equals(slotDateTime);
                });

        if (conflictingAppointment) {
            throw new BadRequestException(
                    "You already have an active future appointment with this doctor at the selected time"
            );
        }

        LocalDateTime holdExpiresAt = now.plusMinutes(HOLD_MINUTES);

        slot.setStatus(SlotStatus.HELD);
        slot.setHeldUntil(holdExpiresAt);
        timeSlotRepository.save(slot);

        Appointment appointment = Appointment.builder()
                .referenceNo("TEMP")
                .patient(patient)
                .doctor(doctor)
                .location(location)
                .service(service)
                .slot(slot)
                .status(AppointmentStatus.PENDING_PAYMENT)
                .reasonForVisit(request.reasonForVisit())
                .medicalHistoryNotes(request.medicalHistoryNotes())
                .amount(service.getBasePrice())
                .rescheduleCount(0)
                .build();

        appointment = appointmentRepository.save(appointment);
        appointment.setReferenceNo(generateReferenceNo(appointment.getId()));
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment, holdExpiresAt);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getMyAppointments(
            UserPrincipal principal,
            String status,
            Pageable pageable
    ) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Page<Appointment> appointments;

        if (status == null || status.isBlank()) {
            appointments = appointmentRepository.findByPatientId(patient.getId(), pageable);
        } else if ("upcoming".equalsIgnoreCase(status)) {
            appointments = appointmentRepository.findByPatientIdAndStatus(
                    patient.getId(), AppointmentStatus.CONFIRMED, pageable
            );
        } else if ("completed".equalsIgnoreCase(status)) {
            appointments = appointmentRepository.findByPatientIdAndStatus(
                    patient.getId(), AppointmentStatus.COMPLETED, pageable
            );
        } else {
            throw new BadRequestException("Status must be upcoming or completed");
        }

        return appointments.map(appointment -> AppointmentResponse.from(
                appointment,
                appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT
                        ? appointment.getSlot().getHeldUntil()
                        : null
        ));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(
            UserPrincipal principal,
            Long appointmentId
    ) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!canView(principal, appointment)) {
            throw new BadRequestException("You are not authorized to view this appointment");
        }

        return AppointmentResponse.from(
                appointment,
                appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT
                        ? appointment.getSlot().getHeldUntil()
                        : null
        );
    }

    @Transactional
    public AppointmentResponse cancelAppointment(
            UserPrincipal principal,
            Long appointmentId
    ) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        ensurePatientOwnsAppointment(principal, appointment);
        ensureCancellable(appointment);

        LocalDateTime scheduledAt = getScheduledAt(appointment);
        ensureCancellationWindow(scheduledAt);

        releaseSlot(appointment.getSlot());
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment, null);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(
            UserPrincipal principal,
            Long appointmentId,
            RescheduleAppointmentRequest request
    ) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        ensurePatientOwnsAppointment(principal, appointment);
        ensureReschedulable(appointment);

        if (appointment.getRescheduleCount() >= MAX_RESCHEDULES) {
            throw new BadRequestException("Maximum of 2 reschedules has been reached");
        }

        LocalDateTime oldScheduledAt = getScheduledAt(appointment);
        ensureCancellationWindow(oldScheduledAt);

        Long oldSlotId = appointment.getSlot().getId();
        if (oldSlotId.equals(request.slotId())) {
            throw new BadRequestException("New slot must be different from the current slot");
        }

        TimeSlot oldSlot;
        TimeSlot newSlot;

        if (oldSlotId < request.slotId()) {
            oldSlot = timeSlotRepository.findByIdForUpdate(oldSlotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Current time slot not found"));
            newSlot = timeSlotRepository.findByIdForUpdate(request.slotId())
                    .orElseThrow(() -> new ResourceNotFoundException("New time slot not found"));
        } else {
            newSlot = timeSlotRepository.findByIdForUpdate(request.slotId())
                    .orElseThrow(() -> new ResourceNotFoundException("New time slot not found"));
            oldSlot = timeSlotRepository.findByIdForUpdate(oldSlotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Current time slot not found"));
        }

        validateNewSlot(appointment, newSlot);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newScheduledAt = LocalDateTime.of(
                newSlot.getSlotDate(),
                newSlot.getStartTime()
        );

        if (!newScheduledAt.isAfter(now)) {
            throw new BadRequestException("New time slot has already started or passed");
        }

        if (newSlot.getStatus() == SlotStatus.HELD
                && newSlot.getHeldUntil() != null
                && newSlot.getHeldUntil().isBefore(now)) {
            newSlot.setStatus(SlotStatus.AVAILABLE);
            newSlot.setHeldUntil(null);
        }

        if (newSlot.getStatus() != SlotStatus.AVAILABLE) {
            throw new BadRequestException("The selected time slot is no longer available");
        }

        List<Appointment> activeAppointments = appointmentRepository
                .findByPatientIdAndDoctorIdAndStatusIn(
                        appointment.getPatient().getId(),
                        appointment.getDoctor().getId(),
                        List.of(AppointmentStatus.PENDING_PAYMENT, AppointmentStatus.CONFIRMED)
                );

        boolean conflictingAppointment = activeAppointments.stream()
                .filter(existing -> !existing.getId().equals(appointment.getId()))
                .anyMatch(existing -> getScheduledAt(existing).equals(newScheduledAt));

        if (conflictingAppointment) {
            throw new BadRequestException(
                    "You already have an active future appointment with this doctor at the selected time"
            );
        }

        releaseSlot(oldSlot);

        if (appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT) {
            LocalDateTime holdExpiresAt = now.plusMinutes(HOLD_MINUTES);
            newSlot.setStatus(SlotStatus.HELD);
            newSlot.setHeldUntil(holdExpiresAt);
        } else {
            newSlot.setStatus(SlotStatus.BOOKED);
            newSlot.setHeldUntil(null);
        }

        timeSlotRepository.save(newSlot);

        appointment.setSlot(newSlot);
        appointment.setLocation(newSlot.getLocation());
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        appointment = appointmentRepository.save(appointment);

        LocalDateTime holdExpiresAt = appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT
                ? newSlot.getHeldUntil()
                : null;

        return AppointmentResponse.from(appointment, holdExpiresAt);
    }

    @Transactional
    public AppointmentResponse updateAppointmentStatus(
            UserPrincipal principal,
            Long appointmentId,
            UpdateAppointmentStatusRequest request
    ) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        ensureDoctorOrAdminCanUpdate(principal, appointment);

        AppointmentStatus newStatus = parseStatus(request.status());

        if (newStatus != AppointmentStatus.COMPLETED
                && newStatus != AppointmentStatus.NO_SHOW) {
            throw new BadRequestException("Status can only be COMPLETED or NO_SHOW");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed appointments can be marked completed or no-show");
        }

        if (!getScheduledAt(appointment).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Appointment status can only be updated after the scheduled time");
        }

        appointment.setStatus(newStatus);
        appointment = appointmentRepository.save(appointment);

        return AppointmentResponse.from(appointment, null);
    }

    private void ensurePatientOwnsAppointment(UserPrincipal principal, Appointment appointment) {
        if (!hasRole(principal, "ROLE_PATIENT")
                || !appointment.getPatient().getUser().getId().equals(principal.getId())) {
            throw new BadRequestException("You are not authorized to modify this appointment");
        }
    }

    private void ensureDoctorOrAdminCanUpdate(UserPrincipal principal, Appointment appointment) {
        if (hasRole(principal, "ROLE_ADMIN")) {
            return;
        }

        if (hasRole(principal, "ROLE_DOCTOR")
                && appointment.getDoctor().getUser().getId().equals(principal.getId())) {
            return;
        }

        throw new BadRequestException("You are not authorized to update this appointment");
    }

    private void ensureCancellable(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.PENDING_PAYMENT
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only pending or confirmed appointments can be cancelled");
        }
    }

    private void ensureReschedulable(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.PENDING_PAYMENT
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only pending or confirmed appointments can be rescheduled");
        }
    }

    private void ensureCancellationWindow(LocalDateTime scheduledAt) {
        LocalDateTime now = LocalDateTime.now();
        if (!scheduledAt.isAfter(now.plusHours(CANCELLATION_CUTOFF_HOURS))) {
            throw new BadRequestException("Appointments can only be cancelled or rescheduled at least 4 hours before the scheduled time");
        }
    }

    private void validateNewSlot(Appointment appointment, TimeSlot newSlot) {
        if (!newSlot.getDoctor().getId().equals(appointment.getDoctor().getId())) {
            throw new BadRequestException("New slot must belong to the same doctor");
        }

        if (!newSlot.getLocation().getId().equals(appointment.getLocation().getId())) {
            throw new BadRequestException("New slot must belong to the same service location");
        }
    }

    private void releaseSlot(TimeSlot slot) {
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setHeldUntil(null);
        timeSlotRepository.save(slot);
    }

    private LocalDateTime getScheduledAt(Appointment appointment) {
        return LocalDateTime.of(
                appointment.getSlot().getSlotDate(),
                appointment.getSlot().getStartTime()
        );
    }

    private AppointmentStatus parseStatus(String status) {
        try {
            return AppointmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Invalid appointment status");
        }
    }

    private boolean canView(UserPrincipal principal, Appointment appointment) {
        if (hasRole(principal, "ROLE_ADMIN")) {
            return true;
        }

        if (hasRole(principal, "ROLE_PATIENT")) {
            return appointment.getPatient().getUser().getId().equals(principal.getId());
        }

        if (hasRole(principal, "ROLE_DOCTOR")) {
            return appointment.getDoctor().getUser().getId().equals(principal.getId());
        }

        return false;
    }

    private boolean hasRole(UserPrincipal principal, String role) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private String generateReferenceNo(Long appointmentId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PHY-" + appointmentId + "-" + suffix;
    }
}
