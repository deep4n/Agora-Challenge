package com.agora.booking.management.service.impl;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.response.CreatorResponse;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.dto.response.PageResponse;
import com.agora.booking.management.entity.Event;
import com.agora.booking.management.entity.User;
import com.agora.booking.management.exception.ResourceNotFoundException;
import com.agora.booking.management.repository.EventRepository;
import com.agora.booking.management.repository.EventSpecification;
import com.agora.booking.management.repository.UserRepository;
import com.agora.booking.management.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // =============================================
    // FR04 — Create Event
    // =============================================
    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, String creatorEmail) {

        log.debug("Creating event '{}' by user: {}", request.getTitle(), creatorEmail);

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorEmail));

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

        Event savedEvent = eventRepository.save(event);

        log.debug("Event created successfully with id: {}", savedEvent.getId());

        return mapToEventResponse(savedEvent);
    }

    // =============================================
    // FR05 + FR09 — List & Search Upcoming Events
    // =============================================
    @Override
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> listEvents(
            int page,
            int size,
            String title,
            String location,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.debug("Listing events - page: {}, size: {}, title: {}, location: {}, " +
                "startDate: {}, endDate: {}", page, size, title, location, startDate, endDate);

        // 1. Buat Pageable dengan urutan eventDate ascending
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());

        // 2. Gunakan Specification untuk query dinamis
        Specification<Event> spec = EventSpecification.findUpcomingEvents(
                title, location, startDate, endDate);

        // 3. Query ke DB
        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        // 4. Map ke DTO lalu wrap ke PageResponse
        Page<EventResponse> responsePage = eventPage.map(this::mapToEventResponse);

        return PageResponse.of(responsePage);
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