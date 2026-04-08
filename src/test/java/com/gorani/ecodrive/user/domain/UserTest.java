package com.gorani.ecodrive.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("User 빌더 - isOnboardingCompleted=false로 생성")
    void builder_withOnboardingCompletedFalse() {
        // when
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-001")
                .email("test@example.com")
                .nickname("테스트유저")
                .profileImageUrl("https://img.example.com/1.jpg")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        // then
        assertThat(user.isOnboardingCompleted()).isFalse();
        assertThat(user.getOauthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(user.getOauthId()).isEqualTo("kakao-001");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getNickname()).isEqualTo("테스트유저");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://img.example.com/1.jpg");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("User 빌더 - isOnboardingCompleted=true로 생성")
    void builder_withOnboardingCompletedTrue() {
        // when
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-002")
                .email("completed@example.com")
                .nickname("완료유저")
                .isOnboardingCompleted(true)
                .role(UserRole.USER)
                .build();

        // then
        assertThat(user.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateOnboardingCompleted - false에서 true로 변경")
    void updateOnboardingCompleted_fromFalseToTrue() {
        // given
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-003")
                .email("test@test.com")
                .nickname("유저")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        assertThat(user.isOnboardingCompleted()).isFalse();

        // when
        user.updateOnboardingCompleted(true);

        // then
        assertThat(user.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateOnboardingCompleted - true에서 false로 변경")
    void updateOnboardingCompleted_fromTrueToFalse() {
        // given
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-004")
                .email("test2@test.com")
                .nickname("유저2")
                .isOnboardingCompleted(true)
                .role(UserRole.USER)
                .build();

        // when
        user.updateOnboardingCompleted(false);

        // then
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateProfile - 이메일, 닉네임, 프로필이미지 업데이트")
    void updateProfile_updatesEmailNicknameAndProfileImage() {
        // given
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-005")
                .email("old@test.com")
                .nickname("이전닉네임")
                .profileImageUrl("https://old.com/img.jpg")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        // when
        user.updateProfile("new@test.com", "새닉네임", "https://new.com/img.jpg");

        // then
        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://new.com/img.jpg");
    }

    @Test
    @DisplayName("User 빌더 - profileImageUrl null 허용")
    void builder_withNullProfileImageUrl() {
        // when
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-006")
                .email("noimage@test.com")
                .nickname("이미지없음")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        // then
        assertThat(user.getProfileImageUrl()).isNull();
    }
}