package com.findmycoach.venue.dto;

import com.findmycoach.venue.VenueBooking;
import com.findmycoach.venue.VenueBookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record VenueBookingResponse(UUID id, String venueId, String venueName, String venueImage, String sport,
                                   String city, LocalDate date, LocalTime startTime, LocalTime endTime, int hours,
                                   BigDecimal totalPrice, VenueBookingStatus status, String notes,
                                   UUID userId, String userName, String userEmail, String cancellationReason,
                                   boolean canCancel, Instant createdAt) {
    public static VenueBookingResponse from(VenueBooking b, boolean canCancel, boolean includeContact) {
        var v = b.getVenue();
        return new VenueBookingResponse(b.getId(), v.getId(), v.getName(), v.getImage(), v.getSport(), v.getCity(),
                b.getDate(), b.getStartTime(), b.endTime(), b.getHours(), b.getTotalPrice(), b.getStatus(), b.getNotes(),
                b.getUser().getId(), includeContact ? b.getUser().getName() : null,
                includeContact ? b.getUser().getEmail() : null, b.getCancellationReason(), canCancel, b.getCreatedAt());
    }
}
