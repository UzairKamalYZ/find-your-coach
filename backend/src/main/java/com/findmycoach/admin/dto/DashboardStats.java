package com.findmycoach.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardStats(
        Map<String, Long> usersByRole,
        long totalUsers,
        long disabledUsers,
        Map<String, Long> trainersByStatus,
        long activeVenues,
        Map<String, Long> bookingsByStatus,
        long bookingsLast7Days,
        long venueBookings,
        BigDecimal completedRevenue,
        BigDecimal upcomingRevenue,
        BigDecimal venueRevenue,
        List<TopTrainer> topTrainers,
        Map<String, Long> bookingsBySport,
        List<DayCount> bookingsNext14Days
) {
    public record TopTrainer(String trainerId, String name, long sessions, BigDecimal revenue) {
    }

    public record DayCount(LocalDate date, long count) {
    }
}
