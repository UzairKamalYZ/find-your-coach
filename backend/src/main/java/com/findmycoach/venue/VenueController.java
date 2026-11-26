package com.findmycoach.venue;

import com.findmycoach.common.web.PageResponse;
import com.findmycoach.venue.dto.VenueDayAvailability;
import com.findmycoach.venue.dto.VenueResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Venues (public)")
@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService service;

    @GetMapping
    public PageResponse<VenueResponse> list(@RequestParam(required = false) String sport,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) VenueType type,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) BigDecimal maxPrice,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.search(sport, city, type, q, maxPrice, sort, false, page, size);
    }

    @GetMapping("/{id}")
    public VenueResponse get(@PathVariable String id) {
        return service.getPublic(id);
    }

    @GetMapping("/{id}/availability")
    public List<VenueDayAvailability> availability(@PathVariable String id,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        service.getPublic(id);
        return service.availability(id, from, to);
    }
}
