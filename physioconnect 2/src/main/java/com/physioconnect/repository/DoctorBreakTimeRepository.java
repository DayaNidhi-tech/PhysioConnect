package com.physioconnect.repository;

import com.physioconnect.entity.DoctorBreakTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorBreakTimeRepository extends JpaRepository<DoctorBreakTime, Long> {
}
