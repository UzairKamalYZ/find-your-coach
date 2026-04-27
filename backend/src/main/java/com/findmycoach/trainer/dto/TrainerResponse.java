package com.findmycoach.trainer.dto;

import com.findmycoach.trainer.Trainer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public record TrainerResponse(
    String id, String name, String sport, String city,
    double rating, int reviews, double hourlyRate,
    int experience, String bio, String imageUrl,
    List<String> specialties
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TrainerResponse from(Trainer t) {
        List<String> specs;
        try {
            specs = MAPPER.readValue(t.getSpecialties(), new TypeReference<>() {});
        } catch (Exception e) {
            specs = List.of();
        }
        return new TrainerResponse(
            t.getId(), t.getName(), t.getSport(), t.getCity(),
            t.getRating().doubleValue(), t.getReviews(),
            t.getHourlyRate() / 100.0,
            t.getExperience(), t.getBio(), t.getImageUrl(), specs
        );
    }
}
