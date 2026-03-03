package com.agora.booking.management.service.impl;

import com.agora.booking.management.dto.request.LoginRequest;
import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.LoginResponse;
import com.agora.booking.management.dto.response.UserResponse;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.EmailAlreadyExistsException;
import com.agora.booking.management.exception.InvalidCredentialsException;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.AuthService;
import com.agora.booking.management.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // =============================================
    // FR01 — Register (tidak berubah)
    // =============================================
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        log.debug("Attempting registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(hashedPassword)
                .build();

        User savedUser = userRepository.save(user);

        log.debug("User registered successfully with id: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    // =============================================
    // FR02 — Login
    // =============================================
    @Override
    public LoginResponse login(LoginRequest request) {

        log.debug("Attempting login for email: {}", request.getEmail());

        try {
            // 1. Autentikasi email & password via Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (BadCredentialsException e) {
            // 2. Kredensial salah → pesan generik (cegah user enumeration)
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        // 3. Kredensial valid → ambil data user dari DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // 4. Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        log.debug("Login successful for user id: {}", user.getId());

        // 5. Return response dengan token + data user
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }

    // =============================================
    // FR03 — Get My Profile
    // email diambil dari JWT token, bukan request body
    // =============================================
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {

        log.debug("Fetching profile for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        return mapToUserResponse(user);
    }

    // =============================================
    // Private helper
    // =============================================
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}