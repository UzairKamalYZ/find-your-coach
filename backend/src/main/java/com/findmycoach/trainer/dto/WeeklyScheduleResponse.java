package com.findmycoach.trainer.dto;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public record WeeklyScheduleResponse(Map<DayOfWeek, List<String>> days, List<String> allSlots) {
}
