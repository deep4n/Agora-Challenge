package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.request.RegisterRequest;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.dto.response.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    // FR04 — Create Event Tests
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
        verify(userRepository, times(1)).findByEmail("alice@example.com");
    }

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

        verify(eventRepository, never()).save(any(Event.class));
    }

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
        verify(eventRepository).save(argThat(event -> event.getIsActive().equals(true)));
    }

    // =============================================
    // FR05 + FR09 — List & Search Events Tests
    // =============================================

    @Test
    @DisplayName("Should return paginated events when no filter applied")
    void listEvents_ShouldReturnPagedEvents_WhenNoFilterApplied() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(savedEvent), pageable, 1);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(eventPage);

        // Act
        PageResponse<EventResponse> response = eventService.listEvents(
                0, 10, null, null, null, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should return filtered events when title filter applied")
    void listEvents_ShouldReturnFilteredEvents_WhenTitleFilterApplied() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(savedEvent), pageable, 1);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(eventPage);

        // Act
        PageResponse<EventResponse> response = eventService.listEvents(
                0, 10, "tech", null, null, null);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle())
                .containsIgnoringCase("tech");
    }

    @Test
    @DisplayName("Should return filtered events when location filter applied")
    void listEvents_ShouldReturnFilteredEvents_WhenLocationFilterApplied() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(savedEvent), pageable, 1);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(eventPage);

        // Act
        PageResponse<EventResponse> response = eventService.listEvents(
                0, 10, null, "jakarta", null, null);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getLocation())
                .containsIgnoringCase("jakarta");
    }

    @Test
    @DisplayName("Should return empty page when no events match filter")
    void listEvents_ShouldReturnEmptyPage_WhenNoEventsMatchFilter() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PageResponse<EventResponse> response = eventService.listEvents(
                0, 10, "nonexistent", null, null, null);

        // Assert
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should return filtered events when date range filter applied")
    void listEvents_ShouldReturnFilteredEvents_WhenDateRangeFilterApplied() {

        // Arrange
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(savedEvent), pageable, 1);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(eventPage);

        // Act
        PageResponse<EventResponse> response = eventService.listEvents(
                0, 10, null, null, startDate, endDate);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getEventDate())
                .isBetween(startDate, endDate);
    }
}