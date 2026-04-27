package com.findmycoach.auth;

import com.findmycoach.auth.dto.AuthResponse;
import com.findmycoach.auth.dto.CompleteProfileRequest;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import com.findmycoach.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse refresh(String oldRefreshToken) {
        User user = tokenService.validateRefreshToken(oldRefreshToken);
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, null);
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    @Transactional
    public AuthResponse completeProfile(String userId, CompleteProfileRequest request) {
        User user = userRepository.findById(java.util.UUID.fromString(userId)).orElseThrow();
        user.setCity(request.city());
        userRepository.save(user);
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}
