package com.findmycoach.venue;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VenueApiTest extends IntegrationTest {

    private final String day = LocalDate.now().plusDays(4).toString();

    @Test
    void listsAndFiltersVenues() throws Exception {
        get(API + "/venues", null).andExpect(jsonPath("$.totalElements").value(4));
        get(API + "/venues?sport=Cricket", null).andExpect(jsonPath("$.content[0].id").value("v4"));
        get(API + "/venues?type=Court", null).andExpect(jsonPath("$.totalElements").value(2));
        get(API + "/venues/v1", null).andExpect(jsonPath("$.capacity").value(4)).andExpect(jsonPath("$.type").value("Court"));
        get(API + "/venues/nope", null).andExpect(status().isNotFound());
    }

    @Test
    void bookingConsumesCapacity() throws Exception {
        String a = register("trainee");
        String b = register("trainee");
        post(API + "/venue-bookings", Map.of("venueId", "v4", "date", day, "startTime", "10:00", "hours", 2), a)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(240.0))
                .andExpect(jsonPath("$.endTime").value("12:00:00"));
        // v4 has a single ground: overlapping hour is taken
        post(API + "/venue-bookings", Map.of("venueId", "v4", "date", day, "startTime", "11:00", "hours", 1), b)
                .andExpect(status().isConflict());
        post(API + "/venue-bookings", Map.of("venueId", "v4", "date", day, "startTime", "12:00", "hours", 1), b)
                .andExpect(status().isCreated());

        JsonNode avail = body(get(API + "/venues/v4/availability?from=" + day + "&to=" + day, null));
        for (JsonNode h : avail.get(0).get("hours")) {
            String t = h.get("time").asText();
            if (t.equals("10:00") || t.equals("11:00") || t.equals("12:00")) assertThat(h.get("available").asBoolean()).isFalse();
            if (t.equals("13:00")) assertThat(h.get("available").asBoolean()).isTrue();
        }
    }

    @Test
    void enforcesOpeningHoursAndOwnership() throws Exception {
        String token = register("trainee");
        post(API + "/venue-bookings", Map.of("venueId", "v1", "date", day, "startTime", "21:00", "hours", 2), token)
                .andExpect(status().isUnprocessableEntity());
        post(API + "/venue-bookings", Map.of("venueId", "v1", "date", day, "startTime", "06:00", "hours", 1), token)
                .andExpect(status().isUnprocessableEntity());
        post(API + "/venue-bookings", Map.of("venueId", "v1", "date", day, "startTime", "09:00", "hours", 5), token)
                .andExpect(status().isBadRequest());
        String id = body(post(API + "/venue-bookings", Map.of("venueId", "v1", "date", day, "startTime", "09:00", "hours", 1), token)
                .andExpect(status().isCreated())).get("id").asText();
        get(API + "/venue-bookings", token).andExpect(jsonPath("$.length()").value(1));
        post(API + "/venue-bookings/" + id + "/cancel", null, register("trainee")).andExpect(status().isNotFound());
        post(API + "/venue-bookings/" + id + "/cancel", Map.of("reason", "Rain"), token)
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        get(API + "/venue-bookings", token).andExpect(jsonPath("$.length()").value(0));
        get(API + "/venue-bookings?includeCancelled=true", token).andExpect(jsonPath("$.length()").value(1));
    }
}
