package com.gorani.ecodrive.notification.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.notification.domain.Notification;
import com.gorani.ecodrive.notification.domain.NotificationType;
import com.gorani.ecodrive.notification.dto.NotificationResponse;
import com.gorani.ecodrive.notification.repository.NotificationRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Transactional
    public void save(Long userId, NotificationType type, String title, String body) {
        User user = userService.getById(userId);
        Notification notification = Notification.create(user, type, title, body);
        notificationRepository.save(notification);
        log.info("알림 저장 완료. userId={}, type={}", userId, type);
    }

    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("전체 읽음 처리 완료. userId={}", userId);
    }
}
