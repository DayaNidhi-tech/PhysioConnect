package com.physioconnect.repository;

import com.physioconnect.entity.EmailVerificationToken;
import com.physioconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Invalidates any still-unused tokens for a user before issuing a new one,
     * so only the latest token can ever be redeemed.
     */
    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now " +
           "where t.user = :user and t.usedAt is null")
    void invalidatePendingForUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
