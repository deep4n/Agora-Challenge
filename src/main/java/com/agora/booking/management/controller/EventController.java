package com.agora.booking.management.controller;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.response.ApiResponse;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // =============================================
    // POST /api/events — FR04
    // Hanya user login yang bisa membuat event
    // Creator diambil otomatis dari JWT token
    // =============================================
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("POST /api/events - title: '{}', creator: {}",
                request.getTitle(), userDetails.getUsername());

        EventResponse eventResponse = eventService.createEvent(
                request,
                userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully", eventResponse));
    }
}