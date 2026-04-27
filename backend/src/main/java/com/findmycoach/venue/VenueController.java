package com.findmycoach.venue;

import com.findmycoach.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public Page<VenueResponse> list(
        @RequestParam(required = false) String sport,
        @RequestParam(required = false) String city,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return venueService.search(sport, city, PageRequest.of(page, Math.min(size, 100)));
    }
}
