package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.response.EventResponse;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request, String creatorEmail);
}