package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.entity.Event;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.repository.EventRepository;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventServiceImpl Unit Tests")
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private CreateEventRequest validRequest;
    private User creator;
    private Event savedEvent;

    @BeforeEach
    void setUp() {

        // Creator
        creator = User.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .password("$2a$12$hashedPassword")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();

        // Request
        validRequest = new CreateEventRequest();
        validRequest.setTitle("Tech Conference 2025");
        validRequest.setDescription("Annual tech conference");
        validRequest.setLocation("Jakarta Convention Center");
        validRequest.setEventDate(LocalDateTime.of(2025, 6, 15, 9, 0, 0));
        validRequest.setAvailableSeats(200);
        validRequest.setTicketPrice(new BigDecimal("150000.00"));

        // Saved event
        savedEvent = Event.builder()
                .id(10L)
                .title("Tech Conference 2025")
                .description("Annual tech conference")
                .location("Jakarta Convention Center")
                .eventDate(LocalDateTime.of(2025, 6, 15, 9, 0, 0))
                .availableSeats(200)
                .ticketPrice(new BigDecimal("150000.00"))
                .isActive(true)
                .creator(creator)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();
    }

    // =============================================
    // Test 1 — Create event berhasil
    // =============================================
    @Test
    @DisplayName("Should return EventResponse when event is created successfully")
    void createEvent_ShouldReturnEventResponse_WhenDataIsValid() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(creator));
        when(eventRepository.save(any(Event.class)))
                .thenReturn(savedEvent);

        // Act
        EventResponse response = eventService.createEvent(validRequest, "alice@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Tech Conference 2025");
        assertThat(response.getLocation()).isEqualTo("Jakarta Convention Center");
        assertThat(response.getAvailableSeats()).isEqualTo(200);
        assertThat(response.getTicketPrice()).isEqualByComparingTo("150000.00");
        assertThat(response.getIsActive()).isTrue();
    }

    // =============================================
    // Test 2 — Creator diambil dari token
    // =============================================
    @Test
    @DisplayName("Should set creator from JWT token email, not from request body")
    void createEvent_ShouldSetCreator_FromTokenEmail() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(creator));
        when(eventRepository.save(any(Event.class)))
                .thenReturn(savedEvent);

        // Act
        EventResponse response = eventService.createEvent(validRequest, "alice@example.com");

        // Assert
        assertThat(response.getCreator()).isNotNull();
        assertThat(response.getCreator().getId()).isEqualTo(1L);
        assertThat(response.getCreator().getName()).isEqualTo("Alice Johnson");

        // Pastikan findByEmail dipanggil dengan email dari token
        verify(userRepository, times(1)).findByEmail("alice@example.com");
    }

    // =============================================
    // Test 3 — Creator tidak ditemukan di DB
    // =============================================
    @Test
    @DisplayName("Should throw ResourceNotFoundException when creator not found")
    void createEvent_ShouldThrowResourceNotFoundException_WhenCreatorNotFound() {

        // Arrange
        when(userRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> eventService.createEvent(validRequest, "notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("notfound@example.com");

        // Pastikan save TIDAK dipanggil
        verify(eventRepository, never()).save(any(Event.class));
    }

    // =============================================
    // Test 4 — Event gratis (ticketPrice = 0)
    // =============================================
    @Test
    @DisplayName("Should allow ticketPrice zero for free events")
    void createEvent_ShouldAllowZeroTicketPrice_ForFreeEvents() {

        // Arrange
        validRequest.setTicketPrice(BigDecimal.ZERO);

        Event freeEvent = Event.builder()
                .id(11L)
                .title("Free Workshop")
                .description("Free coding workshop")
                .location("Online")
                .eventDate(LocalDateTime.of(2025, 7, 1, 9, 0, 0))
                .availableSeats(50)
                .ticketPrice(BigDecimal.ZERO)
                .isActive(true)
                .creator(creator)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(creator));
        when(eventRepository.save(any(Event.class)))
                .thenReturn(freeEvent);

        // Act
        EventResponse response = eventService.createEvent(validRequest, "alice@example.com");

        // Assert
        assertThat(response.getTicketPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =============================================
    // Test 5 — isActive default true saat dibuat
    // =============================================
    @Test
    @DisplayName("Should set isActive to true when event is created")
    void createEvent_ShouldSetIsActive_ToTrueByDefault() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(creator));
        when(eventRepository.save(any(Event.class)))
                .thenReturn(savedEvent);

        // Act
        EventResponse response = eventService.createEvent(validRequest, "alice@example.com");

        // Assert
        assertThat(response.getIsActive()).isTrue();

        // Pastikan event yang disave memiliki isActive = true
        verify(eventRepository).save(argThat(event -> event.getIsActive().equals(true)));
    }
}