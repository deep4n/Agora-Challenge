package com.agora.booking.management.controller;

import com.agora.booking.management.dto.request.LoginRequest;
import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.ApiResponse;
import com.agora.booking.management.dto.response.LoginResponse;
import com.agora.booking.management.dto.response.UserResponse;
import com.agora.booking.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =============================================
    // POST /api/auth/register — FR01
    // =============================================
    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.debug("POST /api/auth/register - email: {}", request.getEmail());

        UserResponse userResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userResponse));
    }

    // =============================================
    // POST /api/auth/login — FR02
    // =============================================
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.debug("POST /api/auth/login - email: {}", request.getEmail());

        LoginResponse loginResponse = authService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Login successful", loginResponse));
    }

    // =============================================
    // GET /api/users/me — FR03
    // @AuthenticationPrincipal → ambil user dari JWT token
    // TIDAK dari request body atau query param
    // =============================================
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("GET /api/users/me - email: {}", userDetails.getUsername());

        UserResponse userResponse = authService.getMyProfile(userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("User profile retrieved successfully", userResponse));
    }
}