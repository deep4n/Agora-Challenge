package com.agora.booking.management.service.impl;

import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.UserResponse;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.EmailAlreadyExistsException;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        log.debug("Attempting registration for email: {}", request.getEmail());

        // 1. Cek apakah email sudah terdaftar → 409 Conflict
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // 2. Hash password dengan BCrypt — TIDAK PERNAH simpan plain text
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Build dan simpan entity User
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(hashedPassword)
                .build();

        User savedUser = userRepository.save(user);

        log.debug("User registered successfully with id: {}", savedUser.getId());

        // 4. Map ke DTO response — password TIDAK ikut di sini
        return mapToUserResponse(savedUser);
    }

    // =============================================
    // Private helper — mapping Entity ke DTO
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