package com.findmycoach.review.dto;

import com.findmycoach.review.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(UUID id, String trainerId, String trainerName, UUID bookingId, UUID authorId,
                             String authorName, int rating, String comment, String reply, Instant repliedAt,
                             boolean hidden, Instant createdAt) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getTrainer().getId(), r.getTrainer().getName(),
                r.getBooking() == null ? null : r.getBooking().getId(), r.getAuthor().getId(),
                publicName(r.getAuthor().getName()), r.getRating(), r.getComment(), r.getReply(), r.getRepliedAt(),
                r.isHidden(), r.getCreatedAt());
    }

    /** "Jordan Lee" becomes "Jordan L." to protect reviewer privacy on public pages. */
    static String publicName(String name) {
        if (name == null || name.isBlank()) return "Trainee";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
