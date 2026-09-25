package com.example.bookmarks.account;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAuthRepository extends JpaRepository<UserSocialAuth, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<UserSocialAuth> findByProviderAndUid(String provider, String uid);
}
