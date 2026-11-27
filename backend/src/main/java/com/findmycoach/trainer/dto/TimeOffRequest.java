package com.findmycoach.trainer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Blocks every day from "from" to "to" (inclusive; "to" defaults to "from"). */
public record TimeOffRequest(@NotNull LocalDate from, LocalDate to, @Size(max = 200) String reason,
                             Boolean cancelExistingBookings) {
}
