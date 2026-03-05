package com.agora.booking.management.service.impl;

import com.agora.booking.management.dto.request.CreateBookingRequest;
import com.agora.booking.management.dto.response.BookingEventResponse;
import com.agora.booking.management.dto.response.BookingResponse;
import com.agora.booking.management.entity.Booking;
import com.agora.booking.management.entity.Event;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.DuplicateBookingException;
import com.agora.booking.management.exception.EventNotAvailableException;
import com.agora.booking.management.exception.InsufficientSeatsException;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.model.BookingStatus;
import com.agora.booking.management.repository.BookingRepository;
import com.agora.booking.management.repository.EventRepository;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.BookingService;
import com.agora.booking.management.util.ReferenceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;

    // =============================================
    // FR10 + FR13 + FR14 — Create Booking
    // =============================================
    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String userEmail) {

        log.debug("Creating booking for event id: {} by user: {}",
                request.getEventId(), userEmail);

        // 1. Ambil user dari token
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        // 2. Ambil event — findById karena kita perlu cek kondisi manual
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event", request.getEventId()));

        // 3. Validasi event masih aktif (FR10)
        if (!event.getIsActive()) {
            throw new EventNotAvailableException("Event is no longer active");
        }

        // 4. Validasi event belum lewat (FR10)
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new EventNotAvailableException("Cannot book tickets for a past event");
        }

        // 5. FR14 — cek double booking
        if (bookingRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new DuplicateBookingException();
        }

        // 6. Validasi ketersediaan kursi (FR10)
        if (event.getAvailableSeats() < request.getNumTickets()) {
            throw new InsufficientSeatsException(
                    request.getNumTickets(),
                    event.getAvailableSeats());
        }

        // 7. Kurangi available seats — @Version akan handle concurrent request
        event.setAvailableSeats(event.getAvailableSeats() - request.getNumTickets());
        eventRepository.save(event);

        // 8. Hitung total price (FR13)
        BigDecimal totalPrice = event.getTicketPrice()
                .multiply(BigDecimal.valueOf(request.getNumTickets()));

        // 9. Generate reference number (FR13)
        String referenceNumber = referenceNumberGenerator.generate();

        // 10. Buat dan simpan booking
        Booking booking = Booking.builder()
                .referenceNumber(referenceNumber)
                .user(user)
                .event(event)
                .numTickets(request.getNumTickets())
                .totalPrice(totalPrice)
                .status(BookingStatus.ACTIVE)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        log.debug("Booking created successfully - ref: {}, user: {}, event: {}",
                referenceNumber, userEmail, event.getId());

        return mapToBookingResponse(savedBooking);
    }

    // =============================================
    // Private helper — mapping Entity ke DTO
    // =============================================
    public BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .referenceNumber(booking.getReferenceNumber())
                .status(booking.getStatus())
                .numTickets(booking.getNumTickets())
                .totalPrice(booking.getTotalPrice())
                .event(BookingEventResponse.builder()
                        .id(booking.getEvent().getId())
                        .title(booking.getEvent().getTitle())
                        .location(booking.getEvent().getLocation())
                        .eventDate(booking.getEvent().getEventDate())
                        .ticketPrice(booking.getEvent().getTicketPrice())
                        .build())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}