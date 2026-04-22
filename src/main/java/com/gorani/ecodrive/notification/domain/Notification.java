package com.gorani.ecodrive.notification.domain;

import com.gorani.ecodrive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 255)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Notification create(User user, NotificationType type, String title, String body) {
        Notification n = new Notification();
        n.user = user;
        n.type = type;
        n.title = title;
        n.body = body;
        n.isRead = false;
        n.createdAt = LocalDateTime.now();
        return n;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
