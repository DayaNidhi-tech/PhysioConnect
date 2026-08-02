package com.physioconnect.repository;

import com.physioconnect.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);

    @Query("select d from Doctor d join d.locations l " +
           "where l.id = :locationId and d.isApproved = true")
    List<Doctor> findByLocationId(Long locationId);
}
