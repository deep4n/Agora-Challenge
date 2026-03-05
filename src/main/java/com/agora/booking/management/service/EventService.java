package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.dto.response.PageResponse;

import java.time.LocalDateTime;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request, String creatorEmail);

    PageResponse<EventResponse> listEvents(
            int page,
            int size,
            String title,
            String location,
            LocalDateTime startDate,
            LocalDateTime endDate);
}