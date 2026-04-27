package com.findmycoach.trainer.dto;

import java.util.List;

public record DayAvailability(String date, List<String> slots) {}
