package com.gorani.ecodrive.auth.oauth;

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
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2FailureHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException exception;

    @InjectMocks
    private OAuth2FailureHandler oauth2FailureHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oauth2FailureHandler, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 시 프론트엔드 /login?error=oauth_login_failed로 리다이렉트")
    void onAuthenticationFailure_redirectsToLoginWithError() throws IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/login/oauth2/code/kakao");
        given(request.getParameter("error")).willReturn("access_denied");
        given(exception.getMessage()).willReturn("인증 실패");

        // when
        oauth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertThat(redirectUrl).contains("/login");
        assertThat(redirectUrl).contains("error=oauth_login_failed");
        assertThat(redirectUrl).startsWith("http://localhost:3000");
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 시 리다이렉트 URL에 프론트엔드 URL이 포함됨")
    void onAuthenticationFailure_redirectUrlContainsFrontendUrl() throws IOException {
        // given
        ReflectionTestUtils.setField(oauth2FailureHandler, "frontendUrl", "https://dev-gorani.lab.terminal-lab.kr");
        given(request.getRequestURI()).willReturn("/api/login/oauth2/code/kakao");
        given(request.getParameter("error")).willReturn(null);
        given(exception.getMessage()).willReturn("error");

        // when
        oauth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        assertThat(redirectCaptor.getValue()).startsWith("https://dev-gorani.lab.terminal-lab.kr");
        assertThat(redirectCaptor.getValue()).contains("error=oauth_login_failed");
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 - 예외 메시지와 무관하게 항상 고정 에러 파라미터로 리다이렉트")
    void onAuthenticationFailure_alwaysUsesFixedErrorParam() throws IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/oauth2/authorization/kakao");
        given(request.getParameter("error")).willReturn("server_error");
        given(exception.getMessage()).willReturn("서버 오류");

        // when
        oauth2FailureHandler.onAuthenticationFailure(request, response, exception);

        // then
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertThat(redirectUrl).contains("error=oauth_login_failed");
        assertThat(redirectUrl).doesNotContain("server_error");
    }
}