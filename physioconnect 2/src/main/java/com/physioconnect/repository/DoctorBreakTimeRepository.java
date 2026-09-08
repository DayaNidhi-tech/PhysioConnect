package com.physioconnect.repository;

import com.physioconnect.entity.DoctorBreakTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorBreakTimeRepository extends JpaRepository<DoctorBreakTime, Long> {
    List<DoctorBreakTime> findByDoctorId(Long doctorId);
    void deleteByDoctorId(Long doctorId);
}
