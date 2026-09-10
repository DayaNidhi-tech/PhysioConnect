package com.physioconnect.service;

import com.physioconnect.dto.RegisterRequest;
import com.physioconnect.entity.User;
import com.physioconnect.repository.EmailVerificationTokenRepository;
import com.physioconnect.repository.PatientRepository;
import com.physioconnect.repository.RefreshTokenRepository;
import com.physioconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AuthServiceRegistrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        emailVerificationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerShouldCreateUserAndToken() {
        RegisterRequest request = new RegisterRequest(
                "Test Patient",
                "testpatient@example.com",
                "9876543210",
                "Test1234"
        );

        User user = authService.register(request);

        assertNotNull(user.getId());
    }
}
