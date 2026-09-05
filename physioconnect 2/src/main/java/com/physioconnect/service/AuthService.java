package com.physioconnect.service;

import com.physioconnect.dto.RegisterRequest;
import com.physioconnect.entity.EmailVerificationToken;
import com.physioconnect.entity.User;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.EmailVerificationTokenRepository;
import com.physioconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Account registration and email verification.
 */
@Service
public class AuthService {

    private static final long TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final String baseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public User register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        String phone = request.phone();

        if (phone != null) {
            phone = phone.trim();

            if (phone.isBlank()) {
                phone = null;
            }
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(
                    "An account with this email already exists"
            );
        }

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BadRequestException(
                    "An account with this phone number already exists"
            );
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
                .role(request.effectiveRole())

                // Account remains inactive until email verification.
                .isActive(false)

                .isEmailVerified(false)
                .build();

        user = userRepository.save(user);

        sendVerificationEmail(user);

        return user;
    }

    @Transactional
    public void sendVerificationEmail(User user) {

        LocalDateTime now = LocalDateTime.now();

        tokenRepository.invalidatePendingForUser(user, now);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .token(generateToken())
                .expiresAt(
                        now.plusHours(TOKEN_TTL_HOURS)
                )
                .build();

        tokenRepository.save(token);

        String link = baseUrl
                + "/api/v1/auth/verify-email?token="
                + token.getToken();

        String body = "Hi " + user.getFullName() + ",\n\n"
                + "Please verify your email address by clicking the link below. "
                + "The link is valid for 24 hours.\n\n"
                + link + "\n\n"
                + "If you did not create a PhysioConnect account, "
                + "you can safely ignore this email.";

        mailService.send(
                user.getEmail(),
                "Verify your PhysioConnect account",
                body
        );
    }

    @Transactional
    public User verifyEmail(String rawToken) {

        EmailVerificationToken token = tokenRepository
                .findByToken(rawToken)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Invalid or expired verification token"
                        )
                );

        if (!token.isValid()) {
            throw new BadRequestException(
                    "This verification link is invalid or has expired. "
                    + "Please request a new one."
            );
        }

        User user = token.getUser();

        // Email verification completes account activation.
        user.setIsEmailVerified(true);
        user.setIsActive(true);

        token.setUsedAt(LocalDateTime.now());

        userRepository.save(user);
        tokenRepository.save(token);

        return user;
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {

        return userRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private String generateToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
