package com.gorani.ecodrive.user.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.OnboardingService;
import com.gorani.ecodrive.user.service.UserService;
import com.gorani.ecodrive.vehicle.service.UserVehicleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;
    private final UserVehicleQueryService userVehicleQueryService;

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
                isOnboardingCompleted,
                user.getAge()
        ));
    }

    public record UserMeResponse(
            Long id,
            String email,
            String nickname,
            String profileImageUrl,
            String role,
            boolean isOnboardingCompleted,
            Integer age
    ) {
    }

    @GetMapping("/me/vehicles")
    public ApiResponse<VehicleListResponse> getMyVehicles(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        User user = userService.getById(principal.getUserId());
        List<VehicleResponse> vehicles = userVehicleQueryService.getMyVehicles(principal.getUserId())
                .stream()
                .map(vehicle -> new VehicleResponse(
                        vehicle.userVehicleId(),
                        vehicle.vehicleNumber(),
                        vehicle.vehicleModelId(),
                        vehicle.manufacturer(),
                        vehicle.modelName(),
                        vehicle.modelYear(),
                        vehicle.fuelType(),
                        vehicle.status(),
                        vehicle.registeredAt(),
                        vehicle.userVehicleId().equals(user.getRepresentativeUserVehicleId())
                ))
                .toList();

        return ApiResponse.success(new VehicleListResponse(vehicles));
    }

    @PatchMapping("/me/representative-vehicle")
    public ApiResponse<RepresentativeVehicleResponse> updateRepresentativeVehicle(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody RepresentativeVehicleRequest request
    ) {
        userService.updateRepresentativeVehicle(principal.getUserId(), request.userVehicleId());

        return ApiResponse.success(
                "대표 차량 변경 성공",
                new RepresentativeVehicleResponse(request.userVehicleId())
        );
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

    @PostMapping("/me/onboarding/insurances")
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

    public record VehicleResponse(
            Long userVehicleId,
            String vehicleNumber,
            Long vehicleModelId,
            String manufacturer,
            String modelName,
            short modelYear,
            String fuelType,
            String status,
            LocalDateTime registeredAt,
            boolean isRepresentative
    ) {
    }

    public record VehicleListResponse(List<VehicleResponse> vehicles) {
    }

    public record RepresentativeVehicleRequest(Long userVehicleId) {
    }

    public record RepresentativeVehicleResponse(Long userVehicleId) {
    }
}
