package com.agora.booking.management.controller;

import com.agora.booking.management.dto.request.CreateEventRequest;
import com.agora.booking.management.dto.request.UpdateEventRequest;
import com.agora.booking.management.dto.response.ApiResponse;
import com.agora.booking.management.dto.response.EventResponse;
import com.agora.booking.management.dto.response.PageResponse;
import com.agora.booking.management.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // =============================================
    // POST /api/events — FR04
    // =============================================
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("POST /api/events - title: '{}', creator: {}",
                request.getTitle(), userDetails.getUsername());

        EventResponse eventResponse = eventService.createEvent(
                request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully", eventResponse));
    }

    // =============================================
    // GET /api/events — FR05 + FR09
    // =============================================
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listEvents(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be zero or greater") int page,

            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") int size,

            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("GET /api/events - page: {}, size: {}, title: {}, location: {}, " +
                "startDate: {}, endDate: {}", page, size, title, location, startDate, endDate);

        PageResponse<EventResponse> response = eventService.listEvents(
                page, size, title, location, startDate, endDate);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Events retrieved successfully", response));
    }

    // =============================================
    // GET /api/events/{id} — FR06
    // =============================================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(
            @PathVariable Long id) {

        log.debug("GET /api/events/{} - fetching event detail", id);

        EventResponse eventResponse = eventService.getEventById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Event retrieved successfully", eventResponse));
    }

    // =============================================
    // PUT /api/events/{id} — FR07
    // =============================================
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("PUT /api/events/{} - editor: {}", id, userDetails.getUsername());

        EventResponse eventResponse = eventService.updateEvent(
                id, request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Event updated successfully", eventResponse));
    }

    // =============================================
    // DELETE /api/events/{id} — FR08
    // Soft delete — set isActive = false
    // Hanya creator yang boleh delete
    // =============================================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("DELETE /api/events/{} - deleter: {}", id, userDetails.getUsername());

        eventService.deleteEvent(id, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Event deleted successfully", null));
    }
}