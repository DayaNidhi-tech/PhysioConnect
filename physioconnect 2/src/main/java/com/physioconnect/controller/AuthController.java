package com.physioconnect.controller;

import com.physioconnect.dto.ApiResponse;
import com.physioconnect.dto.AuthResponse;
import com.physioconnect.dto.LoginRequest;
import com.physioconnect.dto.RegisterRequest;
import com.physioconnect.dto.RegisterResponse;
import com.physioconnect.dto.TokenResponse;
import com.physioconnect.entity.RefreshToken;
import com.physioconnect.entity.User;
import com.physioconnect.security.JwtService;
import com.physioconnect.security.UserPrincipal;
import com.physioconnect.service.AuthService;
import com.physioconnect.service.JwtRevocationService;
import com.physioconnect.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE =
            "physioconnect_refresh_token";

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtRevocationService jwtRevocationService;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            JwtRevocationService jwtRevocationService
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.jwtRevocationService = jwtRevocationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request);

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                null,
                false
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse.Profile>> verifyEmail(
            @RequestParam("token") String token
    ) {
        User user = authService.verifyEmail(token);

        AuthResponse.Profile profile = new AuthResponse.Profile(
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );

        return ResponseEntity.ok(
                ApiResponse.success(profile)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        String email = request.email().trim().toLowerCase();

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.password()
                        )
                );

        User user = authService.getByEmail(email);

        String accessToken =
                jwtService.generateAccessToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        addRefreshTokenCookie(
                response,
                refreshToken.getToken()
        );

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                user.getRole().name(),
                new AuthResponse.Profile(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail()
                )
        );

        return ResponseEntity.ok(
                ApiResponse.success(authResponse)
        );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            )
            String rawRefreshToken,
            HttpServletResponse response
    ) {
        RefreshToken newRefreshToken =
                refreshTokenService.rotate(rawRefreshToken);

        User user = newRefreshToken.getUser();

        String accessToken =
                jwtService.generateAccessToken(user);

        addRefreshTokenCookie(
                response,
                newRefreshToken.getToken()
        );

        TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                user.getRole().name(),
                new AuthResponse.Profile(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail()
                )
        );

        return ResponseEntity.ok(
                ApiResponse.success(tokenResponse)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.Profile>> me(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user =
                authService.getByEmail(principal.getUsername());

        AuthResponse.Profile profile =
                new AuthResponse.Profile(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail()
                );

        return ResponseEntity.ok(
                ApiResponse.success(profile)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            )
            String rawRefreshToken,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader,

            HttpServletResponse response
    ) {
        refreshTokenService.revoke(rawRefreshToken);

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            String accessToken =
                    authorizationHeader.substring(7).trim();

            if (!accessToken.isEmpty()) {
                try {
                    String jti =
                            jwtService.extractJti(accessToken);

                    var expiration =
                            jwtService.extractExpiration(accessToken);

                    jwtRevocationService.revoke(
                            jti,
                            expiration
                    );

                } catch (RuntimeException ignored) {
                    // Invalid access tokens are already unusable.
                }
            }
        }

        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    private void addRefreshTokenCookie(
            HttpServletResponse response,
            String token
    ) {
        ResponseCookie cookie = ResponseCookie.from(
                        REFRESH_TOKEN_COOKIE,
                        token
                )
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    private void clearRefreshTokenCookie(
            HttpServletResponse response
    ) {
        ResponseCookie cookie = ResponseCookie.from(
                        REFRESH_TOKEN_COOKIE,
                        ""
                )
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
}
