package com.findmycoach.trainer.dto;

import com.findmycoach.trainer.TimeOff;

import java.time.LocalDate;
import java.util.UUID;

public record TimeOffResponse(UUID id, LocalDate date, String reason) {
    public static TimeOffResponse from(TimeOff t) {
        return new TimeOffResponse(t.getId(), t.getDate(), t.getReason());
    }
}
