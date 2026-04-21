package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final UserRepository userRepository;
    @Value("${app.cdn-url}")
    private String cdnUrl;

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
                            resolveProfileImageUrl(user, attributes.profileImageUrl())
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
                                .isOnboardingCompleted(false)
                                .role(UserRole.USER)
                                .build()
                ));
    }

    private String resolveProfileImageUrl(User user, String oauthProfileImageUrl) {
        String existingProfileImageUrl = user.getProfileImageUrl();
        if (existingProfileImageUrl == null || existingProfileImageUrl.isBlank()) {
            return oauthProfileImageUrl;
        }

        if (oauthProfileImageUrl == null || oauthProfileImageUrl.isBlank()) {
            return existingProfileImageUrl;
        }

        String normalizedCdnUrl = cdnUrl.endsWith("/")
                ? cdnUrl.substring(0, cdnUrl.length() - 1)
                : cdnUrl;

        if (existingProfileImageUrl.startsWith(normalizedCdnUrl + "/")) {
            return existingProfileImageUrl;
        }

        return oauthProfileImageUrl;
    }
}
