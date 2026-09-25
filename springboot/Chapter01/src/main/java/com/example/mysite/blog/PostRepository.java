package com.example.mysite.blog;

import java.util.List;
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
 * Spring Data 会根据方法名自动生成查询；default 方法用来封装常用条件，
 * 起到 Django 自定义管理器 {@code Post.published} 的作用。
 */
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /** @EntityGraph 让作者随文章一起查出来（JOIN），避免模板里逐条懒加载 —— 相当于 select_related('author')。 */
    @EntityGraph(attributePaths = "author")
    List<Post> findByStatusOrderByPublishDesc(PostStatus status);

    @EntityGraph(attributePaths = "author")
    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    long countByStatus(PostStatus status);

    /** 后台列表页：动态条件 + 分页，同样顺带查出作者。 */
    @Override
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    /** Post.published.all() */
    default List<Post> findPublished() {
        return findByStatusOrderByPublishDesc(PostStatus.PUBLISHED);
    }

    /** get_object_or_404(Post, id=id, status=Post.Status.PUBLISHED) 的前半部分 */
    default Optional<Post> findPublishedById(Long id) {
        return findByIdAndStatus(id, PostStatus.PUBLISHED);
    }

    /** 某个用户收藏的文章，一条 JPQL 完成（原书用了 id__in 子查询）。 */
    @Query("""
            select p from FavouritePost f
              join f.post p
              join fetch p.author
            where f.user.id = :userId
            order by f.created desc
            """)
    List<Post> findFavouritesOf(@Param("userId") Long userId);
}
