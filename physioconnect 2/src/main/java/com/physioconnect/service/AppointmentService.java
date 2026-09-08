package com.physioconnect.service;

import com.physioconnect.dto.AppointmentResponse;
import com.physioconnect.dto.CreateAppointmentRequest;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final long HOLD_MINUTES = 10;

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
