package com.findmycoach.booking;

import com.findmycoach.booking.dto.CreateBookingRequest;
import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerTest extends BaseIntegrationTest {

    @Autowired TrainerRepository trainerRepository;
    @Autowired BookingRepository bookingRepository;

    private Trainer seedTrainer() {
        Trainer t = new Trainer();
        t.setId("t-test"); t.setName("Coach Test"); t.setSport("Tennis");
        t.setCity("London"); t.setRating(BigDecimal.valueOf(4.5));
        t.setReviews(10); t.setHourlyRate(6500); t.setExperience(5); t.setSpecialties("[]");
        return trainerRepository.save(t);
    }

    @Test
    void createBooking_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(1), "08:00"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createBooking_incompleteProfile_returns403() throws Exception {
        seedTrainer();
        User user = createUser("g-inc", "inc@test.com", null);
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", incompleteToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(1), "08:00"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createBooking_valid_returns201WithSnapshot() throws Exception {
        seedTrainer();
        User user = createUser("g1", "u1@test.com", "London");
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(1), "09:00"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.price").value(65.0))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void createBooking_sameSlotTwice_returns409() throws Exception {
        seedTrainer();
        User user = createUser("g2", "u2@test.com", "London");
        var body = objectMapper.writeValueAsString(
            new CreateBookingRequest("t-test", LocalDate.now().plusDays(2), "10:00"));

        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        User user2 = createUser("g3", "u3@test.com", "London");
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(user2))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void deleteBooking_notOwner_returns403() throws Exception {
        seedTrainer();
        User owner = createUser("g4", "owner@test.com", "London");
        User other = createUser("g5", "other@test.com", "London");

        var createBody = objectMapper.writeValueAsString(
            new CreateBookingRequest("t-test", LocalDate.now().plusDays(3), "12:00"));
        String responseBody = mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(owner))
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(responseBody).get("id").asText();
        mockMvc.perform(delete("/api/v1/bookings/" + bookingId)
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getBookings_statusUpcoming_returnsOnlyFutureBookings() throws Exception {
        seedTrainer();
        User user = createUser("g6", "u6@test.com", "London");
        String auth = bearerToken(user);

        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(4), "14:00"))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bookings").param("status", "upcoming")
                .header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
