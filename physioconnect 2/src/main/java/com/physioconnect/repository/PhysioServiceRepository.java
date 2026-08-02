package com.physioconnect.repository;

import com.physioconnect.entity.PhysioService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhysioServiceRepository extends JpaRepository<PhysioService, Long> {
    List<PhysioService> findByIsActiveTrue();
}
