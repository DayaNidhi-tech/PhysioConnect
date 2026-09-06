package com.physioconnect.service;

import com.physioconnect.entity.JwtRevocation;
import com.physioconnect.repository.JwtRevocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class JwtRevocationService {

    private final JwtRevocationRepository repository;

    public JwtRevocationService(JwtRevocationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(String jti, Instant expiration) {
        if (jti == null || jti.isBlank() || expiration == null) {
            return;
        }

        if (repository.existsByJti(jti)) {
            return;
        }

        repository.save(
                JwtRevocation.builder()
                        .jti(jti)
                        .expiresAt(
                                LocalDateTime.ofInstant(
                                        expiration,
                                        ZoneOffset.UTC
                                )
                        )
                        .revokedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return true;
        }

        return repository.existsByJti(jti);
    }

    @Transactional
    public long removeExpired() {
        return repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
