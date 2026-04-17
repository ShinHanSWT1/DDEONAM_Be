package com.gorani.ecodrive.notification.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.notification.dto.NotificationResponse;
import com.gorani.ecodrive.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("알림 목록 조회. userId={}", principal.getUserId());
        return ApiResponse.success("알림 목록 조회 성공",
                notificationService.getMyNotifications(principal.getUserId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("읽지 않은 알림 수 조회. userId={}", principal.getUserId());
        return ApiResponse.success("읽지 않은 알림 수 조회 성공",
                notificationService.getUnreadCount(principal.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        log.info("알림 읽음 처리. userId={}, notificationId={}", principal.getUserId(), notificationId);
        notificationService.markAsRead(principal.getUserId(), notificationId);
        return ApiResponse.success("알림 읽음 처리 성공", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("전체 알림 읽음 처리. userId={}", principal.getUserId());
        notificationService.markAllAsRead(principal.getUserId());
        return ApiResponse.success("전체 알림 읽음 처리 성공", null);
    }
}
