package com.findmycoach.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewApiTest extends IntegrationTest {

    private String reviewableBookingId(String token) throws Exception {
        for (JsonNode b : body(get(API + "/bookings?scope=past", token))) {
            if (b.get("canReview").asBoolean()) return b.get("id").asText();
        }
        throw new IllegalStateException("no reviewable booking");
    }

    @Test
    void traineeReviewsAttendedSessionAndRatingUpdates() throws Exception {
        String sam = login(SAM, TRAINEE_PW);
        String bookingId = reviewableBookingId(sam); // Leah, 3 days ago
        JsonNode before = body(get(API + "/trainers/leah-okafor", null));
        assertThat(before.get("reviews").asInt()).isEqualTo(121);

        String reviewId = body(post(API + "/reviews", Map.of("bookingId", bookingId, "rating", 1, "comment", "Did not enjoy it"), sam)
                .andExpect(status().isCreated())).get("id").asText();
        JsonNode after = body(get(API + "/trainers/leah-okafor", null));
        assertThat(after.get("reviews").asInt()).isEqualTo(122);
        assertThat(after.get("rating").asDouble()).isLessThanOrEqualTo(before.get("rating").asDouble());

        post(API + "/reviews", Map.of("bookingId", bookingId, "rating", 5), sam).andExpect(status().isConflict());

        // coach replies, trainee is notified
        String leah = login("leah@findmytrainer.dev", TRAINER_PW);
        post(API + "/coach/reviews/" + reviewId + "/reply", Map.of("reply", "Sorry to hear that, let's talk."), leah)
                .andExpect(status().isOk()).andExpect(jsonPath("$.reply").value("Sorry to hear that, let's talk."));
        post(API + "/coach/reviews/" + reviewId + "/reply", Map.of("reply", "x"), login(MARCUS, TRAINER_PW))
                .andExpect(status().isNotFound());

        // admin hides it: removed from rating and public list
        String admin = login(ADMIN, ADMIN_PW);
        patch(API + "/admin/reviews/" + reviewId, Map.of("hidden", true), admin).andExpect(jsonPath("$.hidden").value(true));
        assertThat(body(get(API + "/trainers/leah-okafor", null)).get("reviews").asInt()).isEqualTo(121);
        for (JsonNode r : body(get(API + "/trainers/leah-okafor/reviews", null)).get("content")) {
            assertThat(r.get("id").asText()).isNotEqualTo(reviewId);
        }
    }

    @Test
    void cannotReviewFutureOrOthersSessions() throws Exception {
        String sam = login(SAM, TRAINEE_PW);
        String upcoming = body(get(API + "/bookings?scope=upcoming", sam)).get(0).get("id").asText();
        post(API + "/reviews", Map.of("bookingId", upcoming, "rating", 5), sam).andExpect(status().isUnprocessableEntity());
        String other = register("trainee");
        post(API + "/reviews", Map.of("bookingId", reviewableBookingId(sam), "rating", 5), other).andExpect(status().isNotFound());
        post(API + "/reviews", Map.of("bookingId", upcoming, "rating", 6), sam).andExpect(status().isBadRequest());
    }

    @Test
    void authorCanEditAndDelete() throws Exception {
        String sam = login(SAM, TRAINEE_PW);
        String id = body(post(API + "/reviews", Map.of("bookingId", reviewableBookingId(sam), "rating", 3), sam)).get("id").asText();
        patch(API + "/reviews/" + id, Map.of("rating", 5, "comment", "Changed my mind, great!"), sam)
                .andExpect(jsonPath("$.rating").value(5));
        delete(API + "/reviews/" + id, register("trainee")).andExpect(status().isNotFound());
        delete(API + "/reviews/" + id, sam).andExpect(status().isNoContent());
        assertThat(body(get(API + "/trainers/leah-okafor", null)).get("reviews").asInt()).isEqualTo(121);
    }
}
