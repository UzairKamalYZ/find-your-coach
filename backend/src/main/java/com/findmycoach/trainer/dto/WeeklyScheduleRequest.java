package com.findmycoach.trainer.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/** Full replacement of a coach's weekly schedule: day of week to list of slot times ("HH:mm"). */
public record WeeklyScheduleRequest(@NotNull Map<DayOfWeek, List<String>> days) {
}
