package com.findmycoach.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Not transactional, with its own database: requests really race each other. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1;MODE=LEGACY")
class ConcurrentBookingTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    private String register() throws Exception {
        String body = om.writeValueAsString(Map.of("email", "race-" + UUID.randomUUID() + "@test.dev", "password", "Password123",
                "name", "Racer", "city", "London", "role", "trainee"));
        String res = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(res).get("accessToken").asText();
    }

    @Test
    void onlyOneOfManySimultaneousRequestsWinsTheSlot() throws Exception {
        JsonNode days = om.readTree(mvc.perform(get("/api/v1/trainers/leah-okafor/availability"))
                .andReturn().getResponse().getContentAsString());
        String date = null, slot = null;
        outer:
        for (int i = 1; i < days.size(); i++) {
            for (JsonNode s : days.get(i).get("slots")) {
                if (s.get("status").asText().equals("AVAILABLE")) {
                    date = days.get(i).get("date").asText();
                    slot = s.get("time").asText();
                    break outer;
                }
            }
        }
        assertThat(date).isNotNull();
        int n = 6;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < n; i++) tokens.add(register());
        String payload = om.writeValueAsString(Map.of("trainerId", "leah-okafor", "date", date, "slot", slot));

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String t : tokens) {
            results.add(pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/bookings").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) statuses.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(n - 1);
    }
}
