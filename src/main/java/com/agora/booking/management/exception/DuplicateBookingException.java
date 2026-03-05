package com.agora.booking.management.exception;

public class DuplicateBookingException extends RuntimeException {

    public DuplicateBookingException() {
        super("You have already booked this event");
    }
}