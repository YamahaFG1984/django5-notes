package com.example.bookmarks.account;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByEmailIgnoreCase(String email);

    /** clean_email：邮箱是否已被（其他）用户使用 */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** 密码重置：按邮箱找所有启用的账号（Django 也允许多个账号共用一个邮箱）。 */
    List<User> findByEmailIgnoreCaseAndActiveTrue(String email);

    /** 只更新一列，不必先把实体查出来（≈ User.objects.filter(pk=...).update(last_login=now)）。 */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)   // 批量更新绕过了一级缓存，执行后清空它
    @Query("update User u set u.lastLogin = :now where u.username = :username")
    int updateLastLogin(@Param("username") String username, @Param("now") OffsetDateTime now);
}
