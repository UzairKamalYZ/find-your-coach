package com.findmycoach.user;

import com.findmycoach.auth.RefreshTokenService;
import com.findmycoach.booking.BookingService;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.trainer.TrainerStatus;
import com.findmycoach.user.dto.ChangePasswordRequest;
import com.findmycoach.user.dto.UpdateProfileRequest;
import com.findmycoach.user.dto.UserResponse;
import com.findmycoach.venue.VenueBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    @Lazy
    private final BookingService bookingService;
    @Lazy
    private final VenueBookingService venueBookingService;

    @Transactional(readOnly = true)
    public User require(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse toResponse(User user) {
        String trainerId = trainerRepository.findByUserId(user.getId()).map(Trainer::getId).orElse(null);
        return UserResponse.from(user, trainerId);
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateProfileRequest req) {
        User u = require(id);
        if (req.name() != null) {
            String name = Strings.clean(req.name());
            if (name == null) throw ApiException.badRequest("Name cannot be blank");
            u.setName(name);
        }
        apply(req.city(), u::setCity);
        apply(req.phone(), u::setPhone);
        apply(req.avatar(), u::setAvatar);
        apply(req.bio(), u::setBio);
        apply(req.sports(), u::setSports);
        apply(req.goals(), u::setGoals);
        apply(req.level(), u::setLevel);
        apply(req.experience(), u::setExperience);
        apply(req.qualifications(), u::setQualifications);
        return toResponse(u);
    }

    private static void apply(String value, Consumer<String> setter) {
        if (value != null) setter.accept(Strings.clean(value));
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest req) {
        User u = require(id);
        if (u.getPasswordHash() != null) {
            if (Strings.isBlank(req.currentPassword()) || !passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
                throw ApiException.badRequest("Current password is incorrect");
            }
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokenService.revokeAll(u);
    }

    /**
     * Self-service account closure: future bookings are cancelled, the coach listing (if any) is suspended,
     * sign-in is disabled and personal contact details are removed. Booking history is kept for the counterparties.
     */
    @Transactional
    public void deactivate(UUID id) {
        User u = require(id);
        if (u.isAdmin() && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.unprocessable("The last admin account cannot be closed");
        }
        bookingService.cancelAllFutureForAccountClosure(u);
        venueBookingService.cancelAllFutureForUser(u, "Account closed");
        trainerRepository.findByUserId(u.getId()).ifPresent(t -> {
            t.setStatus(TrainerStatus.SUSPENDED);
            t.setStatusReason("Coach closed their account");
        });
        u.setEnabled(false);
        u.setPhone(null);
        u.setAvatar(null);
        u.setBio(null);
        refreshTokenService.revokeAll(u);
    }
}
