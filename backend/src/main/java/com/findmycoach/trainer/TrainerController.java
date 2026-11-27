package com.findmycoach.trainer;

import com.findmycoach.common.web.PageResponse;
import com.findmycoach.review.ReviewService;
import com.findmycoach.review.dto.ReviewResponse;
import com.findmycoach.review.dto.ReviewSummary;
import com.findmycoach.trainer.dto.DayAvailability;
import com.findmycoach.trainer.dto.TrainerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Tag(name = "Coaches (public)")
@RestController
@RequestMapping("/api/v1/trainers")
@RequiredArgsConstructor
public class TrainerController {
    private final TrainerService trainerService;
    private final ReviewService reviewService;

    @Operation(summary = "Search active coaches",
            description = "sort: recommended (default), rating, reviews, price_asc, price_desc, experience, name, newest")
    @GetMapping
    public PageResponse<TrainerResponse> list(@RequestParam(required = false) String sport,
                                              @RequestParam(required = false) String city,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false) BigDecimal minRate,
                                              @RequestParam(required = false) BigDecimal maxRate,
                                              @RequestParam(required = false) Double minRating,
                                              @RequestParam(required = false) Boolean featured,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        var criteria = new TrainerService.SearchCriteria(sport, city, q, minRate, maxRate, minRating, featured, sort);
        return trainerService.search(criteria, EnumSet.of(TrainerStatus.ACTIVE), page, size);
    }

    @GetMapping("/cities")
    public List<String> cities() {
        return trainerService.activeCities();
    }

    @GetMapping("/{id}")
    public TrainerResponse get(@PathVariable String id) {
        return trainerService.getPublic(id);
    }

    @Operation(summary = "Slot-by-slot availability (defaults to the next 14 days, max 31)")
    @GetMapping("/{id}/availability")
    public List<DayAvailability> availability(@PathVariable String id,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        trainerService.getPublic(id);
        return trainerService.availability(id, from, to);
    }

    @GetMapping("/{id}/reviews")
    public PageResponse<ReviewResponse> reviews(@PathVariable String id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        trainerService.getPublic(id);
        return reviewService.listForTrainer(id, page, size);
    }

    @GetMapping("/{id}/reviews/summary")
    public ReviewSummary reviewSummary(@PathVariable String id) {
        trainerService.getPublic(id);
        return reviewService.summary(id);
    }
}
