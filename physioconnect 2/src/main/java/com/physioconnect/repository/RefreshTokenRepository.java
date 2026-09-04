package com.physioconnect.repository;

import com.physioconnect.entity.RefreshToken;
import com.physioconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("""
            UPDATE RefreshToken r
            SET r.revokedAt = :revokedAt
            WHERE r.user = :user
              AND r.revokedAt IS NULL
            """)
    int revokeAllForUser(
            @Param("user") User user,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
