package com.gorani.ecodrive.user.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.user.domain.OAuthProvider;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import com.gorani.ecodrive.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private VehicleOnboardingService vehicleOnboardingService;

    @Mock
    private InsuranceOnboardingService insuranceOnboardingService;

    @InjectMocks
    private OnboardingService onboardingService;

    private User createTestUser(Long id) {
        return User.builder()
                .oauthProvider(OAuthProvider.KAKAO)
                .oauthId("kakao-" + id)
                .email("user" + id + "@test.com")
                .nickname("유저" + id)
                .isOnboardingCompleted(false)
                .role(UserRole.USER)
                .build();
    }

    // ========== registerVehicle 테스트 ==========

    @Test
    @DisplayName("registerVehicle - vehicleNumber가 null이면 INVALID_INPUT_VALUE 예외 발생")
    void registerVehicle_nullVehicleNumber_throwsInvalidInputValue() {
        // given
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                null, 1L
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerVehicle(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("registerVehicle - vehicleNumber가 빈 문자열이면 INVALID_INPUT_VALUE 예외 발생")
    void registerVehicle_blankVehicleNumber_throwsInvalidInputValue() {
        // given
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                "   ", 1L
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerVehicle(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerVehicle - vehicleModelId가 null이면 INVALID_INPUT_VALUE 예외 발생")
    void registerVehicle_nullVehicleModelId_throwsInvalidInputValue() {
        // given
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                "12가3456", null
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerVehicle(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerVehicle - 존재하지 않는 사용자면 USER_NOT_FOUND 예외 발생")
    void registerVehicle_userNotFound_throwsUserNotFound() {
        // given
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                "12가3456", 1L
        );
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> onboardingService.registerVehicle(999L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("registerVehicle - 정상 요청 시 VehicleRegistrationResult 반환")
    void registerVehicle_validRequest_returnsResult() {
        // given
        User user = createTestUser(1L);
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                "  12가3456  ", 5L
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(vehicleOnboardingService.registerVehicle(any(), anyLong(), anyString(), any())).willReturn(100L);
        given(userService.calculateOnboardingCompleted(any())).willReturn(false);

        // when
        OnboardingService.VehicleRegistrationResult result = onboardingService.registerVehicle(1L, request);

        // then
        assertThat(result.userVehicleId()).isEqualTo(100L);
        assertThat(result.vehicleNumber()).isEqualTo("12가3456"); // trimmed
        assertThat(result.vehicleModelId()).isEqualTo(5L);
        assertThat(result.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("registerVehicle - vehicleNumber 앞뒤 공백이 제거됨")
    void registerVehicle_vehicleNumberIsTrimmed() {
        // given
        User user = createTestUser(1L);
        OnboardingService.VehicleRegistrationRequest request = new OnboardingService.VehicleRegistrationRequest(
                "  34나5678  ", 2L
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(vehicleOnboardingService.registerVehicle(any(), anyLong(), anyString(), any())).willReturn(200L);
        given(userService.calculateOnboardingCompleted(any())).willReturn(true);

        // when
        OnboardingService.VehicleRegistrationResult result = onboardingService.registerVehicle(1L, request);

        // then
        assertThat(result.vehicleNumber()).isEqualTo("34나5678");
    }

    // ========== registerInsurance 테스트 ==========

    @Test
    @DisplayName("registerInsurance - insuranceCompanyName이 null이면 INVALID_INPUT_VALUE 예외 발생")
    void registerInsurance_nullCompanyName_throwsInvalidInputValue() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                1L, null, "표준형", 500000, LocalDate.now()
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerInsurance - annualPremium이 0 이하면 INVALID_INPUT_VALUE 예외 발생")
    void registerInsurance_zeroPremium_throwsInvalidInputValue() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                1L, "현대해상", "표준형", 0, LocalDate.now()
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerInsurance - annualPremium이 음수면 INVALID_INPUT_VALUE 예외 발생")
    void registerInsurance_negativePremium_throwsInvalidInputValue() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                1L, "현대해상", "표준형", -1000, LocalDate.now()
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerInsurance - annualPremium이 null이면 INVALID_INPUT_VALUE 예외 발생")
    void registerInsurance_nullPremium_throwsInvalidInputValue() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                1L, "현대해상", "표준형", null, LocalDate.now()
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerInsurance - userVehicleId가 null이면 INVALID_INPUT_VALUE 예외 발생")
    void registerInsurance_nullUserVehicleId_throwsInvalidInputValue() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                null, "현대해상", "표준형", 500000, LocalDate.now()
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerInsurance - insuranceStartedAt이 null이면 INVALID_INPUT_VALUE 예외 발생")
    void registerInsurance_nullInsuranceStartedAt_throwsInvalidInputValue() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                1L, "현대해상", "표준형", 500000, null
        );

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("registerInsurance - 존재하지 않는 사용자면 USER_NOT_FOUND 예외 발생")
    void registerInsurance_userNotFound_throwsUserNotFound() {
        // given
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                1L, "현대해상", "표준형", 500000, LocalDate.now()
        );
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> onboardingService.registerInsurance(999L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("registerInsurance - 정상 요청 시 InsuranceRegistrationResult 반환")
    void registerInsurance_validRequest_returnsResult() {
        // given
        User user = createTestUser(1L);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        OnboardingService.InsuranceRegistrationRequest request = new OnboardingService.InsuranceRegistrationRequest(
                10L, " 삼성화재 ", "표준형", 600000, startDate
        );

        InsuranceOnboardingService.InsuranceRegistrationIds ids =
                new InsuranceOnboardingService.InsuranceRegistrationIds(99L, 10L, 5L, 77L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(insuranceOnboardingService.registerInsurance(any(), any(), anyString(), any(), any(), any(), any()))
                .willReturn(ids);
        given(userService.calculateOnboardingCompleted(any())).willReturn(true);

        // when
        OnboardingService.InsuranceRegistrationResult result = onboardingService.registerInsurance(1L, request);

        // then
        assertThat(result.userInsuranceId()).isEqualTo(99L);
        assertThat(result.userVehicleId()).isEqualTo(10L);
        assertThat(result.insuranceCompanyId()).isEqualTo(5L);
        assertThat(result.insuranceContractId()).isEqualTo(77L);
        assertThat(result.isOnboardingCompleted()).isTrue();
    }

    // ========== completeOnboarding 테스트 ==========

    @Test
    @DisplayName("completeOnboarding - 존재하지 않는 사용자면 USER_NOT_FOUND 예외 발생")
    void completeOnboarding_userNotFound_throwsUserNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> onboardingService.completeOnboarding(999L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("completeOnboarding - 온보딩 미완료 상태 반환")
    void completeOnboarding_notCompleted_returnsFalseResult() {
        // given
        User user = createTestUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userService.calculateOnboardingCompleted(any())).willReturn(false);

        // when
        OnboardingService.OnboardingCompletionResult result = onboardingService.completeOnboarding(1L);

        // then
        assertThat(result.isOnboardingCompleted()).isFalse();
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("completeOnboarding - 온보딩 완료 상태 반환 및 User 업데이트")
    void completeOnboarding_completed_returnsTrueResultAndUpdatesUser() {
        // given
        User user = createTestUser(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(userService.calculateOnboardingCompleted(any())).willReturn(true);

        // when
        OnboardingService.OnboardingCompletionResult result = onboardingService.completeOnboarding(2L);

        // then
        assertThat(result.isOnboardingCompleted()).isTrue();
        assertThat(user.isOnboardingCompleted()).isTrue();
    }
}