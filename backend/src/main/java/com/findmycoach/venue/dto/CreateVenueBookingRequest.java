package com.findmycoach.venue.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateVenueBookingRequest(
        @NotBlank String venueId,
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):00$", message = "must be a full hour like 18:00") String startTime,
        @NotNull @Min(1) @Max(4) Integer hours,
        @Size(max = 500) String notes
) {
}
