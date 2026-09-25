package com.example.mysite.blog;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    /** post.comments.filter(active=True)，按 Meta.ordering = ['created'] 排序 */
    List<Comment> findByPostAndActiveTrueOrderByCreatedAsc(Post post);

    long countByActive(boolean active);

    @Override
    @EntityGraph(attributePaths = "post")
    Page<Comment> findAll(Specification<Comment> spec, Pageable pageable);
}
