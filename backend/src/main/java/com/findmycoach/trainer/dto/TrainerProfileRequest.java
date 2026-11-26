package com.findmycoach.trainer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Coach-editable listing fields (also used by admins). */
public record TrainerProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String sport,
        @NotBlank @Size(max = 100) String city,
        @NotNull @DecimalMin(value = "5.00", message = "must be at least 5") @DecimalMax(value = "1000.00", message = "must be at most 1000")
        @Digits(integer = 4, fraction = 2) BigDecimal hourlyRate,
        @NotNull @Min(0) @Max(80) Integer experience,
        @Size(max = 1000) String bio,
        @Size(max = 500) String image,
        @Size(max = 8) List<@NotBlank @Size(max = 60) String> specialties,
        @Size(max = 300) String qualifications
) {
}
