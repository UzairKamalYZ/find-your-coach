package com.findmycoach.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmycoach.seed.DemoDataSeeder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test base: real security, H2 in-memory DB seeded with demo data. Each test runs in a transaction that
 * is rolled back, so tests do not see each other's writes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTest {
    protected static final String API = "/api/v1";
    protected static final String ADMIN = DemoDataSeeder.ADMIN_EMAIL;
    protected static final String ADMIN_PW = DemoDataSeeder.ADMIN_PASSWORD;
    protected static final String SAM = DemoDataSeeder.TRAINEE_EMAIL;
    protected static final String TRAINEE_PW = DemoDataSeeder.TRAINEE_PASSWORD;
    protected static final String MARCUS = "marcus@findmytrainer.dev";
    protected static final String TRAINER_PW = DemoDataSeeder.TRAINER_PASSWORD;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper om;

    protected String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    protected JsonNode body(ResultActions r) throws Exception {
        String s = r.andReturn().getResponse().getContentAsString();
        return s.isEmpty() ? om.nullNode() : om.readTree(s);
    }

    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header("Authorization", "Bearer " + token);
    }

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token));
    }

    protected ResultActions post(String url, Object body, String token) throws Exception {
        var b = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) b.content(json(body));
        return mvc.perform(auth(b, token));
    }

    protected ResultActions put(String url, Object body, String token) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), token));
    }

    protected ResultActions patch(String url, Object body, String token) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), token));
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url), token));
    }

    protected String login(String email, String password) throws Exception {
        return body(post(API + "/auth/login", Map.of("email", email, "password", password), null)
                .andExpect(status().isOk())).get("accessToken").asText();
    }

    /** Registers a fresh account and returns its access token. */
    protected String register(String role) throws Exception {
        String email = role + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        return body(post(API + "/auth/register", Map.of("email", email, "password", "Password123",
                "name", "Test " + role, "city", "London", "role", role), null)
                .andExpect(status().isCreated())).get("accessToken").asText();
    }

    /** First AVAILABLE slot for a coach within the next two weeks, starting at least a day from now. */
    protected Slot firstAvailable(String trainerId, int skip) throws Exception {
        JsonNode days = body(get(API + "/trainers/" + trainerId + "/availability", null).andExpect(status().isOk()));
        int seen = 0;
        for (int i = 1; i < days.size(); i++) {
            for (JsonNode s : days.get(i).get("slots")) {
                if ("AVAILABLE".equals(s.get("status").asText()) && seen++ == skip) {
                    return new Slot(days.get(i).get("date").asText(), s.get("time").asText());
                }
            }
        }
        throw new IllegalStateException("No available slot for " + trainerId);
    }

    public record Slot(String date, String time) {
    }

    protected JsonNode book(String token, String trainerId, Slot slot) throws Exception {
        return body(post(API + "/bookings", Map.of("trainerId", trainerId, "date", slot.date(), "slot", slot.time()), token)
                .andExpect(status().isCreated()));
    }
}
