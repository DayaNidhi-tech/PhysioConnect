package com.physioconnect.repository;

import com.physioconnect.entity.TimeSlot;
import com.physioconnect.entity.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDoctorIdAndSlotDateAndStatus(Long doctorId, LocalDate slotDate, SlotStatus status);

    // Pessimistic lock as a belt-and-braces option alongside @Version
    // optimistic locking, used when placing a hold on a slot.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TimeSlot t where t.id = :id")
    Optional<TimeSlot> findByIdForUpdate(Long id);
}
