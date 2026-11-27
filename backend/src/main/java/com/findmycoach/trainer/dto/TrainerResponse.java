package com.findmycoach.trainer.dto;

import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Field names match the frontend's Trainer type (image, reviews, hourlyRate...). */
public record TrainerResponse(
        String id,
        String name,
        String sport,
        String city,
        double rating,
        int reviews,
        BigDecimal hourlyRate,
        int experience,
        String bio,
        String image,
        List<String> specialties,
        String qualifications,
        int sessionMinutes,
        boolean featured,
        TrainerStatus status,
        String statusReason,
        Instant createdAt
) {
    public static TrainerResponse from(Trainer t) {
        return new TrainerResponse(t.getId(), t.getName(), t.getSport(), t.getCity(), t.getRating(), t.getReviewCount(),
                t.getHourlyRate(), t.getExperience(), t.getBio(), t.getImage(), List.copyOf(t.getSpecialties()),
                t.getQualifications(), t.getSessionMinutes(), t.isFeatured(), t.getStatus(), t.getStatusReason(),
                t.getCreatedAt());
    }
}
