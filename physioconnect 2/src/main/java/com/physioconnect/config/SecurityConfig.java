package com.physioconnect.config;

import com.physioconnect.security.ApiAccessDeniedHandler;
import com.physioconnect.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Server-side session token security.
 *
 * The client authenticates once via POST /api/auth/login; Spring Security then
 * stores the authenticated SecurityContext in the HTTP session keyed by the
 * JSESSIONID cookie (the "session token"). Subsequent requests carry that
 * cookie and the filter chain trusts it without re-sending credentials.
 *
 * CSRF is disabled because this is a stateless JSON API consumed by non-browser
 * clients (mobile/front-end that keeps the token), not a form-based web app.
 * Session fixation is mitigated by migrating the session id on authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Shared repository so the login endpoint can persist the context to the same place the chain reads it. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           RestAuthenticationEntryPoint entryPoint,
                                           ApiAccessDeniedHandler accessDeniedHandler,
                                           SecurityContextRepository securityContextRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // logout handled explicitly in the auth controller
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        // Public: registration + the email verification link (clicked from email while logged out)
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/verify-email", "/error").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sf -> sf.migrateSession()))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
