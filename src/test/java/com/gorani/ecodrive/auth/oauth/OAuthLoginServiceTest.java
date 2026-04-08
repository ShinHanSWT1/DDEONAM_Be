package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.user.domain.OAuthProvider;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import com.gorani.ecodrive.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuthLoginService oAuthLoginService;

    @Test
    @DisplayName("신규 사용자 - 저장 시 isOnboardingCompleted=false로 설정됨")
    void loginOrRegister_newUser_savesWithOnboardingCompletedFalse() {
        // given
        OAuthAttributes attributes = new OAuthAttributes(
                OAuthProvider.KAKAO,
                "kakao-123",
                "newuser@test.com",
                "새사용자",
                "https://example.com/new.jpg"
        );

        User savedUser = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-123")
                .email("newuser@test.com")
                .nickname("새사용자")
                .profileImageUrl("https://example.com/new.jpg")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "kakao-123"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = oAuthLoginService.loginOrRegister(attributes);

        // then
        assertThat(result.isOnboardingCompleted()).isFalse();
        assertThat(result.getRole()).isEqualTo(UserRole.USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.isOnboardingCompleted()).isFalse();
        assertThat(capturedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(capturedUser.getEmail()).isEqualTo("newuser@test.com");
    }

    @Test
    @DisplayName("기존 사용자 - 프로필 업데이트 후 반환")
    void loginOrRegister_existingUser_updatesProfileAndReturns() {
        // given
        OAuthAttributes attributes = new OAuthAttributes(
                OAuthProvider.KAKAO,
                "kakao-456",
                "updated@test.com",
                "업데이트된닉네임",
                "https://example.com/updated.jpg"
        );

        User existingUser = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-456")
                .email("old@test.com")
                .nickname("이전닉네임")
                .profileImageUrl("https://example.com/old.jpg")
                .isOnboardingCompleted(true)
                .role(UserRole.USER)
                .build();

        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "kakao-456"))
                .willReturn(Optional.of(existingUser));

        // when
        User result = oAuthLoginService.loginOrRegister(attributes);

        // then
        assertThat(result.getEmail()).isEqualTo("updated@test.com");
        assertThat(result.getNickname()).isEqualTo("업데이트된닉네임");
        assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/updated.jpg");
        // onboardingCompleted should remain as it was (not changed by loginOrRegister)
        assertThat(result.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("신규 사용자 - role이 USER로 설정됨")
    void loginOrRegister_newUser_roleIsUser() {
        // given
        OAuthAttributes attributes = new OAuthAttributes(
                OAuthProvider.KAKAO,
                "kakao-789",
                "roletest@test.com",
                "역할테스트",
                null
        );

        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "kakao-789"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = oAuthLoginService.loginOrRegister(attributes);

        // then
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("신규 사용자 - oauthProvider와 oauthId가 올바르게 설정됨")
    void loginOrRegister_newUser_setsOauthProviderAndId() {
        // given
        OAuthAttributes attributes = new OAuthAttributes(
                OAuthProvider.KAKAO,
                "provider-id-001",
                "test@example.com",
                "테스터",
                null
        );

        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "provider-id-001"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = oAuthLoginService.loginOrRegister(attributes);

        // then
        assertThat(result.getOauthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.getOauthId()).isEqualTo("provider-id-001");
    }
}