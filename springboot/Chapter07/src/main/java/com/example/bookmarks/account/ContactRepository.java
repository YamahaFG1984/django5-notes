package com.example.bookmarks.account;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByUserFromIdAndUserToId(Long userFromId, Long userToId);

    /** user.followers.count */
    long countByUserToId(Long userToId);

    /** request.user.following.values_list('id', flat=True) */
    @Query("select c.userTo.id from Contact c where c.userFrom.id = :userId")
    List<Long> findFollowingIds(@Param("userId") Long userId);

    /** Contact.objects.filter(user_from=..., user_to=...).delete() —— 一条 DELETE 语句 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Contact c where c.userFrom.id = :fromId and c.userTo.id = :toId")
    int unfollow(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
