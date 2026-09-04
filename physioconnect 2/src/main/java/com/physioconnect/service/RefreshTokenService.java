package com.physioconnect.service;

import com.physioconnect.entity.RefreshToken;
import com.physioconnect.entity.User;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenExpiration;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-expiration}") Duration refreshTokenExpiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(generateToken())
                .expiresAt(now.plus(refreshTokenExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() ->
                        new BadRequestException("Invalid or expired refresh token"));

        if (!refreshToken.isValid()) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        return refreshToken;
    }

    @Transactional
    public RefreshToken rotate(String rawToken) {
        RefreshToken currentToken = validate(rawToken);

        currentToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(currentToken);

        return createRefreshToken(currentToken.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(rawToken)
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}