package com.findmycoach.stats;

import com.findmycoach.booking.BookingRepository;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.trainer.TrainerStatus;
import com.findmycoach.venue.VenueRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Headline numbers for the landing page. */
@Tag(name = "Catalogue")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class PublicStatsController {
    private final TrainerRepository trainerRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    public record PublicStats(long coaches, long venues, long sessionsBooked) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PublicStats stats() {
        long sessions = bookingRepository.count() - bookingRepository.countByStatus(BookingStatus.CANCELLED);
        return new PublicStats(trainerRepository.countByStatus(TrainerStatus.ACTIVE), venueRepository.countByActiveTrue(), sessions);
    }
}
