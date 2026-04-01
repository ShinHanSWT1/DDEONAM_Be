package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.user.domain.OAuthProvider;

import java.util.Map;

public record OAuthAttributes(
        OAuthProvider provider,
        String providerId,
        String email,
        String nickname,
        String profileImageUrl
) {
    @SuppressWarnings("unchecked")
    public static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        String providerId = String.valueOf(attributes.get("id"));
        if(providerId == null){
            throw new IllegalArgumentException("kakao user id is required");
        }

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        String nickname = properties != null ? (String) properties.get("nickname") : "카카오사용자";
        String profileImageUrl = properties != null ? (String) properties.get("profile_image") : null;

        return new OAuthAttributes(
                OAuthProvider.KAKAO,
                providerId,
                email,
                nickname,
                profileImageUrl
        );
    }
}