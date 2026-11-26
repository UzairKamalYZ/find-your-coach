package com.findmycoach.trainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainerApiTest extends IntegrationTest {

    @Test
    void listsOnlyActiveCoachesWithFilters() throws Exception {
        get(API + "/trainers", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
        get(API + "/trainers?sport=tennis", null).andExpect(jsonPath("$.content[*].sport", everyItem(is("Tennis"))))
                .andExpect(jsonPath("$.totalElements").value(1));
        get(API + "/trainers?q=london", null).andExpect(jsonPath("$.totalElements").value(2));
        get(API + "/trainers?q=spin", null).andExpect(jsonPath("$.content[0].id").value("arjun-mehta"));
        get(API + "/trainers?maxRate=55&sort=price_asc", null)
                .andExpect(jsonPath("$.content[0].id").value("arjun-mehta"))
                .andExpect(jsonPath("$.content[1].id").value("sofia-alvarez"));
        get(API + "/trainers?minRating=4.85", null).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void pendingListingIsNotPublic() throws Exception {
        get(API + "/trainers/hannah-cole", null).andExpect(status().isNotFound());
        get(API + "/trainers/unknown", null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        get(API + "/trainers/marcus-reed", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyRate").value(65.0))
                .andExpect(jsonPath("$.image").value("trainer-1"))
                .andExpect(jsonPath("$.specialties.length()").value(3));
    }

    @Test
    void availabilityFollowsWeeklySchedule() throws Exception {
        JsonNode days = body(get(API + "/trainers/marcus-reed/availability", null).andExpect(status().isOk()));
        assertThat(days).hasSize(14);
        for (JsonNode day : days) {
            LocalDate d = LocalDate.parse(day.get("date").asText());
            for (JsonNode s : day.get("slots")) {
                String st = s.get("status").asText();
                if (d.getDayOfWeek() == java.time.DayOfWeek.SUNDAY || s.get("time").asText().equals("12:00")) {
                    assertThat(st).isIn("UNAVAILABLE", "PAST");
                }
            }
        }
    }

    @Test
    void availabilityRangeIsLimited() throws Exception {
        LocalDate from = LocalDate.now();
        get(API + "/trainers/marcus-reed/availability?from=" + from + "&to=" + from.plusDays(40), null)
                .andExpect(status().isBadRequest());
        get(API + "/trainers/marcus-reed/availability?from=" + from + "&to=" + from.minusDays(1), null)
                .andExpect(status().isBadRequest());
        get(API + "/trainers/marcus-reed/availability?from=not-a-date", null).andExpect(status().isBadRequest());
    }

    @Test
    void reviewsArePublicWithShortenedNames() throws Exception {
        get(API + "/trainers/marcus-reed/reviews", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorName").value("Sam C."))
                .andExpect(jsonPath("$.content[0].reply").isNotEmpty());
        get(API + "/trainers/marcus-reed/reviews/summary", null)
                .andExpect(jsonPath("$.rating").value(4.9))
                .andExpect(jsonPath("$.reviews").value(184))
                .andExpect(jsonPath("$.breakdown.5").value(1));
    }

    @Test
    void catalogueEndpoints() throws Exception {
        get(API + "/sports", null).andExpect(jsonPath("$.length()").value(6)).andExpect(jsonPath("$[0].name").value("Tennis"));
        get(API + "/trainers/cities", null).andExpect(jsonPath("$[0]").value("Birmingham"));
        get(API + "/stats", null).andExpect(jsonPath("$.coaches").value(4)).andExpect(jsonPath("$.venues").value(4));
    }
}
