package com.findmycoach.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmycoach.auth.TokenService;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.Config.class)
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected TokenService tokenService;

    protected User createUser(String googleId, String email, String city) {
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName("Test User");
        user.setCity(city);
        return userRepository.save(user);
    }

    protected String bearerToken(User user) {
        return "Bearer " + tokenService.generateAccessToken(user);
    }

    protected String incompleteToken(User user) {
        user.setCity(null);
        return "Bearer " + tokenService.generateAccessToken(user);
    }
}
