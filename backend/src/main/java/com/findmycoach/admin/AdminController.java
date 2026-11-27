package com.findmycoach.admin;

import com.findmycoach.admin.dto.*;
import com.findmycoach.audit.AuditLog;
import com.findmycoach.booking.BookingService;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.booking.dto.BookingStatusUpdateRequest;
import com.findmycoach.booking.dto.CancelRequest;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.review.ReviewService;
import com.findmycoach.review.dto.ReviewResponse;
import com.findmycoach.security.AuthUser;
import com.findmycoach.sport.SportService;
import com.findmycoach.trainer.TrainerStatus;
import com.findmycoach.trainer.dto.TrainerProfileRequest;
import com.findmycoach.trainer.dto.TrainerResponse;
import com.findmycoach.user.Role;
import com.findmycoach.user.dto.UserResponse;
import com.findmycoach.venue.VenueBookingService;
import com.findmycoach.venue.VenueBookingStatus;
import com.findmycoach.venue.VenueService;
import com.findmycoach.venue.VenueType;
import com.findmycoach.venue.dto.VenueBookingResponse;
import com.findmycoach.venue.dto.VenueRequest;
import com.findmycoach.venue.dto.VenueResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final BookingService bookingService;
    private final VenueService venueService;
    private final VenueBookingService venueBookingService;
    private final ReviewService reviewService;
    private final SportService sportService;

    @GetMapping("/dashboard")
    public DashboardStats dashboard() {
        return adminService.dashboard();
    }

    // users
    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required = false) String q, @RequestParam(required = false) Role role,
                                            @RequestParam(required = false) Boolean enabled,
                                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminService.users(q, role, enabled, page, size);
    }

    @GetMapping("/users/{id}")
    public UserResponse user(@PathVariable UUID id) {
        return adminService.user(id);
    }

    @PatchMapping("/users/{id}")
    public UserResponse updateUser(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                   @Valid @RequestBody AdminUserUpdateRequest req) {
        return adminService.updateUser(me, id, req);
    }

    // trainers
    @GetMapping("/trainers")
    public PageResponse<TrainerResponse> trainers(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) TrainerStatus status,
                                                  @RequestParam(required = false) String sport,
                                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminService.trainers(q, status, sport, page, size);
    }

    @PostMapping("/trainers")
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerResponse createTrainer(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody AdminTrainerCreateRequest req) {
        return adminService.createTrainer(me, req);
    }

    @PutMapping("/trainers/{id}")
    public TrainerResponse updateTrainer(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                         @Valid @RequestBody TrainerProfileRequest req) {
        return adminService.updateTrainer(me, id, req);
    }

    @PatchMapping("/trainers/{id}/status")
    public TrainerResponse trainerStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                         @Valid @RequestBody TrainerStatusRequest req) {
        return adminService.setTrainerStatus(me, id, req);
    }

    @PatchMapping("/trainers/{id}/featured")
    public TrainerResponse featured(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                    @RequestBody Map<String, Boolean> body) {
        return adminService.setFeatured(me, id, Boolean.TRUE.equals(body.get("featured")));
    }

    // bookings
    @GetMapping("/bookings")
    public PageResponse<BookingResponse> bookings(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(required = false) String trainerId,
                                                  @RequestParam(required = false) UUID traineeId,
                                                  @RequestParam(required = false) BookingStatus status,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return bookingService.adminSearch(me, trainerId, traineeId, status, from, to, page, size);
    }

    @PatchMapping("/bookings/{id}")
    public BookingResponse updateBooking(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                         @Valid @RequestBody BookingStatusUpdateRequest req) {
        return bookingService.updateStatus(me, id, req);
    }

    // venues
    @GetMapping("/venues")
    public PageResponse<VenueResponse> venues(@RequestParam(required = false) String q, @RequestParam(required = false) String sport,
                                              @RequestParam(required = false) VenueType type,
                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return venueService.search(sport, null, type, q, null, "name", true, page, size);
    }

    @PostMapping("/venues")
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse createVenue(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody VenueRequest req) {
        return venueService.create(me, req);
    }

    @PutMapping("/venues/{id}")
    public VenueResponse updateVenue(@AuthenticationPrincipal AuthUser me, @PathVariable String id, @Valid @RequestBody VenueRequest req) {
        return venueService.update(me, id, req);
    }

    @DeleteMapping("/venues/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateVenue(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        venueService.deactivate(me, id);
    }

    @GetMapping("/venue-bookings")
    public PageResponse<VenueBookingResponse> venueBookings(@AuthenticationPrincipal AuthUser me,
                                                            @RequestParam(required = false) String venueId,
                                                            @RequestParam(required = false) VenueBookingStatus status,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return venueBookingService.adminSearch(me, venueId, status, from, to, page, size);
    }

    @PostMapping("/venue-bookings/{id}/cancel")
    public VenueBookingResponse cancelVenueBooking(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                                   @Valid @RequestBody(required = false) CancelRequest req) {
        return venueBookingService.cancel(me, id, req == null ? null : req.reason());
    }

    // reviews
    @GetMapping("/reviews")
    public PageResponse<ReviewResponse> reviews(@RequestParam(required = false) String trainerId,
                                                @RequestParam(required = false) Boolean hidden,
                                                @RequestParam(required = false) Integer maxRating,
                                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reviewService.adminSearch(trainerId, hidden, maxRating, page, size);
    }

    @PatchMapping("/reviews/{id}")
    public ReviewResponse moderateReview(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                         @RequestBody Map<String, Boolean> body) {
        return reviewService.setHidden(me, id, Boolean.TRUE.equals(body.get("hidden")));
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        reviewService.delete(me, id);
    }

    // sports
    @GetMapping("/sports")
    public List<SportService.SportResponse> sports() {
        return sportService.listAll();
    }

    @PutMapping("/sports")
    public SportService.SportResponse upsertSport(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SportService.SportRequest req) {
        return sportService.upsert(req);
    }

    // audit
    @GetMapping("/audit-logs")
    public PageResponse<AuditLog> audit(@RequestParam(required = false) String action,
                                        @RequestParam(required = false) String targetType,
                                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return adminService.auditLogs(action, targetType, page, size);
    }
}
