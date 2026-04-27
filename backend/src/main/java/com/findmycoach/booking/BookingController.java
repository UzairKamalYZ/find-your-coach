package com.findmycoach.booking;

import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(jwt.getSubject(), request);
    }

    @GetMapping
    public List<BookingResponse> list(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(defaultValue = "all") String status) {
        return bookingService.list(jwt.getSubject(), status);
    }

    @DeleteMapping("/{id}")
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        bookingService.cancel(id, jwt.getSubject());
    }
}
