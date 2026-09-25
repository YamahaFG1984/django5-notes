package com.example.bookmarks.actions;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActionRepository extends JpaRepository<Action, Long> {

    /** Action.objects.exclude(user=request.user).select_related('user')[:10] */
    @EntityGraph(attributePaths = "user")
    List<Action> findByUserIdNotOrderByCreatedDesc(Long userId, Limit limit);

    /** actions.filter(user_id__in=following_ids) */
    @EntityGraph(attributePaths = "user")
    List<Action> findByUserIdInOrderByCreatedDesc(Collection<Long> userIds, Limit limit);

    /** create_action 里“最近一分钟有没有相同动态”的检查 */
    @Query("""
            select count(a) > 0 from Action a
            where a.user.id = :userId and a.verb = :verb and a.created >= :since
              and ((:targetType is null and a.targetType is null)
                   or (a.targetType = :targetType and a.targetId = :targetId))
            """)
    boolean existsSimilar(@Param("userId") Long userId, @Param("verb") String verb,
                          @Param("targetType") String targetType, @Param("targetId") Long targetId,
                          @Param("since") OffsetDateTime since);
}
