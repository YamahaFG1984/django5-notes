package com.example.mysite.blog;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 仓库接口 ≈ Django 的 Manager（Post.objects）。
 * default 方法用来封装常用条件，起到自定义管理器 {@code Post.published} 的作用。
 */
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @EntityGraph(attributePaths = "author")
    Page<Post> findByStatusOrderByPublishDesc(PostStatus status, Pageable pageable);

    /** post_list = post_list.filter(tags__in=[tag]) */
    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p join p.tags t
            where p.status = :status and t = :tag
            order by p.publish desc
            """)
    Page<Post> findByStatusAndTag(@Param("status") PostStatus status, @Param("tag") Tag tag, Pageable pageable);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    /** 后台编辑页需要一起取出作者和标签。 */
    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Post> findWithTagsById(Long id);

    long countByStatus(PostStatus status);

    List<Post> findByStatusOrderByPublishDesc(PostStatus status, Limit limit);

    List<Post> findByStatusOrderByUpdatedDesc(PostStatus status);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p
            where p.status = :status and p.slug = :slug
              and p.publish >= :start and p.publish < :end
            """)
    Optional<Post> findBySlugPublishedBetween(@Param("status") PostStatus status, @Param("slug") String slug,
                                              @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
            select count(p) > 0 from Post p
            where p.slug = :slug and p.publish >= :start and p.publish < :end
              and (:excludeId is null or p.id <> :excludeId)
            """)
    boolean slugTakenBetween(@Param("slug") String slug, @Param("start") OffsetDateTime start,
                             @Param("end") OffsetDateTime end, @Param("excludeId") Long excludeId);

    /**
     * 相似文章：和当前文章共享标签越多越靠前，其次按发布时间。对应书中的
     * <pre>
     * Post.published.filter(tags__in=post_tags_ids).exclude(id=post.id)
     *     .annotate(same_tags=Count('tags')).order_by('-same_tags', '-publish')[:4]
     * </pre>
     */
    @Query("""
            select p from Post p join p.tags t
            where p.status = :status and p.id <> :postId
              and t in (select t2 from Post p2 join p2.tags t2 where p2.id = :postId)
            group by p
            order by count(t) desc, p.publish desc
            """)
    List<Post> findSimilar(@Param("status") PostStatus status, @Param("postId") Long postId, Limit limit);

    /** get_most_commented_posts：annotate(total_comments=Count('comments')).order_by('-total_comments') */
    @Query("""
            select p from Post p left join p.comments c
            where p.status = :status
            group by p
            order by count(c) desc, p.publish desc
            """)
    List<Post> findMostCommented(@Param("status") PostStatus status, Limit limit);

    /**
     * 三元组（trigram）相似度搜索，需要 PostgreSQL 的 pg_trgm 扩展（见 V5 迁移）。
     * JPQL 不认识 similarity() 这种数据库专有函数，所以用原生 SQL；
     * 对应 Post.published.annotate(similarity=TrigramSimilarity('title', query)).filter(similarity__gt=0.1)。
     */
    @Query(value = """
            select p.* from blog_post p
            where p.status = 'PB' and similarity(p.title, :query) > 0.1
            order by similarity(p.title, :query) desc
            """, nativeQuery = true)
    List<Post> searchByTrigram(@Param("query") String query);

    default Page<Post> findPublished(Pageable pageable) {
        return findByStatusOrderByPublishDesc(PostStatus.PUBLISHED, pageable);
    }

    default Optional<Post> findPublishedById(Long id) {
        return findByIdAndStatus(id, PostStatus.PUBLISHED);
    }

    default Optional<Post> findPublishedBySlugAndDate(String slug, LocalDate date) {
        return findBySlugPublishedBetween(PostStatus.PUBLISHED, slug, startOf(date), startOf(date.plusDays(1)));
    }

    default boolean slugTakenOn(String slug, LocalDate date, Long excludeId) {
        return slugTakenBetween(slug, startOf(date), startOf(date.plusDays(1)), excludeId);
    }

    private static OffsetDateTime startOf(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}
