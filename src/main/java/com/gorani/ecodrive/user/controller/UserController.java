package com.gorani.ecodrive.user.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        User user = userService.getById(principal.getUserId());

        return ApiResponse.success("내 정보 조회 성공", new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole().name()
        ));
    }

    public record UserMeResponse(
            Long id,
            String email,
            String nickname,
            String profileImageUrl,
            String role
    ) {
    }

    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<String> uploadProfileImage(
            @PathVariable Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        String imageUrl = userService.uploadProfileImage(userId, file);
        return ResponseEntity.ok(imageUrl);
    }
}