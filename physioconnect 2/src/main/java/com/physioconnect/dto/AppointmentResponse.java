package com.physioconnect.dto;

import com.physioconnect.entity.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentResponse(
        Long appointmentId,
        String referenceNo,
        Long patientId,
        String patientName,
        String patientPhone,
        Long doctorId,
        String doctorName,
        Long locationId,
        String locationName,
        Long serviceId,
        String serviceName,
        Long slotId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String reasonForVisit,
        String medicalHistoryNotes,
        BigDecimal amount,
        LocalDateTime holdExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentResponse from(Appointment appointment, LocalDateTime holdExpiresAt) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getReferenceNo(),
                appointment.getPatient().getId(),
                appointment.getPatient().getUser().getFullName(),
                appointment.getPatient().getUser().getPhone(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getUser().getFullName(),
                appointment.getLocation().getId(),
                appointment.getLocation().getName(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getSlot().getId(),
                appointment.getSlot().getSlotDate(),
                appointment.getSlot().getStartTime(),
                appointment.getSlot().getEndTime(),
                appointment.getStatus().name(),
                appointment.getReasonForVisit(),
                appointment.getMedicalHistoryNotes(),
                appointment.getAmount(),
                holdExpiresAt,
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
