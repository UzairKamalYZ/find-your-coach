package com.findmycoach.user;

import com.findmycoach.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeApiTest extends IntegrationTest {

    @Test
    void updatesProfileFieldsPartially() throws Exception {
        String token = login(SAM, TRAINEE_PW);
        patch(API + "/me", Map.of("sports", "Tennis", "goals", "Improve serve", "phone", "456", "level", "Advanced"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports").value("Tennis"))
                .andExpect(jsonPath("$.phone").value("456"))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.name").value("Sam Carter"));
        Map<String, Object> clear = new HashMap<>();
        clear.put("phone", "");
        patch(API + "/me", clear, token).andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void validatesProfile() throws Exception {
        String token = login(SAM, TRAINEE_PW);
        patch(API + "/me", Map.of("level", "Expert"), token).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.level").exists());
        patch(API + "/me", Map.of("name", ""), token).andExpect(status().isBadRequest());
        patch(API + "/me", Map.of("experience", "500"), token).andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        String token = login(SAM, TRAINEE_PW);
        post(API + "/me/password", Map.of("currentPassword", "nope", "newPassword", "BrandNew123"), token)
                .andExpect(status().isBadRequest());
        post(API + "/me/password", Map.of("currentPassword", TRAINEE_PW, "newPassword", "BrandNew123"), token)
                .andExpect(status().isNoContent());
        login(SAM, "BrandNew123");
    }

    @Test
    void deactivatedAccountCannotSignIn() throws Exception {
        String token = register("trainee");
        String email = body(get(API + "/me", token)).get("email").asText();
        delete(API + "/me", token).andExpect(status().isNoContent());
        get(API + "/me", token).andExpect(status().isUnauthorized());
        post(API + "/auth/login", Map.of("email", email, "password", "Password123"), null).andExpect(status().isForbidden());
    }

    @Test
    void favoritesRoundTrip() throws Exception {
        String token = register("trainee");
        put(API + "/me/favorites/marcus-reed", Map.of(), token).andExpect(status().isNoContent());
        put(API + "/me/favorites/marcus-reed", Map.of(), token).andExpect(status().isNoContent()); // idempotent
        get(API + "/me/favorites", token).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("marcus-reed"));
        get(API + "/me/favorites/ids", token).andExpect(jsonPath("$[0]").value("marcus-reed"));
        put(API + "/me/favorites/hannah-cole", Map.of(), token).andExpect(status().isNotFound()); // pending listing
        delete(API + "/me/favorites/marcus-reed", token).andExpect(status().isNoContent());
        get(API + "/me/favorites", token).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void notificationsAreScopedToOwner() throws Exception {
        String token = register("trainee"); // registration creates a welcome notification
        get(API + "/notifications/unread-count", token).andExpect(jsonPath("$.count").value(1));
        String id = body(get(API + "/notifications", token)).get("content").get(0).get("id").asText();
        String other = register("trainee");
        post(API + "/notifications/" + id + "/read", null, other).andExpect(status().isNotFound());
        post(API + "/notifications/" + id + "/read", null, token).andExpect(jsonPath("$.read").value(true));
        get(API + "/notifications/unread-count", token).andExpect(jsonPath("$.count").value(0));
        post(API + "/notifications/read-all", null, token).andExpect(status().isOk());
        delete(API + "/notifications/" + id, token).andExpect(status().isNoContent());
    }
}
