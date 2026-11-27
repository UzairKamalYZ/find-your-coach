package com.findmycoach.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiTest extends IntegrationTest {

    @Test
    void dashboardSummarisesPlatform() throws Exception {
        get(API + "/admin/dashboard", login(ADMIN, ADMIN_PW)).andExpect(status().isOk())
                .andExpect(jsonPath("$.usersByRole.admin").value(1))
                .andExpect(jsonPath("$.usersByRole.trainer").value(5))
                .andExpect(jsonPath("$.trainersByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.bookingsByStatus.CONFIRMED").value(5))
                .andExpect(jsonPath("$.completedRevenue").value(180.0))
                .andExpect(jsonPath("$.bookingsNext14Days.length()").value(14))
                .andExpect(jsonPath("$.topTrainers[0].trainerId").value("marcus-reed"));
    }

    @Test
    void searchAndDisableUsers() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        get(API + "/admin/users?role=TRAINEE", admin).andExpect(jsonPath("$.totalElements").value(3));
        get(API + "/admin/users?q=priya", admin).andExpect(jsonPath("$.content[0].email").value("priya@findmytrainer.dev"));

        String victim = register("trainee");
        String id = body(get(API + "/me", victim)).get("id").asText();
        patch(API + "/admin/users/" + id, Map.of("enabled", false), admin).andExpect(jsonPath("$.enabled").value(false));
        get(API + "/me", victim).andExpect(status().isUnauthorized()); // takes effect immediately
        patch(API + "/admin/users/" + id, Map.of("enabled", true, "role", "trainer"), admin)
                .andExpect(jsonPath("$.role").value("trainer"));
        get(API + "/coach/listing", victim).andExpect(status().isNotFound()); // now allowed into the coach area

        get(API + "/admin/audit-logs?targetType=USER", admin).andExpect(jsonPath("$.content[0].action").value("USER_UPDATED"));
    }

    @Test
    void adminCannotLockThemselvesOut() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        String id = body(get(API + "/me", admin)).get("id").asText();
        patch(API + "/admin/users/" + id, Map.of("enabled", false), admin).andExpect(status().isUnprocessableEntity());
        patch(API + "/admin/users/" + id, Map.of("role", "trainee"), admin).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void suspendingCoachHidesListingAndBlocksBookings() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        patch(API + "/admin/trainers/sofia-alvarez/status", Map.of("status", "SUSPENDED", "reason", "Licence expired"), admin)
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        get(API + "/trainers/sofia-alvarez", null).andExpect(status().isNotFound());
        get(API + "/trainers", null).andExpect(jsonPath("$.totalElements").value(3));
        patch(API + "/admin/trainers/sofia-alvarez/featured", Map.of("featured", false), admin)
                .andExpect(jsonPath("$.featured").value(false));
    }

    @Test
    void venueLifecycle() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        Map<String, Object> req = Map.of("name", "Aqua Centre", "type", "Pool", "sport", "Swimming", "city", "Leeds",
                "pricePerHour", 25, "capacity", 6, "amenities", List.of("Sauna"), "openTime", "06:00", "closeTime", "21:00");
        JsonNode v = body(post(API + "/admin/venues", req, admin).andExpect(status().isCreated()));
        assertThat(v.get("id").asText()).isEqualTo("aqua-centre");
        get(API + "/venues/aqua-centre", null).andExpect(jsonPath("$.type").value("Pool"));
        post(API + "/admin/venues", req, admin).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("aqua-centre-2"));
        delete(API + "/admin/venues/aqua-centre", admin).andExpect(status().isNoContent());
        get(API + "/venues/aqua-centre", null).andExpect(status().isNotFound());
        get(API + "/admin/venues?q=aqua", admin).andExpect(jsonPath("$.totalElements").value(2));
        post(API + "/admin/venues", Map.of("name", "Bad", "type", "Pool", "sport", "Swimming", "city", "Leeds",
                "pricePerHour", 25, "openTime", "20:00", "closeTime", "08:00"), admin).andExpect(status().isBadRequest());
    }

    @Test
    void adminCanCreateListingForExistingCoach() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        String coach = register("trainer");
        String email = body(get(API + "/me", coach)).get("email").asText();
        Map<String, Object> profile = Map.of("name", "Imported Coach", "sport", "Tennis", "city", "York", "hourlyRate", 45, "experience", 3);
        post(API + "/admin/trainers", Map.of("profile", profile, "ownerEmail", email), admin)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));
        get(API + "/coach/listing", coach).andExpect(jsonPath("$.name").value("Imported Coach"));
        post(API + "/admin/trainers", Map.of("profile", profile, "ownerEmail", SAM), admin).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminSeesAndCancelsAnyBooking() throws Exception {
        String admin = login(ADMIN, ADMIN_PW);
        JsonNode page = body(get(API + "/admin/bookings?status=CONFIRMED", admin));
        assertThat(page.get("totalElements").asInt()).isEqualTo(5);
        String id = page.get("content").get(0).get("id").asText();
        patch(API + "/admin/bookings/" + id, Map.of("status", "CANCELLED", "reason", "Duplicate"), admin)
                .andExpect(jsonPath("$.cancelledBy").value("admin"));
        get(API + "/admin/venue-bookings", admin).andExpect(jsonPath("$.totalElements").value(1));
        get(API + "/admin/sports", admin).andExpect(jsonPath("$.length()").value(6));
        put(API + "/admin/sports", Map.of("name", "Padel", "emoji", "🎾", "sortOrder", 7), admin).andExpect(jsonPath("$.active").value(true));
        get(API + "/sports", null).andExpect(jsonPath("$.length()").value(7));
    }
}
