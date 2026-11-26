package com.findmycoach.booking;

import com.findmycoach.booking.dto.*;
import com.findmycoach.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Coach bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService service;

    @Operation(summary = "Book a 60-minute session with a coach")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateBookingRequest req) {
        return service.create(me, req);
    }

    @Operation(summary = "My bookings as a trainee", description = "scope: active (default, excludes cancelled), upcoming, past, cancelled, all")
    @GetMapping
    public List<BookingResponse> mine(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String scope) {
        return service.listMine(me, scope);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.get(me, id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                  @Valid @RequestBody(required = false) CancelRequest req) {
        return service.cancel(me, id, req == null ? null : req.reason());
    }

    @Operation(summary = "Cancel (same as POST /{id}/cancel without a reason)")
    @DeleteMapping("/{id}")
    public BookingResponse delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.cancel(me, id, null);
    }

    @PostMapping("/{id}/reschedule")
    public BookingResponse reschedule(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                      @Valid @RequestBody RescheduleRequest req) {
        return service.reschedule(me, id, req);
    }
}
