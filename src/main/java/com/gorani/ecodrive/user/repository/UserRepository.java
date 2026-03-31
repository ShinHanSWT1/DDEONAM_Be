package com.gorani.ecodrive.user.repository;

import com.gorani.ecodrive.user.domain.OAuthProvider;
import com.gorani.ecodrive.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOauthProviderAndOauthId(OAuthProvider oauthProvider, String oauthId);
}
