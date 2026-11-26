package com.findmycoach.coach;

import com.findmycoach.booking.BookingService;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.booking.dto.BookingStatusUpdateRequest;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.review.ReviewService;
import com.findmycoach.review.dto.ReplyRequest;
import com.findmycoach.review.dto.ReviewResponse;
import com.findmycoach.security.AuthUser;
import com.findmycoach.trainer.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Self-service endpoints for users with the TRAINER role. */
@Tag(name = "Coach dashboard")
@RestController
@RequestMapping("/api/v1/coach")
@RequiredArgsConstructor
public class CoachController {
    private final CoachService coachService;
    private final BookingService bookingService;
    private final ReviewService reviewService;

    @GetMapping("/listing")
    public TrainerResponse listing(@AuthenticationPrincipal AuthUser me) {
        return coachService.getListing(me);
    }

    @Operation(summary = "Create or update my coaching listing (new listings await admin approval)")
    @PutMapping("/listing")
    public TrainerResponse saveListing(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TrainerProfileRequest req) {
        return coachService.saveListing(me, req);
    }

    @GetMapping("/schedule")
    public WeeklyScheduleResponse schedule(@AuthenticationPrincipal AuthUser me) {
        return coachService.getSchedule(me);
    }

    @PutMapping("/schedule")
    public WeeklyScheduleResponse saveSchedule(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody WeeklyScheduleRequest req) {
        return coachService.saveSchedule(me, req);
    }

    @GetMapping("/time-off")
    public List<TimeOffResponse> timeOff(@AuthenticationPrincipal AuthUser me) {
        return coachService.listTimeOff(me);
    }

    @PostMapping("/time-off")
    @ResponseStatus(HttpStatus.CREATED)
    public CoachService.TimeOffResult addTimeOff(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TimeOffRequest req) {
        return coachService.addTimeOff(me, req);
    }

    @DeleteMapping("/time-off/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTimeOff(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        coachService.removeTimeOff(me, id);
    }

    @GetMapping("/bookings")
    public PageResponse<BookingResponse> bookings(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(required = false) BookingStatus status,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        String trainerId = coachService.requireOwnListing(me).getId();
        return bookingService.listForCoach(me, trainerId, status, from, to, page, size);
    }

    @Operation(summary = "Mark a session COMPLETED / NO_SHOW, or CANCEL it (trainee is notified)")
    @PatchMapping("/bookings/{id}")
    public BookingResponse updateBooking(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                         @Valid @RequestBody BookingStatusUpdateRequest req) {
        return bookingService.updateStatus(me, id, req);
    }

    @GetMapping("/reviews")
    public PageResponse<ReviewResponse> reviews(@AuthenticationPrincipal AuthUser me,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return reviewService.listForTrainer(coachService.requireOwnListing(me).getId(), page, size);
    }

    @PostMapping("/reviews/{id}/reply")
    public ReviewResponse reply(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody ReplyRequest req) {
        return reviewService.reply(me, id, req);
    }

    @GetMapping("/stats")
    public CoachStats stats(@AuthenticationPrincipal AuthUser me) {
        return coachService.stats(me);
    }
}
