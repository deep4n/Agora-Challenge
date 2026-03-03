package com.agora.booking.management.service.impl;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.response.CreatorResponse;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.entity.Event;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.repository.EventRepository;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // =============================================
    // FR04 — Create Event
    // creatorEmail diambil dari JWT token
    // =============================================
    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, String creatorEmail) {

        log.debug("Creating event '{}' by user: {}", request.getTitle(), creatorEmail);

        // 1. Ambil data creator dari DB berdasarkan email dari token
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorEmail));

        // 2. Build entity Event
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .availableSeats(request.getAvailableSeats())
                .ticketPrice(request.getTicketPrice())
                .isActive(true)
                .creator(creator)
                .build();

        // 3. Simpan ke DB
        Event savedEvent = eventRepository.save(event);

        log.debug("Event created successfully with id: {}", savedEvent.getId());

        // 4. Map ke DTO response
        return mapToEventResponse(savedEvent);
    }

    // =============================================
    // Private helper — mapping Entity ke DTO
    // =============================================
    public EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .availableSeats(event.getAvailableSeats())
                .ticketPrice(event.getTicketPrice())
                .isActive(event.getIsActive())
                .creator(CreatorResponse.builder()
                        .id(event.getCreator().getId())
                        .name(event.getCreator().getName())
                        .build())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}