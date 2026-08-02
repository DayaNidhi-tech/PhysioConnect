package com.physioconnect.service;

import com.physioconnect.dto.RegisterRequest;
import com.physioconnect.entity.EmailVerificationToken;
import com.physioconnect.entity.User;
import com.physioconnect.entity.enums.Role;
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
 * Account creation + email verification.
 *
 * Each registration issues a fresh, single-use, time-limited token. The
 * verification flow runs in one transaction: marking the user verified and
 * consuming the token either both succeed or both roll back together.
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

    public AuthService(UserRepository userRepository,
                       EmailVerificationTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService,
                       @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        String phone = request.phone().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("An account with this phone number already exists");
        }

        Role role = request.effectiveRole();
        if (role == Role.ADMIN) {
            throw new BadRequestException("ADMIN accounts cannot be self-registered");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .isActive(true)
                .isEmailVerified(false)
                .build();
        user = userRepository.save(user);

        // Issue the verification token + send the mail. Token/send order is
        // atomic with the account insert via the enclosing transaction.
        sendVerificationEmail(user);
        return user;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        tokenRepository.invalidatePendingForUser(user, LocalDateTime.now());

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .token(generateToken())
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_TTL_HOURS))
                .build();
        tokenRepository.save(token);

        String link = baseUrl + "/api/auth/verify-email?token=" + token.getToken();
        String body = "Hi " + user.getFullName() + ",\n\n"
                + "Please verify your email address by clicking the link below. "
                + "The link is valid for 24 hours.\n\n"
                + link + "\n\n"
                + "If you did not create a PhysioConnect account, you can safely ignore this email.";
        mailService.send(user.getEmail(), "Verify your PhysioConnect account", body);
    }

    @Transactional
    public User verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (!token.isValid()) {
            throw new BadRequestException("This verification link is invalid or has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setIsEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());

        // Both writes happen in one transaction.
        userRepository.save(user);
        tokenRepository.save(token);
        return user;
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
