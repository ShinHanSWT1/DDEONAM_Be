package com.gorani.ecodrive.user.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.OnboardingService;
import com.gorani.ecodrive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        User user = userService.getById(principal.getUserId());
        boolean isOnboardingCompleted = userService.calculateOnboardingCompleted(principal.getUserId());

        return ApiResponse.success("내 정보 조회 성공", new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole().name(),
                isOnboardingCompleted
        ));
    }

    public record UserMeResponse(
            Long id,
            String email,
            String nickname,
            String profileImageUrl,
            String role,
            boolean isOnboardingCompleted
    ) {
    }

    @PostMapping("/me/vehicles")
    public ApiResponse<OnboardingService.VehicleRegistrationResult> registerMyVehicle(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody OnboardingService.VehicleRegistrationRequest request
    ) {
        return ApiResponse.success(
                "내 차량 등록 성공",
                onboardingService.registerVehicle(principal.getUserId(), request)
        );
    }

    @PostMapping("/me/insurances")
    public ApiResponse<OnboardingService.InsuranceRegistrationResult> registerMyInsurance(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody OnboardingService.InsuranceRegistrationRequest request
    ) {
        return ApiResponse.success(
                "내 보험 등록 성공",
                onboardingService.registerInsurance(principal.getUserId(), request)
        );
    }

    @PatchMapping("/me/onboarding")
    public ApiResponse<OnboardingService.OnboardingCompletionResult> completeOnboarding(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(
                "온보딩 완료 처리 성공",
                onboardingService.completeOnboarding(principal.getUserId())
        );
    }

//    @PostMapping("/{userId}/profile-image")
//    public ResponseEntity<String> uploadProfileImage(
//            @PathVariable Long userId,
//            @RequestPart("file") MultipartFile file
//    ) {
//        String imageUrl = userService.uploadProfileImage(userId, file);
//        return ResponseEntity.ok(imageUrl);
//    }
}
