package com.findmycoach.user;

import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseIntegrationTest {

    @Test
    void getMe_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_incompleteProfile_returns403() throws Exception {
        User user = createUser("g1", "a@test.com", null);
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", incompleteToken(user)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getMe_validJwt_returnsUser() throws Exception {
        User user = createUser("g2", "b@test.com", "London");
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("b@test.com"))
            .andExpect(jsonPath("$.city").value("London"));
    }

    @Test
    void patchMe_updatesNameAndCity() throws Exception {
        User user = createUser("g3", "c@test.com", "London");
        mockMvc.perform(patch("/api/v1/me")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateProfileRequest("New Name", "Manchester", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.city").value("Manchester"));
    }
}
