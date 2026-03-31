package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final UserRepository userRepository;

    @Transactional
    public User loginOrRegister(OAuthAttributes attributes) {
        return userRepository.findByOauthProviderAndOauthId(
                        attributes.provider(),
                        attributes.providerId()
                )
                .map(user -> {
                    user.updateProfile(
                            attributes.email(),
                            attributes.nickname(),
                            attributes.profileImageUrl()
                    );
                    return user;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .oauthProvider(attributes.provider())
                                .oauthId(attributes.providerId())
                                .email(attributes.email())
                                .nickname(attributes.nickname())
                                .profileImageUrl(attributes.profileImageUrl())
                                .role(UserRole.USER)
                                .build()
                ));
    }
}