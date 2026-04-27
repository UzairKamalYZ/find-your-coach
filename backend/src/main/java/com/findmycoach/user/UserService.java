package com.findmycoach.user;

import com.findmycoach.common.exception.AppException;
import com.findmycoach.user.dto.UpdateProfileRequest;
import com.findmycoach.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUser(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> AppException.notFound("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> AppException.notFound("User not found"));
        if (request.name() != null) user.setName(request.name());
        if (request.city() != null) user.setCity(request.city());
        if (request.avatar() != null) user.setAvatar(request.avatar());
        return UserResponse.from(userRepository.save(user));
    }
}
