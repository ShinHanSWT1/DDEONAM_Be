package com.gorani.ecodrive.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani.ecodrive.auth.oauth.OAuth2FailureHandler;
import com.gorani.ecodrive.auth.oauth.OAuth2SuccessHandler;
import com.gorani.ecodrive.auth.token.JwtTokenProvider;
import com.gorani.ecodrive.common.config.SecurityConfig;
import com.gorani.ecodrive.common.security.CustomAccessDeniedHandler;
import com.gorani.ecodrive.common.security.CustomAuthenticationEntryPoint;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.user.domain.OAuthProvider;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import com.gorani.ecodrive.user.service.OnboardingService;
import com.gorani.ecodrive.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private OnboardingService onboardingService;

    // SecurityConfig dependencies that must be present for component scan
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private Authentication mockAuth(Long userId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "USER");
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
    }

    private User createTestUser() {
        return User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-1")
                .email("test@test.com")
                .nickname("테스트유저")
                .profileImageUrl("https://img.example.com/1.jpg")
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("GET /api/users/me - 내 정보 조회 성공")
    void getMyInfo_success() throws Exception {
        // given
        User user = createTestUser();
        given(userService.getById(1L)).willReturn(user);
        given(userService.calculateOnboardingCompleted(1L)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .with(authentication(mockAuth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.isOnboardingCompleted").value(false));
    }

    @Test
    @DisplayName("GET /api/users/me - 온보딩 완료된 사용자 정보 조회")
    void getMyInfo_withOnboardingCompleted() throws Exception {
        // given
        User user = User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-2")
                .email("completed@test.com")
                .nickname("완료유저")
                .isOnboardingCompleted(true)
                .role(UserRole.USER)
                .build();
        given(userService.getById(2L)).willReturn(user);
        given(userService.calculateOnboardingCompleted(2L)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .with(authentication(mockAuth(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOnboardingCompleted").value(true));
    }

    @Test
    @DisplayName("POST /api/users/me/vehicles - 차량 등록 성공")
    void registerMyVehicle_success() throws Exception {
        // given
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                "12가3456", 10L
        );
        OnboardingService.VehicleRegistrationResult result = new OnboardingService.VehicleRegistrationResult(
                100L, "12가3456", 10L, false
        );
        given(onboardingService.registerVehicle(eq(1L), any())).willReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/me/vehicles")
                        .with(authentication(mockAuth(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 차량 등록 성공"))
                .andExpect(jsonPath("$.data.userVehicleId").value(100))
                .andExpect(jsonPath("$.data.vehicleNumber").value("12가3456"));
    }

    @Test
    @DisplayName("POST /api/users/me/insurances - 보험 등록 성공")
    void registerMyInsurance_success() throws Exception {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                10L, "현대해상", "표준형", 500000, LocalDate.of(2025, 1, 1)
        );
        OnboardingService.InsuranceRegistrationResult result = new OnboardingService.InsuranceRegistrationResult(
                99L, 10L, 5L, 77L, true
        );
        given(onboardingService.registerInsurance(eq(1L), any())).willReturn(result);

        // when & then
        mockMvc.perform(post("/api/users/me/insurances")
                        .with(authentication(mockAuth(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 보험 등록 성공"))
                .andExpect(jsonPath("$.data.userInsuranceId").value(99))
                .andExpect(jsonPath("$.data.isOnboardingCompleted").value(true));
    }

    @Test
    @DisplayName("PATCH /api/users/me/onboarding - 온보딩 완료 처리 성공")
    void completeOnboarding_success() throws Exception {
        // given
        OnboardingService.OnboardingCompletionResult result = new OnboardingService.OnboardingCompletionResult(true);
        given(onboardingService.completeOnboarding(1L)).willReturn(result);

        // when & then
        mockMvc.perform(patch("/api/users/me/onboarding")
                        .with(authentication(mockAuth(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("온보딩 완료 처리 성공"))
                .andExpect(jsonPath("$.data.isOnboardingCompleted").value(true));
    }

    @Test
    @DisplayName("PATCH /api/users/me/onboarding - 온보딩 미완료 상태 반환")
    void completeOnboarding_notCompleted() throws Exception {
        // given
        OnboardingService.OnboardingCompletionResult result = new OnboardingService.OnboardingCompletionResult(false);
        given(onboardingService.completeOnboarding(3L)).willReturn(result);

        // when & then
        mockMvc.perform(patch("/api/users/me/onboarding")
                        .with(authentication(mockAuth(3L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOnboardingCompleted").value(false));
    }
}