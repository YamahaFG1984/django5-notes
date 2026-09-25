package com.example.bookmarks.images;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Page<Image> findAllByOrderByCreatedDesc(Pageable pageable);

    List<Image> findByUserIdOrderByCreatedDesc(Long userId);

    /** request.user.images_created.count */
    long countByUserId(Long userId);

    /** 详情页需要点赞的人，一次查出来 */
    @EntityGraph(attributePaths = "usersLike")
    Optional<Image> findWithLikesByIdAndSlug(Long id, String slug);

    /** 用 SQL 直接判断，不必把所有点赞用户加载进内存（≈ image.users_like.filter(id=...).exists()） */
    @Query("select count(i) > 0 from Image i join i.usersLike u where i.id = :imageId and u.id = :userId")
    boolean isLikedBy(@Param("imageId") Long imageId, @Param("userId") Long userId);
}
