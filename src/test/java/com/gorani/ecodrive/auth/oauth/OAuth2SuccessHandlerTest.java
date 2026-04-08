package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.auth.token.JwtTokenProvider;
import com.gorani.ecodrive.user.domain.OAuthProvider;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private OAuthLoginService oAuthLoginService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2SuccessHandler oauth2SuccessHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oauth2SuccessHandler, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 이메일 존재 시 토큰 발급 후 리다이렉트")
    void onAuthenticationSuccess_withValidEmail_redirectsWithToken() throws IOException {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "테스트유저");
        properties.put("profile_image", "https://example.com/profile.jpg");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123L);
        attributes.put("kakao_account", kakaoAccount);
        attributes.put("properties", properties);

        User mockUser = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("123")
                .email("test@kakao.com")
                .nickname("테스트유저")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(attributes);
        given(oAuthLoginService.loginOrRegister(any(OAuthAttributes.class))).willReturn(mockUser);
        given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("test-jwt-token");

        // when
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertThat(redirectUrl).contains("/oauth/callback");
        assertThat(redirectUrl).contains("accessToken=test-jwt-token");
        verify(oAuthLoginService).loginOrRegister(any(OAuthAttributes.class));
        verify(jwtTokenProvider).createAccessToken(any(), any());
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 이메일이 null이면 email_required 오류로 리다이렉트")
    void onAuthenticationSuccess_withNullEmail_redirectsWithEmailRequiredError() throws IOException {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "테스트유저");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 456L);
        attributes.put("kakao_account", null);
        attributes.put("properties", properties);

        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(attributes);

        // when
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertThat(redirectUrl).contains("/login");
        assertThat(redirectUrl).contains("error=email_required");
        verify(oAuthLoginService, never()).loginOrRegister(any());
        verify(jwtTokenProvider, never()).createAccessToken(any(), any());
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 이메일이 빈 문자열이면 email_required 오류로 리다이렉트")
    void onAuthenticationSuccess_withBlankEmail_redirectsWithEmailRequiredError() throws IOException {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "   ");

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "테스트유저");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 789L);
        attributes.put("kakao_account", kakaoAccount);
        attributes.put("properties", properties);

        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(attributes);

        // when
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertThat(redirectUrl).contains("error=email_required");
        verify(oAuthLoginService, never()).loginOrRegister(any());
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - 프론트엔드 URL이 리다이렉트에 포함됨")
    void onAuthenticationSuccess_redirectUrlContainsFrontendUrl() throws IOException {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "user@test.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 100L);
        attributes.put("kakao_account", kakaoAccount);
        attributes.put("properties", null);

        User mockUser = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("100")
                .email("user@test.com")
                .nickname("카카오사용자")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();

        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(attributes);
        given(oAuthLoginService.loginOrRegister(any())).willReturn(mockUser);
        given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("some-token");

        // when
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        assertThat(redirectCaptor.getValue()).startsWith("http://localhost:3000");
    }
}