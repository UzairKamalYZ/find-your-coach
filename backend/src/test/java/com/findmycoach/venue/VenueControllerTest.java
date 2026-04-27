package com.findmycoach.venue;

import com.findmycoach.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VenueControllerTest extends BaseIntegrationTest {

    @Autowired
    private VenueRepository venueRepository;

    private void seedVenue(String id, String sport, String city) {
        Venue v = new Venue();
        v.setId(id); v.setName("Venue " + id);
        v.setType("Court"); v.setSport(sport); v.setCity(city);
        v.setPricePerHour(3000); v.setRating(BigDecimal.valueOf(4.5));
        venueRepository.save(v);
    }

    @Test
    void listVenues_filterBySport_returnsOnlyMatchingSport() throws Exception {
        seedVenue("v1", "Tennis", "London");
        seedVenue("v2", "Football", "Manchester");

        mockMvc.perform(get("/api/v1/venues").param("sport", "Tennis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].sport")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Tennis"))));
    }

    @Test
    void listVenues_noFilter_returnsAll() throws Exception {
        seedVenue("v3", "Cricket", "Birmingham");
        seedVenue("v4", "Basketball", "London");

        mockMvc.perform(get("/api/v1/venues"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
