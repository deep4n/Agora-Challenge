package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.CreateBookingRequest;
import com.agora.booking.management.dto.response.BookingResponse;
import com.agora.booking.management.entity.Booking;
import com.agora.booking.management.entity.Event;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.DuplicateBookingException;
import com.agora.booking.management.exception.EventNotAvailableException;
import com.agora.booking.management.exception.InsufficientSeatsException;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.exception.BookingCancellationException;
import com.agora.booking.management.model.BookingStatus;
import com.agora.booking.management.repository.BookingRepository;
import com.agora.booking.management.repository.EventRepository;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.impl.BookingServiceImpl;
import com.agora.booking.management.util.ReferenceNumberGenerator;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl Unit Tests")
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReferenceNumberGenerator referenceNumberGenerator;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Event event;
    private Booking savedBooking;
    private CreateBookingRequest validRequest;

    @BeforeEach
    void setUp() {

        // User
        user = User.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .password("$2a$12$hashedPassword")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();

        // Event
        event = Event.builder()
                .id(10L)
                .title("Tech Conference 2025")
                .description("Annual tech conference")
                .location("Jakarta Convention Center")
                .eventDate(LocalDateTime.now().plusDays(30))
                .availableSeats(200)
                .ticketPrice(new BigDecimal("150000.00"))
                .isActive(true)
                .creator(user)
                .version(0L)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0))
                .build();

        // Request
        validRequest = new CreateBookingRequest();
        validRequest.setEventId(10L);
        validRequest.setNumTickets(2);

        // Saved booking
        savedBooking = Booking.builder()
                .id(55L)
                .referenceNumber("BK-20250101-483921")
                .user(user)
                .event(event)
                .numTickets(2)
                .totalPrice(new BigDecimal("300000.00"))
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 1, 1, 11, 0, 0))
                .build();
    }

    // =============================================
    // Test 1 — Booking berhasil
    // =============================================
    @Test
    @DisplayName("Should return BookingResponse when booking is successful")
    void createBooking_ShouldReturnBookingResponse_WhenDataIsValid() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventId(1L, 10L))
                .thenReturn(false);
        when(referenceNumberGenerator.generate())
                .thenReturn("BK-20250101-483921");
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        // Act
        BookingResponse response = bookingService.createBooking(
                validRequest, "alice@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getReferenceNumber()).isEqualTo("BK-20250101-483921");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.ACTIVE);
        assertThat(response.getNumTickets()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("300000.00");
        assertThat(response.getEvent()).isNotNull();
        assertThat(response.getEvent().getTitle()).isEqualTo("Tech Conference 2025");
    }

    // =============================================
    // Test 2 — Total price = numTickets x ticketPrice
    // =============================================
    @Test
    @DisplayName("Should calculate totalPrice correctly as numTickets x ticketPrice")
    void createBooking_ShouldCalculateTotalPrice_Correctly() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventId(1L, 10L))
                .thenReturn(false);
        when(referenceNumberGenerator.generate())
                .thenReturn("BK-20250101-483921");
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        // Act
        bookingService.createBooking(validRequest, "alice@example.com");

        // Assert — verify save dipanggil dengan totalPrice yang benar
        verify(bookingRepository).save(argThat(booking -> booking.getTotalPrice().compareTo(
                new BigDecimal("300000.00")) == 0));
    }

    // =============================================
    // Test 3 — Available seats dikurangi setelah booking
    // =============================================
    @Test
    @DisplayName("Should decrement availableSeats after successful booking")
    void createBooking_ShouldDecrementAvailableSeats_AfterBooking() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventId(1L, 10L))
                .thenReturn(false);
        when(referenceNumberGenerator.generate())
                .thenReturn("BK-20250101-483921");
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        // Act
        bookingService.createBooking(validRequest, "alice@example.com");

        // Assert — event di-save dengan availableSeats berkurang 2
        verify(eventRepository).save(argThat(e -> e.getAvailableSeats().equals(198) // 200 - 2
        ));
    }

    // =============================================
    // Test 4 — Event tidak ditemukan → 404
    // =============================================
    @Test
    @DisplayName("Should throw ResourceNotFoundException when event not found")
    void createBooking_ShouldThrowResourceNotFoundException_WhenEventNotFound() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(99999L))
                .thenReturn(Optional.empty());

        validRequest.setEventId(99999L);

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(validRequest, "alice@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99999");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // =============================================
    // Test 5 — Event tidak aktif → 400
    // =============================================
    @Test
    @DisplayName("Should throw EventNotAvailableException when event is inactive")
    void createBooking_ShouldThrowEventNotAvailableException_WhenEventIsInactive() {

        // Arrange
        event.setIsActive(false);

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(validRequest, "alice@example.com"))
                .isInstanceOf(EventNotAvailableException.class)
                .hasMessage("Event is no longer active");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // =============================================
    // Test 6 — Event sudah lewat → 400
    // =============================================
    @Test
    @DisplayName("Should throw EventNotAvailableException when event is in the past")
    void createBooking_ShouldThrowEventNotAvailableException_WhenEventIsPast() {

        // Arrange
        event.setEventDate(LocalDateTime.now().minusDays(1));

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(validRequest, "alice@example.com"))
                .isInstanceOf(EventNotAvailableException.class)
                .hasMessage("Cannot book tickets for a past event");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // =============================================
    // Test 7 — Double booking → 409 (FR14)
    // =============================================
    @Test
    @DisplayName("Should throw DuplicateBookingException when user already booked")
    void createBooking_ShouldThrowDuplicateBookingException_WhenAlreadyBooked() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventId(1L, 10L))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(validRequest, "alice@example.com"))
                .isInstanceOf(DuplicateBookingException.class)
                .hasMessage("You have already booked this event");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // =============================================
    // Test 8 — Kursi tidak cukup → 400
    // =============================================
    @Test
    @DisplayName("Should throw InsufficientSeatsException when not enough seats")
    void createBooking_ShouldThrowInsufficientSeatsException_WhenNotEnoughSeats() {

        // Arrange
        event.setAvailableSeats(1); // hanya 1 kursi tersisa
        validRequest.setNumTickets(2); // tapi request 2 tiket

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventId(1L, 10L))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(validRequest, "alice@example.com"))
                .isInstanceOf(InsufficientSeatsException.class)
                .hasMessageContaining("Requested: 2, Available: 1");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // =============================================
    // Test 9 — Reference number di-generate (FR13)
    // =============================================
    @Test
    @DisplayName("Should generate reference number for successful booking")
    void createBooking_ShouldGenerateReferenceNumber_ForSuccessfulBooking() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.existsByUserIdAndEventId(1L, 10L))
                .thenReturn(false);
        when(referenceNumberGenerator.generate())
                .thenReturn("BK-20250101-483921");
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        // Act
        BookingResponse response = bookingService.createBooking(
                validRequest, "alice@example.com");

        // Assert
        verify(referenceNumberGenerator, times(1)).generate();
        assertThat(response.getReferenceNumber()).startsWith("BK-");
    }

    // =============================================
    // FR11 — Get My Bookings Tests
    // =============================================

    @Test
    @DisplayName("Should return list of bookings when user has bookings")
    void getMyBookings_ShouldReturnListOfBookings_WhenUserHasBookings() {

        // Arrange
        Booking secondBooking = Booking.builder()
                .id(56L)
                .referenceNumber("BK-20250102-123456")
                .user(user)
                .event(event)
                .numTickets(1)
                .totalPrice(new BigDecimal("150000.00"))
                .status(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 1, 2, 11, 0, 0))
                .build();

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(savedBooking, secondBooking));

        // Act
        List<BookingResponse> response = bookingService.getMyBookings("alice@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getReferenceNumber()).isEqualTo("BK-20250101-483921");
        assertThat(response.get(1).getReferenceNumber()).isEqualTo("BK-20250102-123456");
    }

    @Test
    @DisplayName("Should return empty list when user has no bookings")
    void getMyBookings_ShouldReturnEmptyList_WhenUserHasNoBookings() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        // Act
        List<BookingResponse> response = bookingService.getMyBookings("alice@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void getMyBookings_ShouldThrowResourceNotFoundException_WhenUserNotFound() {

        // Arrange
        when(userRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.getMyBookings("notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("notfound@example.com");

        verify(bookingRepository, never())
                .findByUserIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    @DisplayName("Should call findByUserIdOrderByCreatedAtDesc with correct user id")
    void getMyBookings_ShouldCallRepository_WithCorrectUserId() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(savedBooking));

        // Act
        bookingService.getMyBookings("alice@example.com");

        // Assert
        verify(bookingRepository, times(1))
                .findByUserIdOrderByCreatedAtDesc(1L);
    }

    // =============================================
    // FR12 — Cancel Booking Tests
    // =============================================

    @Test
    @DisplayName("Should return cancelled BookingResponse when cancellation is successful")
    void cancelBooking_ShouldReturnCancelledBookingResponse_WhenSuccessful() {

        // Arrange
        Booking cancelledBooking = Booking.builder()
                .id(55L)
                .referenceNumber("BK-20250101-483921")
                .user(user)
                .event(event)
                .numTickets(2)
                .totalPrice(new BigDecimal("300000.00"))
                .status(BookingStatus.CANCELLED)
                .createdAt(LocalDateTime.of(2025, 1, 1, 11, 0, 0))
                .build();

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByIdAndUserId(55L, 1L))
                .thenReturn(Optional.of(savedBooking));
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(cancelledBooking);

        // Act
        BookingResponse response = bookingService.cancelBooking(55L, "alice@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.getReferenceNumber()).isEqualTo("BK-20250101-483921");
    }

    @Test
    @DisplayName("Should restore availableSeats after cancellation")
    void cancelBooking_ShouldRestoreAvailableSeats_AfterCancellation() {

        // Arrange — event dengan 198 seats (sudah dikurangi 2 saat booking)
        event.setAvailableSeats(198);

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByIdAndUserId(55L, 1L))
                .thenReturn(Optional.of(savedBooking));
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        // Act
        bookingService.cancelBooking(55L, "alice@example.com");

        // Assert — event di-save dengan availableSeats dikembalikan 2
        verify(eventRepository).save(argThat(e -> e.getAvailableSeats().equals(200) // 198 + 2
        ));
    }

    @Test
    @DisplayName("Should throw BookingCancellationException when booking is already cancelled")
    void cancelBooking_ShouldThrowBookingCancellationException_WhenAlreadyCancelled() {

        // Arrange
        savedBooking.setStatus(BookingStatus.CANCELLED);

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByIdAndUserId(55L, 1L))
                .thenReturn(Optional.of(savedBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(55L, "alice@example.com"))
                .isInstanceOf(BookingCancellationException.class)
                .hasMessage("Booking is already cancelled");

        verify(eventRepository, never()).save(any(Event.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BookingCancellationException when cancellation deadline passed")
    void cancelBooking_ShouldThrowBookingCancellationException_WhenDeadlinePassed() {

        // Arrange — eventDate kurang dari 24 jam dari sekarang
        event.setEventDate(LocalDateTime.now().plusHours(12));

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByIdAndUserId(55L, 1L))
                .thenReturn(Optional.of(savedBooking));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(55L, "alice@example.com"))
                .isInstanceOf(BookingCancellationException.class)
                .hasMessage("Cancellation is only allowed up to 24 hours before the event");

        verify(eventRepository, never()).save(any(Event.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking not found")
    void cancelBooking_ShouldThrowResourceNotFoundException_WhenBookingNotFound() {

        // Arrange
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(bookingRepository.findByIdAndUserId(99999L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(99999L, "alice@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99999");

        verify(eventRepository, never()).save(any(Event.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking does not belong to user")
    void cancelBooking_ShouldThrowResourceNotFoundException_WhenBookingNotOwnedByUser() {

        // Arrange — findByIdAndUserId return empty karena bukan milik user
        when(userRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(2L)
                        .name("Bob Smith")
                        .email("other@example.com")
                        .password("$2a$12$hashedPassword")
                        .build()));
        when(bookingRepository.findByIdAndUserId(55L, 2L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(55L, "other@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("55");

        verify(eventRepository, never()).save(any(Event.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}