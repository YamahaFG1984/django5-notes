package com.example.educa.account;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 登录时连同组和组的权限一起取出 */
    @EntityGraph(attributePaths = {"groups", "groups.permissions"})
    Optional<User> findWithGroupsByUsername(String username);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * 注册时间早于 cutoff、有邮箱、一门课都没选的用户（enroll_reminder 命令）。
     * ≈ User.objects.annotate(course_count=Count('courses_joined')).filter(course_count=0, date_joined__date__lte=...)
     */
    @Query("""
            select u from User u
            where u.dateJoined < :cutoff and u.email <> '' and u.active = true
              and not exists (select 1 from Course c join c.students s where s = u)
            order by u.id
            """)
    List<User> findNotEnrolledJoinedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
