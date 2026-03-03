package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.LoginRequest;
import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.LoginResponse;
import com.agora.booking.management.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse getMyProfile(String email);
}