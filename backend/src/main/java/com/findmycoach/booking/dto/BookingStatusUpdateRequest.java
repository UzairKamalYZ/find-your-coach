package com.findmycoach.booking.dto;

import com.findmycoach.booking.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Coach/admin outcome update: COMPLETED, NO_SHOW or CANCELLED (with optional reason). */
public record BookingStatusUpdateRequest(@NotNull BookingStatus status, @Size(max = 300) String reason) {
}
