package com.physioconnect.repository;

import com.physioconnect.entity.Appointment;
import com.physioconnect.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByReferenceNo(String referenceNo);
    Page<Appointment> findByPatientId(Pageable pageable, Long patientId);
    Page<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status, Pageable pageable);
    Page<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable);
    List<Appointment> findByPatientIdAndDoctorIdAndStatusIn(
            Long patientId,
            Long doctorId,
            Collection<AppointmentStatus> statuses
    );
}
