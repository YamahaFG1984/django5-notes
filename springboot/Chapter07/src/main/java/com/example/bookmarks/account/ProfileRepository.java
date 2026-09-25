package com.example.bookmarks.account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    List<Profile> findByUserIdIn(Collection<Long> userIds);

    /** 用户列表页：User.objects.filter(is_active=True)，连同资料一起取（join fetch ≈ select_related） */
    @Query("select p from Profile p join fetch p.user u where u.active = true order by u.username")
    List<Profile> findAllOfActiveUsers();
}
