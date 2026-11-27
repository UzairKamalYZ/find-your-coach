package com.findmycoach.admin.dto;

import com.findmycoach.trainer.TrainerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainerStatusRequest(@NotNull TrainerStatus status, @Size(max = 300) String reason) {
}
