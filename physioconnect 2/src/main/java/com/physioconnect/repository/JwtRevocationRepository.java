package com.physioconnect.repository;

import com.physioconnect.entity.JwtRevocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface JwtRevocationRepository
        extends JpaRepository<JwtRevocation, Long> {

    boolean existsByJti(String jti);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}