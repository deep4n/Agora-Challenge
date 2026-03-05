package com.agora.booking.management.controller;

import com.agora.booking.management.dto.request.CreateBookingRequest;
import com.agora.booking.management.dto.response.ApiResponse;
import com.agora.booking.management.dto.response.BookingResponse;
import com.agora.booking.management.service.BookingService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // =============================================
    // POST /api/bookings — FR10 + FR13 + FR14
    // Protected — hanya user login yang bisa booking
    // =============================================
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("POST /api/bookings - eventId: {}, user: {}",
                request.getEventId(), userDetails.getUsername());

        BookingResponse bookingResponse = bookingService.createBooking(
                request,
                userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", bookingResponse));
    }

    // =============================================
    // GET /api/bookings/me — FR11
    // Protected — ambil semua booking milik user
    // Urutan: terbaru di atas
    // =============================================
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("GET /api/bookings/me - user: {}", userDetails.getUsername());

        List<BookingResponse> bookings = bookingService.getMyBookings(
                userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Bookings retrieved successfully", bookings));
    }
}