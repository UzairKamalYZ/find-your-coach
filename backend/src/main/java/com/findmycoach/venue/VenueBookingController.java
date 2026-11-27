package com.findmycoach.venue;

import com.findmycoach.booking.dto.CancelRequest;
import com.findmycoach.security.AuthUser;
import com.findmycoach.venue.dto.CreateVenueBookingRequest;
import com.findmycoach.venue.dto.VenueBookingResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Venue bookings")
@RestController
@RequestMapping("/api/v1/venue-bookings")
@RequiredArgsConstructor
public class VenueBookingController {
    private final VenueBookingService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueBookingResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateVenueBookingRequest req) {
        return service.create(me, req);
    }

    @GetMapping
    public List<VenueBookingResponse> mine(@AuthenticationPrincipal AuthUser me,
                                           @RequestParam(defaultValue = "false") boolean includeCancelled) {
        return service.listMine(me, includeCancelled);
    }

    @PostMapping("/{id}/cancel")
    public VenueBookingResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                       @Valid @RequestBody(required = false) CancelRequest req) {
        return service.cancel(me, id, req == null ? null : req.reason());
    }

    @DeleteMapping("/{id}")
    public VenueBookingResponse delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.cancel(me, id, null);
    }
}
