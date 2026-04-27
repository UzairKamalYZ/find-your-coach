package com.findmycoach.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateBookingRequest(
    @NotBlank String trainerId,
    @NotNull LocalDate date,
    @NotBlank @Pattern(regexp = "^(08|09|10|12|14|15|17|18|19):00$",
                       message = "must be a valid slot time") String slot
) {}
