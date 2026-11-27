package com.findmycoach.booking.dto;

import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.user.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String trainerId,
        String trainerName,
        String trainerImage,
        String sport,
        LocalDate date,
        String slot,
        LocalDateTime startsAt,
        BigDecimal price,
        BookingStatus status,
        String notes,
        UUID traineeId,
        String traineeName,
        String traineeEmail,
        String traineePhone,
        Role cancelledBy,
        String cancellationReason,
        Instant cancelledAt,
        boolean lateCancellation,
        int rescheduleCount,
        boolean canCancel,
        boolean canReschedule,
        boolean canReview,
        boolean reviewed,
        Instant createdAt
) {
    public record Flags(boolean canCancel, boolean canReschedule, boolean canReview, boolean reviewed) {
    }

    public static BookingResponse from(Booking b, Flags f, boolean includeTraineeContact) {
        return new BookingResponse(b.getId(), b.getTrainer().getId(), b.getTrainerName(), b.getTrainerImage(), b.getSport(),
                b.getDate(), b.getSlot(), b.getDate().atTime(java.time.LocalTime.parse(b.getSlot())), b.getPrice(),
                b.getStatus(), b.getNotes(), b.getTrainee().getId(), b.getTrainee().getName(),
                includeTraineeContact ? b.getTrainee().getEmail() : null,
                includeTraineeContact ? b.getTrainee().getPhone() : null,
                b.getCancelledBy(), b.getCancellationReason(), b.getCancelledAt(), b.isLateCancellation(),
                b.getRescheduleCount(), f.canCancel(), f.canReschedule(), f.canReview(), f.reviewed(), b.getCreatedAt());
    }
}
