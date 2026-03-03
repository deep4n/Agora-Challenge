package com.agora.booking.management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    // description tidak wajib — boleh null
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotNull(message = "Available seats is required")
    @Min(value = 1, message = "Must be at least 1")
    private Integer availableSeats;

    @NotNull(message = "Ticket price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ticket price must be zero or greater")
    private BigDecimal ticketPrice;
}