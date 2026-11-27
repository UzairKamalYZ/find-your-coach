package com.findmycoach.booking.dto;

import com.findmycoach.common.Slots;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record RescheduleRequest(
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = Slots.PATTERN, message = "must be a time like 09:00") String slot
) {
}
