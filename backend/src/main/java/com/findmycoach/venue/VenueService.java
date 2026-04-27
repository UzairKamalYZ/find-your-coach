package com.findmycoach.venue;

import com.findmycoach.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    public Page<VenueResponse> search(String sport, String city, Pageable pageable) {
        return venueRepository.search(
            (sport != null && !sport.isBlank()) ? sport : null,
            (city != null && !city.isBlank()) ? city : null,
            pageable
        ).map(VenueResponse::from);
    }
}
