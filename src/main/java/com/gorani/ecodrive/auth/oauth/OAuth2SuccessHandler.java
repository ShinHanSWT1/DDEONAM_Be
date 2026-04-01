package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.auth.token.JwtTokenProvider;
import com.gorani.ecodrive.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthLoginService oAuthLoginService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = Objects.requireNonNull(oAuth2User).getAttributes();

        OAuthAttributes oAuthAttributes = OAuthAttributes.ofKakao(attributes);
        User user = oAuthLoginService.loginOrRegister(oAuthAttributes);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
