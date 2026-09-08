package com.physioconnect.repository;

import com.physioconnect.entity.Appointment;
import com.physioconnect.entity.enums.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByReferenceNo(String referenceNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(Long id);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
    Page<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status, Pageable pageable);
    Page<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable);

    List<Appointment> findByPatientIdAndDoctorIdAndStatusIn(
            Long patientId,
            Long doctorId,
            Collection<AppointmentStatus> statuses
    );
}
