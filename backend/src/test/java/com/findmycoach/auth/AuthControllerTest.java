package com.findmycoach.auth;

import com.findmycoach.auth.dto.CompleteProfileRequest;
import com.findmycoach.auth.dto.RefreshRequest;
import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Test
    void refresh_validToken_returnsNewTokenPair() throws Exception {
        User user = createUser("g1", "a@test.com", "London");
        String refreshToken = tokenService.generateRefreshToken(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_revokedToken_returns401() throws Exception {
        User user = createUser("g2", "b@test.com", "London");
        String refreshToken = tokenService.generateRefreshToken(user);
        tokenService.revokeRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void completeProfile_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/complete-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteProfileRequest("London"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void completeProfile_blankCity_returns400() throws Exception {
        User user = createUser("g3", "c@test.com", null);
        mockMvc.perform(post("/api/v1/auth/complete-profile")
                .header("Authorization", incompleteToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteProfileRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.city").exists());
    }

    @Test
    void completeProfile_valid_returnsTokensAndUser() throws Exception {
        User user = createUser("g4", "d@test.com", null);
        mockMvc.perform(post("/api/v1/auth/complete-profile")
                .header("Authorization", incompleteToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteProfileRequest("London"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.city").value("London"));
    }
}
