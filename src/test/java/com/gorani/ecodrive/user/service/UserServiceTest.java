package com.gorani.ecodrive.user.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.infra.s3.S3Service;
import com.gorani.ecodrive.insurance.repository.UserInsuranceRepository;
import com.gorani.ecodrive.user.domain.OAuthProvider;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.domain.UserRole;
import com.gorani.ecodrive.user.repository.UserRepository;
import com.gorani.ecodrive.vehicle.repository.UserVehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private UserVehicleRepository userVehicleRepository;

    @Mock
    private UserInsuranceRepository userInsuranceRepository;

    @InjectMocks
    private UserService userService;

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

    @Test
    @DisplayName("getById - 존재하는 사용자 반환")
    void getById_existingUser_returnsUser() {
        // given
        User user = createTestUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        User result = userService.getById(1L);

        // then
        assertThat(result).isEqualTo(user);
        assertThat(result.getEmail()).isEqualTo("user1@test.com");
    }

    @Test
    @DisplayName("getById - 존재하지 않는 사용자 -> CustomException(USER_NOT_FOUND) 발생")
    void getById_nonExistentUser_throwsCustomException() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("calculateOnboardingCompleted - 차량도 있고 보험도 있으면 true")
    void calculateOnboardingCompleted_bothVehicleAndInsurance_returnsTrue() {
        // given
        given(userVehicleRepository.existsByUserId(1L)).willReturn(true);
        given(userInsuranceRepository.existsByUserVehicleUserId(1L)).willReturn(true);

        // when
        boolean result = userService.calculateOnboardingCompleted(1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("calculateOnboardingCompleted - 차량은 있지만 보험이 없으면 false")
    void calculateOnboardingCompleted_vehicleWithoutInsurance_returnsFalse() {
        // given
        given(userVehicleRepository.existsByUserId(2L)).willReturn(true);
        given(userInsuranceRepository.existsByUserVehicleUserId(2L)).willReturn(false);

        // when
        boolean result = userService.calculateOnboardingCompleted(2L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("calculateOnboardingCompleted - 차량도 없고 보험도 없으면 false")
    void calculateOnboardingCompleted_neitherVehicleNorInsurance_returnsFalse() {
        // given
        given(userVehicleRepository.existsByUserId(3L)).willReturn(false);
        given(userInsuranceRepository.existsByUserVehicleUserId(3L)).willReturn(false);

        // when
        boolean result = userService.calculateOnboardingCompleted(3L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("calculateOnboardingCompleted - 차량이 없으면 보험 조회 결과와 무관하게 false")
    void calculateOnboardingCompleted_noVehicle_returnsFalseRegardlessOfInsurance() {
        // given
        given(userVehicleRepository.existsByUserId(4L)).willReturn(false);
        given(userInsuranceRepository.existsByUserVehicleUserId(4L)).willReturn(true);

        // when
        boolean result = userService.calculateOnboardingCompleted(4L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("uploadProfileImage - 존재하지 않는 사용자 -> CustomException(USER_NOT_FOUND) 발생")
    void uploadProfileImage_nonExistentUser_throwsCustomException() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());

        // when & then
        assertThatThrownBy(() -> userService.uploadProfileImage(999L, file))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
        verify(s3Service, never()).upload(any(), anyString());
    }

    @Test
    @DisplayName("uploadProfileImage - 정상 업로드 시 프로필 이미지 URL 반환")
    void uploadProfileImage_validUser_uploadsAndUpdatesProfile() {
        // given
        User user = createTestUser(5L);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "imgdata".getBytes());
        String expectedUrl = "https://s3.example.com/users/profile/uuid.jpg";

        given(userRepository.findById(5L)).willReturn(Optional.of(user));
        given(s3Service.upload(file, "users/profile")).willReturn(expectedUrl);

        // when
        String result = userService.uploadProfileImage(5L, file);

        // then
        assertThat(result).isEqualTo(expectedUrl);
        assertThat(user.getProfileImageUrl()).isEqualTo(expectedUrl);
        verify(s3Service).upload(file, "users/profile");
    }
}