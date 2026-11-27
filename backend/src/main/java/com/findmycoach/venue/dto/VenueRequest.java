package com.findmycoach.venue.dto;

import com.findmycoach.venue.VenueType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record VenueRequest(
        @Size(max = 80) @Pattern(regexp = "^[a-z0-9-]*$", message = "may only contain lowercase letters, digits and dashes") String id,
        @NotBlank @Size(max = 100) String name,
        @NotNull VenueType type,
        @NotBlank @Size(max = 50) String sport,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 200) String address,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") @DecimalMax("10000.00") @Digits(integer = 5, fraction = 2) BigDecimal pricePerHour,
        @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
        @Size(max = 500) String image,
        @Size(max = 12) List<@NotBlank @Size(max = 60) String> amenities,
        @Min(1) @Max(50) Integer capacity,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean active
) {
}
