package com.physioconnect.repository;

import com.physioconnect.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByIsActiveTrue();
}
