package com.findmycoach.venue.dto;

import java.time.LocalDate;
import java.util.List;

/** Hour-by-hour remaining capacity for one day. */
public record VenueDayAvailability(LocalDate date, List<HourAvailability> hours) {
    public record HourAvailability(String time, int remaining, boolean available) {
    }
}
