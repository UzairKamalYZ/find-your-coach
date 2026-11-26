package com.findmycoach.admin.dto;

import com.findmycoach.trainer.dto.TrainerProfileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/** Admin-created listing, optionally linked to an existing TRAINER account by email. */
public record AdminTrainerCreateRequest(@NotNull @Valid TrainerProfileRequest profile, @Email String ownerEmail,
                                        Boolean featured) {
}
