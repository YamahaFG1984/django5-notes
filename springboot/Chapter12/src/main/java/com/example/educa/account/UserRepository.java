package com.example.educa.account;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 登录时连同组和组的权限一起取出 */
    @EntityGraph(attributePaths = {"groups", "groups.permissions"})
    Optional<User> findWithGroupsByUsername(String username);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
