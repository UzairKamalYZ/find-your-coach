package com.findmycoach.trainer.dto;

import java.time.LocalDate;
import java.util.List;

public record DayAvailability(LocalDate date, boolean dayOff, String dayOffReason, List<SlotAvailability> slots) {

    public enum SlotStatus {AVAILABLE, BOOKED, UNAVAILABLE, PAST}

    public record SlotAvailability(String time, SlotStatus status) {
        public boolean available() {
            return status == SlotStatus.AVAILABLE;
        }
    }
}
