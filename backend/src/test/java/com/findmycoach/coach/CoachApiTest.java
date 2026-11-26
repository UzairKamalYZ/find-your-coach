package com.findmycoach.coach;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoachApiTest extends IntegrationTest {

    private Map<String, Object> listing(String name) {
        return Map.of("name", name, "sport", "swimming", "city", "Bristol", "hourlyRate", 42.5, "experience", 7,
                "bio", "Open water and pool technique.", "specialties", List.of("Front crawl", "Triathlon"));
    }

    @Test
    void newCoachListingNeedsApprovalBeforeGoingLive() throws Exception {
        String coach = register("trainer");
        get(API + "/coach/listing", coach).andExpect(status().isNotFound());
        String id = body(put(API + "/coach/listing", listing("Nadia Swim"), coach)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.sport").value("Swimming"))).get("id").asText();
        assertThat(id).isEqualTo("nadia-swim");
        get(API + "/me", coach).andExpect(jsonPath("$.trainerId").value(id));
        get(API + "/trainers/" + id, null).andExpect(status().isNotFound());

        String admin = login(ADMIN, ADMIN_PW);
        get(API + "/admin/trainers?status=PENDING", admin).andExpect(jsonPath("$.totalElements").value(2));
        patch(API + "/admin/trainers/" + id + "/status", Map.of("status", "ACTIVE"), admin).andExpect(status().isOk());
        get(API + "/trainers/" + id, null).andExpect(status().isOk()).andExpect(jsonPath("$.hourlyRate").value(42.5));
        get(API + "/notifications", coach).andExpect(jsonPath("$.content[0].type").value("LISTING_STATUS"));
    }

    @Test
    void listingValidation() throws Exception {
        String coach = register("trainer");
        put(API + "/coach/listing", Map.of("name", "X", "sport", "Quidditch", "city", "Leeds", "hourlyRate", 40, "experience", 2), coach)
                .andExpect(status().isBadRequest());
        put(API + "/coach/listing", Map.of("name", "X", "sport", "Tennis", "city", "Leeds", "hourlyRate", 1, "experience", 2), coach)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.hourlyRate").exists());
    }

    @Test
    void scheduleAndTimeOffDriveAvailability() throws Exception {
        String coach = register("trainer");
        String id = body(put(API + "/coach/listing", listing("Sched Coach"), coach)).get("id").asText();
        patch(API + "/admin/trainers/" + id + "/status", Map.of("status", "ACTIVE"), login(ADMIN, ADMIN_PW));

        Map<String, List<String>> days = Map.of("MONDAY", List.of("09:00"), "TUESDAY", List.of("09:00"), "WEDNESDAY", List.of("09:00"),
                "THURSDAY", List.of("09:00"), "FRIDAY", List.of("09:00"), "SATURDAY", List.of("09:00"), "SUNDAY", List.of("09:00"));
        put(API + "/coach/schedule", Map.of("days", days), coach).andExpect(status().isOk())
                .andExpect(jsonPath("$.days.MONDAY[0]").value("09:00"));
        put(API + "/coach/schedule", Map.of("days", Map.of("MONDAY", List.of("11:30"))), coach).andExpect(status().isBadRequest());

        LocalDate d = LocalDate.now().plusDays(3);
        JsonNode day = body(get(API + "/trainers/" + id + "/availability?from=" + d + "&to=" + d, null)).get(0);
        long avail = 0;
        for (JsonNode s : day.get("slots")) if (s.get("status").asText().equals("AVAILABLE")) avail++;
        assertThat(avail).isEqualTo(1);

        // a trainee books, then the coach takes that day off
        String trainee = register("trainee");
        String bookingId = book(trainee, id, new Slot(d.toString(), "09:00")).get("id").asText();
        post(API + "/coach/time-off", Map.of("from", d.toString(), "reason", "Competition"), coach).andExpect(status().isConflict());
        post(API + "/coach/time-off", Map.of("from", d.toString(), "reason", "Competition", "cancelExistingBookings", true), coach)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cancelledBookings").value(1));
        get(API + "/bookings/" + bookingId, trainee).andExpect(jsonPath("$.status").value("CANCELLED"));
        JsonNode off = body(get(API + "/trainers/" + id + "/availability?from=" + d + "&to=" + d, null)).get(0);
        assertThat(off.get("dayOff").asBoolean()).isTrue();
        assertThat(off.get("dayOffReason").asText()).isEqualTo("Competition");

        String timeOffId = body(get(API + "/coach/time-off", coach)).get(0).get("id").asText();
        delete(API + "/coach/time-off/" + timeOffId, coach).andExpect(status().isNoContent());
        post(API + "/coach/time-off", Map.of("from", LocalDate.now().minusDays(1).toString()), coach).andExpect(status().isBadRequest());
    }

    @Test
    void statsSummariseCoachActivity() throws Exception {
        String marcus = login(MARCUS, TRAINER_PW);
        get(API + "/coach/stats", marcus).andExpect(status().isOk())
                .andExpect(jsonPath("$.listingStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.completedSessions").value(1))
                .andExpect(jsonPath("$.upcomingSessions").value(2))
                .andExpect(jsonPath("$.totalEarnings").value(65.0))
                .andExpect(jsonPath("$.pendingEarnings").value(130.0))
                .andExpect(jsonPath("$.nextSessions.length()").value(2));
        get(API + "/coach/reviews", marcus).andExpect(jsonPath("$.totalElements").value(1));
    }
}
