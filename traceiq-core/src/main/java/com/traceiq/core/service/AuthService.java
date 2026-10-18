package com.traceiq.core.service;

import com.traceiq.core.config.JwtService;
import com.traceiq.core.exception.BadRequestException;
import com.traceiq.core.model.dto.AuthResponse;
import com.traceiq.core.model.dto.LoginRequest;
import com.traceiq.core.model.dto.RegisterRequest;
import com.traceiq.core.model.entity.Role;
import com.traceiq.core.model.entity.User;
import com.traceiq.core.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${traceiq.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @PostConstruct
    public void seedDefaultAdminUser() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@traceiq.internal")
                    .password(passwordEncoder.encode("Admin@12345"))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default admin user: 'admin' (password: 'Admin@12345')");

            User developer = User.builder()
                    .username("developer")
                    .email("developer@traceiq.internal")
                    .password(passwordEncoder.encode("Dev@12345"))
                    .role(Role.ROLE_DEVELOPER)
                    .enabled(true)
                    .build();
            userRepository.save(developer);
            log.info("Initialized default developer user: 'developer' (password: 'Dev@12345')");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        Role userRole = request.getRole() != null ? request.getRole() : Role.ROLE_DEVELOPER;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .enabled(true)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInMs(jwtExpirationMs)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInMs(jwtExpirationMs)
                .build();
    }
}
