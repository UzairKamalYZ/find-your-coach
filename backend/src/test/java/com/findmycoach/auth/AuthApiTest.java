package com.findmycoach.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTest {

    private Map<String, Object> registration(String email, String role) {
        return Map.of("email", email, "password", "Password123", "name", "Jordan Lee", "city", "Brussels",
                "role", role, "phone", "+32 123");
    }

    @Test
    void registerReturnsTokensAndProfile() throws Exception {
        post(API + "/auth/register", registration("Jordan@Example.com", "trainer"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("jordan@example.com"))
                .andExpect(jsonPath("$.user.role").value("trainer"))
                .andExpect(jsonPath("$.user.phone").value("+32 123"))
                .andExpect(jsonPath("$.user.provider").value("Email"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        post(API + "/auth/register", registration("dup@example.com", "trainee"), null).andExpect(status().isCreated());
        post(API + "/auth/register", registration("DUP@example.com", "trainee"), null).andExpect(status().isConflict());
    }

    @Test
    void adminCannotSelfRegister() throws Exception {
        post(API + "/auth/register", registration("evil@example.com", "admin"), null).andExpect(status().isBadRequest());
    }

    @Test
    void registrationValidatesFields() throws Exception {
        post(API + "/auth/register", Map.of("email", "not-an-email", "password", "short", "name", "", "city", "", "role", "trainee"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists())
                .andExpect(jsonPath("$.details.city").exists());
    }

    @Test
    void loginChecksPassword() throws Exception {
        post(API + "/auth/login", Map.of("email", SAM, "password", "wrong-password"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Incorrect email or password"));
        post(API + "/auth/login", Map.of("email", "nobody@example.com", "password", "whatever"), null)
                .andExpect(status().isUnauthorized());
        post(API + "/auth/login", Map.of("email", SAM.toUpperCase(), "password", TRAINEE_PW), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Sam Carter"));
    }

    @Test
    void refreshRotatesAndDetectsReuse() throws Exception {
        JsonNode first = body(post(API + "/auth/login", Map.of("email", SAM, "password", TRAINEE_PW), null));
        String r1 = first.get("refreshToken").asText();
        JsonNode second = body(post(API + "/auth/refresh", Map.of("refreshToken", r1), null).andExpect(status().isOk()));
        String r2 = second.get("refreshToken").asText();
        assertThat(r2).isNotEqualTo(r1);
        // reusing r1 is treated as theft and revokes r2 as well
        post(API + "/auth/refresh", Map.of("refreshToken", r1), null).andExpect(status().isUnauthorized());
        post(API + "/auth/refresh", Map.of("refreshToken", r2), null).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String refresh = body(post(API + "/auth/login", Map.of("email", SAM, "password", TRAINEE_PW), null)).get("refreshToken").asText();
        post(API + "/auth/logout", Map.of("refreshToken", refresh), null).andExpect(status().isNoContent());
        post(API + "/auth/refresh", Map.of("refreshToken", refresh), null).andExpect(status().isUnauthorized());
    }

    @Test
    void demoSocialLoginKeepsRoleOfExistingAccount() throws Exception {
        post(API + "/auth/social", Map.of("provider", "Apple", "role", "trainer"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("alex.apple@example.com"))
                .andExpect(jsonPath("$.user.name").value("Alex Morgan (demo)"))
                .andExpect(jsonPath("$.user.role").value("trainer"))
                .andExpect(jsonPath("$.user.hasPassword").value(false));
        post(API + "/auth/social", Map.of("provider", "Apple"), null)
                .andExpect(jsonPath("$.user.role").value("trainer"));
        post(API + "/auth/social", Map.of("provider", "Email"), null).andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointsRequireValidToken() throws Exception {
        get(API + "/me", null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        get(API + "/me", "garbage.token.value").andExpect(status().isUnauthorized());
        get(API + "/me", login(SAM, TRAINEE_PW)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(SAM));
    }

    @Test
    void roleProtectedAreas() throws Exception {
        String trainee = login(SAM, TRAINEE_PW);
        get(API + "/admin/dashboard", trainee).andExpect(status().isForbidden());
        get(API + "/coach/stats", trainee).andExpect(status().isForbidden());
        get(API + "/admin/dashboard", login(ADMIN, ADMIN_PW)).andExpect(status().isOk());
    }
}
