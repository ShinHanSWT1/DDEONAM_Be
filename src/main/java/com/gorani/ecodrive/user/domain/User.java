package com.gorani.ecodrive.user.domain;

import com.gorani.ecodrive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String oauthId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "age")
    private Integer age;

    @Builder
    public User(
            OAuthProvider oauthProvider,
            String oauthId,
            String email,
            String nickname,
            String profileImageUrl,
            UserRole role
    ) {
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}