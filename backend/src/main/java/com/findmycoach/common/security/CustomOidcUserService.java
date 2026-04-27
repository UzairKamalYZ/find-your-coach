package com.findmycoach.common.security;

import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : email;
        String picture = oidcUser.getPicture();

        userRepository.findByGoogleId(googleId).ifPresentOrElse(user -> {
            boolean changed = !Objects.equals(user.getName(), name)
                || !Objects.equals(user.getAvatar(), picture);
            if (changed) {
                user.setName(name);
                user.setAvatar(picture);
                userRepository.save(user);
            }
        }, () -> {
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setAvatar(picture);
            userRepository.save(newUser);
        });

        return oidcUser;
    }
}
