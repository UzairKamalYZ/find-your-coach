package com.findmycoach.booking.dto;

import com.findmycoach.common.Slots;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotBlank String trainerId,
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = Slots.PATTERN, message = "must be a time like 09:00") String slot,
        @Size(max = 500) String notes
) {
}
