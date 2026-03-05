package com.agora.booking.management.exception;

public class InsufficientSeatsException extends RuntimeException {

    public InsufficientSeatsException(int requested, int available) {
        super("Not enough seats available. Requested: " + requested
                + ", Available: " + available);
    }
}