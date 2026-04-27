package com.findmycoach.trainer;

import com.findmycoach.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrainerControllerTest extends BaseIntegrationTest {

    @Autowired
    private TrainerRepository trainerRepository;

    private Trainer seedTrainer(String id, String sport, String city) {
        Trainer t = new Trainer();
        t.setId(id);
        t.setName("Coach " + id);
        t.setSport(sport);
        t.setCity(city);
        t.setRating(BigDecimal.valueOf(4.5));
        t.setReviews(10);
        t.setHourlyRate(5000);
        t.setExperience(5);
        t.setSpecialties("[]");
        return trainerRepository.save(t);
    }

    @Test
    void listTrainers_filterBySport_returnsOnlyMatchingSport() throws Exception {
        seedTrainer("t1", "Tennis", "London");
        seedTrainer("t2", "Football", "London");

        mockMvc.perform(get("/api/v1/trainers").param("sport", "Tennis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].sport").value(org.hamcrest.Matchers.everyItem(
                org.hamcrest.Matchers.is("Tennis"))));
    }

    @Test
    void getTrainer_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/trainers/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void availability_rangeOver14Days_returns400() throws Exception {
        seedTrainer("t3", "Tennis", "London");
        String from = LocalDate.now().toString();
        String to = LocalDate.now().plusDays(30).toString();

        mockMvc.perform(get("/api/v1/trainers/t3/availability")
                .param("from", from).param("to", to))
            .andExpect(status().isBadRequest());
    }

    @Test
    void availability_returns14DaysOfSlots() throws Exception {
        seedTrainer("t4", "Tennis", "London");
        String from = LocalDate.now().toString();
        String to = LocalDate.now().plusDays(5).toString();

        mockMvc.perform(get("/api/v1/trainers/t4/availability")
                .param("from", from).param("to", to))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(6));
    }
}
