package com.example.mysite.blog;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

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

    /** 分页版本：传入 Pageable，返回 Page（≈ Paginator.page(n)），会额外发一条 count 查询。 */
    @EntityGraph(attributePaths = "author")
    Page<Post> findByStatusOrderByPublishDesc(PostStatus status, Pageable pageable);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    long countByStatus(PostStatus status);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    /**
     * publish__year / publish__month / publish__day 的等价写法。
     * 用“当天 0 点 ≤ publish &lt; 次日 0 点”的范围条件，而不是对列取 EXTRACT(YEAR ...)，这样能用上 publish 索引。
     */
    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p
            where p.status = :status and p.slug = :slug
              and p.publish >= :start and p.publish < :end
            """)
    Optional<Post> findBySlugPublishedBetween(@Param("status") PostStatus status, @Param("slug") String slug,
                                              @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    /** unique_for_date 校验用：同一天有没有别的文章用了这个 slug。 */
    @Query("""
            select count(p) > 0 from Post p
            where p.slug = :slug and p.publish >= :start and p.publish < :end
              and (:excludeId is null or p.id <> :excludeId)
            """)
    boolean slugTakenBetween(@Param("slug") String slug, @Param("start") OffsetDateTime start,
                             @Param("end") OffsetDateTime end, @Param("excludeId") Long excludeId);

    /** Post.published.all()，分页 */
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
