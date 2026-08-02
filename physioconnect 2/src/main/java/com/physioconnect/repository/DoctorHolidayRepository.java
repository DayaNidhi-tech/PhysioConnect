package com.physioconnect.repository;

import com.physioconnect.entity.DoctorHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DoctorHolidayRepository extends JpaRepository<DoctorHoliday, Long> {
    List<DoctorHoliday> findByDoctorIdAndHolidayDate(Long doctorId, LocalDate date);
}
