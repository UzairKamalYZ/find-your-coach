package com.findmycoach.coach;

import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.trainer.TrainerStatus;

import java.math.BigDecimal;
import java.util.List;

public record CoachStats(
        TrainerStatus listingStatus,
        long upcomingSessions,
        long completedSessions,
        long noShows,
        long cancelledSessions,
        long uniqueTrainees,
        BigDecimal totalEarnings,
        BigDecimal earningsThisMonth,
        BigDecimal pendingEarnings,
        double rating,
        int reviews,
        long favorites,
        List<BookingResponse> nextSessions
) {
}
