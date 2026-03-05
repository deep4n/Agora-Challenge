package com.agora.booking.management.service;

import com.agora.booking.management.dto.request.CreateBookingRequest;
import com.agora.booking.management.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, String userEmail);

    List<BookingResponse> getMyBookings(String userEmail);
}