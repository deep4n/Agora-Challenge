package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.UserResponse;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.EmailAlreadyExistsException;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setName("Alice Johnson");
        validRequest.setEmail("alice@example.com");
        validRequest.setPassword("Password123!");

        savedUser = User.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .password("$2a$12$hashedPassword")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();
    }

    // =============================================
    // Test 1 — Register berhasil
    // =============================================
    @Test
    @DisplayName("Should return UserResponse when registration is successful")
    void register_ShouldReturnUserResponse_WhenDataIsValid() {

        // Arrange
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = authService.register(validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    // =============================================
    // Test 2 — Password tidak muncul di response
    // =============================================
    @Test
    @DisplayName("Should not expose password in UserResponse")
    void register_ShouldNotExposePassword_InResponse() {

        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = authService.register(validRequest);

        // Assert — UserResponse tidak punya field password
        assertThat(response.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("password");
    }

    // =============================================
    // Test 3 — Email duplikat → 409 Conflict
    // =============================================
    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is duplicate")
    void register_ShouldThrowEmailAlreadyExistsException_WhenEmailIsDuplicate() {

        // Arrange
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        // Pastikan save TIDAK dipanggil jika email duplikat
        verify(userRepository, never()).save(any(User.class));
    }

    // =============================================
    // Test 4 — Password di-hash sebelum disimpan
    // =============================================
    @Test
    @DisplayName("Should hash password before saving to database")
    void register_ShouldHashPassword_BeforeSaving() {

        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.register(validRequest);

        // Assert — encode dipanggil tepat 1x dengan plain text password
        verify(passwordEncoder, times(1)).encode("Password123!");

        // Pastikan yang disimpan ke DB adalah hash, bukan plain text
        verify(userRepository).save(argThat(user -> user.getPassword().equals("$2a$12$hashedPassword") &&
                !user.getPassword().equals("Password123!")));
    }

    // =============================================
    // Test 5 — Email duplikat tidak panggil encoder
    // =============================================
    @Test
    @DisplayName("Should not call passwordEncoder when email already exists")
    void register_ShouldNotEncodePassword_WhenEmailIsDuplicate() {

        // Arrange
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        // Pastikan encoder TIDAK dipanggil sama sekali
        verify(passwordEncoder, never()).encode(anyString());
    }
}