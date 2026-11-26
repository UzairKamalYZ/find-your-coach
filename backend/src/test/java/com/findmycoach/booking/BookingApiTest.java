package com.findmycoach.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingApiTest extends IntegrationTest {

    @Test
    void bookSnapshotsTrainerAndBlocksSlot() throws Exception {
        String token = register("trainee");
        Slot slot = firstAvailable("marcus-reed", 0);
        JsonNode b = book(token, "marcus-reed", slot);
        assertThat(b.get("trainerName").asText()).isEqualTo("Marcus Reed");
        assertThat(b.get("price").asDouble()).isEqualTo(65.0);
        assertThat(b.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(b.get("canCancel").asBoolean()).isTrue();
        assertThat(b.get("traineeEmail")).isNull(); // trainee does not need their own contact echoed

        JsonNode days = body(get(API + "/trainers/marcus-reed/availability?from=" + slot.date() + "&to=" + slot.date(), null));
        boolean booked = false;
        for (JsonNode s : days.get(0).get("slots")) {
            if (s.get("time").asText().equals(slot.time())) booked = s.get("status").asText().equals("BOOKED");
        }
        assertThat(booked).isTrue();

        String other = register("trainee");
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", slot.date(), "slot", slot.time()), other)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Slot already booked"));
    }

    @Test
    void traineeCannotBeInTwoPlacesAtOnce() throws Exception {
        String token = register("trainee");
        // Leah and Marcus both work Tuesdays at 09:00; find a common free slot
        Slot slot = firstAvailable("leah-okafor", 0);
        book(token, "leah-okafor", slot);
        JsonNode r = body(post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", slot.date(), "slot", slot.time()), token));
        assertThat(r.get("status").asInt()).isIn(409, 422);
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        String token = register("trainee");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", LocalDate.now().minusDays(1).toString(), "slot", "09:00"), token)
                .andExpect(status().isUnprocessableEntity());
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", tomorrow.toString(), "slot", "11:00"), token)
                .andExpect(status().isBadRequest());
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", tomorrow.toString(), "slot", "9am"), token)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.slot").exists());
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", tomorrow.toString(), "slot", "12:00"), token)
                .andExpect(status().isUnprocessableEntity()); // Marcus never works 12:00
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", LocalDate.now().plusDays(90).toString(), "slot", "09:00"), token)
                .andExpect(status().isUnprocessableEntity());
        post(API + "/bookings", Map.of("trainerId", "hannah-cole", "date", tomorrow.toString(), "slot", "17:00"), token)
                .andExpect(status().isUnprocessableEntity()); // pending listing
        post(API + "/bookings", Map.of("trainerId", "nobody", "date", tomorrow.toString(), "slot", "09:00"), token)
                .andExpect(status().isNotFound());
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", tomorrow.toString(), "slot", "09:00"), null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void coachCannotBookThemselves() throws Exception {
        String marcus = login(MARCUS, TRAINER_PW);
        Slot slot = firstAvailable("marcus-reed", 0);
        post(API + "/bookings", Map.of("trainerId", "marcus-reed", "date", slot.date(), "slot", slot.time()), marcus)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listScopesAndCancellation() throws Exception {
        String sam = login(SAM, TRAINEE_PW);
        JsonNode active = body(get(API + "/bookings", sam).andExpect(status().isOk()));
        assertThat(active).hasSize(4);
        JsonNode upcoming = body(get(API + "/bookings?scope=upcoming", sam));
        assertThat(upcoming).hasSize(2);
        assertThat(body(get(API + "/bookings?scope=past", sam))).hasSize(2);

        String id = upcoming.get(0).get("id").asText();
        String stranger = register("trainee");
        post(API + "/bookings/" + id + "/cancel", Map.of(), stranger).andExpect(status().isNotFound());
        get(API + "/bookings/" + id, stranger).andExpect(status().isNotFound());

        post(API + "/bookings/" + id + "/cancel", Map.of("reason", "Feeling unwell"), sam)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("trainee"))
                .andExpect(jsonPath("$.cancellationReason").value("Feeling unwell"));
        post(API + "/bookings/" + id + "/cancel", Map.of(), sam).andExpect(status().isUnprocessableEntity());
        assertThat(body(get(API + "/bookings", sam))).hasSize(3);
        assertThat(body(get(API + "/bookings?scope=cancelled", sam))).hasSize(1);

        // past sessions cannot be cancelled
        String pastId = body(get(API + "/bookings?scope=past", sam)).get(0).get("id").asText();
        delete(API + "/bookings/" + pastId, sam).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rescheduleMovesBookingAndFreesOldSlot() throws Exception {
        String token = register("trainee");
        Slot a = firstAvailable("leah-okafor", 0);
        String id = book(token, "leah-okafor", a).get("id").asText();
        Slot b = firstAvailable("leah-okafor", 0); // a is now BOOKED, so the next free slot
        post(API + "/bookings/" + id + "/reschedule", Map.of("date", b.date(), "slot", b.time()), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(b.date()))
                .andExpect(jsonPath("$.slot").value(b.time()))
                .andExpect(jsonPath("$.rescheduleCount").value(1));
        assertThat(firstAvailable("leah-okafor", 0)).isEqualTo(a);
        post(API + "/bookings/" + id + "/reschedule", Map.of("date", b.date(), "slot", b.time()), token)
                .andExpect(status().isBadRequest());
    }

    @Test
    void coachManagesOwnSessions() throws Exception {
        String trainee = register("trainee");
        Slot slot = firstAvailable("marcus-reed", 0);
        String id = book(trainee, "marcus-reed", slot).get("id").asText();
        String marcus = login(MARCUS, TRAINER_PW);

        JsonNode list = body(get(API + "/coach/bookings?status=CONFIRMED", marcus).andExpect(status().isOk()));
        assertThat(list.get("totalElements").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(list.get("content").get(0).get("traineeEmail")).isNotNull();

        patch(API + "/coach/bookings/" + id, Map.of("status", "COMPLETED"), marcus)
                .andExpect(status().isUnprocessableEntity()); // not started yet
        patch(API + "/coach/bookings/" + id, Map.of("status", "CANCELLED", "reason", "Court flooded"), marcus)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBy").value("trainer"));

        JsonNode notes = body(get(API + "/notifications", trainee));
        assertThat(notes.get("content").get(0).get("type").asText()).isEqualTo("BOOKING_CANCELLED");
        assertThat(notes.get("content").get(0).get("message").asText()).contains("Court flooded");

        String sofia = login("sofia@findmytrainer.dev", TRAINER_PW);
        patch(API + "/coach/bookings/" + id, Map.of("status", "NO_SHOW"), sofia).andExpect(status().isNotFound());
    }

    @Test
    void coachIsNotifiedOfNewBookings() throws Exception {
        String trainee = register("trainee");
        book(trainee, "arjun-mehta", firstAvailable("arjun-mehta", 0));
        String arjun = login("arjun@findmytrainer.dev", TRAINER_PW);
        get(API + "/notifications?unreadOnly=true", arjun)
                .andExpect(jsonPath("$.content[0].type").value("BOOKING_RECEIVED"));
    }
}
