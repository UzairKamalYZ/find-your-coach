package com.findmycoach.booking.dto;

import com.findmycoach.booking.Booking;

public record BookingResponse(
    String id, String trainerId, String trainerName, String trainerImageUrl,
    String sport, String date, String slot, double price, String status, String createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
            b.getId().toString(),
            b.getTrainer().getId(),
            b.getTrainerName(),
            b.getTrainerImageUrl(),
            b.getSport(),
            b.getBookingDate().toString(),
            b.getSlotTime(),
            b.getPrice() / 100.0,
            b.getStatus(),
            b.getCreatedAt().toString()
        );
    }
}
