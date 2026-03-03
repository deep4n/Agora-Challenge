package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.LoginRequest;
import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.LoginResponse;
import com.agora.booking.management.dto.response.UserResponse;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.EmailAlreadyExistsException;
import com.agora.booking.management.exception.InvalidCredentialsException;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.impl.AuthServiceImpl;
import com.agora.booking.management.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        // Register
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setName("Alice Johnson");
        validRegisterRequest.setEmail("alice@example.com");
        validRegisterRequest.setPassword("Password123!");

        // Login
        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("alice@example.com");
        validLoginRequest.setPassword("Password123!");

        // Saved user
        savedUser = User.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .password("$2a$12$hashedPassword")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();
    }

    // =============================================
    // FR01 — Register Tests (tidak berubah)
    // =============================================
    @Test
    @DisplayName("Should return UserResponse when registration is successful")
    void register_ShouldReturnUserResponse_WhenDataIsValid() {
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.register(validRegisterRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not expose password in UserResponse")
    void register_ShouldNotExposePassword_InResponse() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.register(validRegisterRequest);

        assertThat(response.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("password");
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is duplicate")
    void register_ShouldThrowEmailAlreadyExistsException_WhenEmailIsDuplicate() {
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should hash password before saving to database")
    void register_ShouldHashPassword_BeforeSaving() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(validRegisterRequest);

        verify(passwordEncoder, times(1)).encode("Password123!");
        verify(userRepository).save(argThat(user -> user.getPassword().equals("$2a$12$hashedPassword") &&
                !user.getPassword().equals("Password123!")));
    }

    @Test
    @DisplayName("Should not call passwordEncoder when email already exists")
    void register_ShouldNotEncodePassword_WhenEmailIsDuplicate() {
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(anyString());
    }

    // =============================================
    // FR02 — Login Tests
    // =============================================
    @Test
    @DisplayName("Should return LoginResponse with token when credentials are valid")
    void login_ShouldReturnLoginResponse_WhenCredentialsAreValid() {

        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(validLoginRequest.getEmail()))
                .thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(savedUser.getEmail()))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.mocktoken");

        // Act
        LoginResponse response = authService.login(validLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.mocktoken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is wrong")
    void login_ShouldThrowInvalidCredentialsException_WhenPasswordIsWrong() {

        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        // Pastikan token TIDAK di-generate jika login gagal
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should not expose password in LoginResponse")
    void login_ShouldNotExposePassword_InLoginResponse() {

        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(anyString()))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.mocktoken");

        // Act
        LoginResponse response = authService.login(validLoginRequest);

        // Assert — UserResponse di dalam LoginResponse tidak punya field password
        assertThat(response.getUser().getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("password");
    }

    @Test
    @DisplayName("Should call generateToken with user email after successful authentication")
    void login_ShouldCallGenerateToken_WithUserEmail() {

        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(validLoginRequest.getEmail()))
                .thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(savedUser.getEmail()))
                .thenReturn("eyJhbGciOiJIUzI1NiJ9.mocktoken");

        // Act
        authService.login(validLoginRequest);

        // Assert — generateToken dipanggil tepat 1x dengan email yang benar
        verify(jwtUtil, times(1)).generateToken("alice@example.com");
    }

    // =============================================
    // FR03 — Get My Profile Tests
    // =============================================
    @Test
    @DisplayName("Should return UserResponse when token is valid and user exists")
    void getMyProfile_ShouldReturnUserResponse_WhenUserExists() {

        // Arrange
        when(userRepository.findByEmail(savedUser.getEmail()))
                .thenReturn(Optional.of(savedUser));

        // Act
        UserResponse response = authService.getMyProfile("alice@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void getMyProfile_ShouldThrowResourceNotFoundException_WhenUserNotFound() {

        // Arrange
        when(userRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.getMyProfile("notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("notfound@example.com");

        verify(userRepository, times(1)).findByEmail("notfound@example.com");
    }

    @Test
    @DisplayName("Should not expose password in profile response")
    void getMyProfile_ShouldNotExposePassword_InResponse() {

        // Arrange
        when(userRepository.findByEmail(savedUser.getEmail()))
                .thenReturn(Optional.of(savedUser));

        // Act
        UserResponse response = authService.getMyProfile("alice@example.com");

        // Assert
        assertThat(response.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("password");
    }

    @Test
    @DisplayName("Should call findByEmail once with correct email from token")
    void getMyProfile_ShouldCallFindByEmail_WithCorrectEmail() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(savedUser));

        // Act
        authService.getMyProfile("alice@example.com");

        // Assert — findByEmail dipanggil tepat 1x dengan email dari token
        verify(userRepository, times(1)).findByEmail("alice@example.com");
    }
}