package com.example.educa.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** OwnerMixin.get_queryset：qs.filter(owner=self.request.user) */
    @EntityGraph(attributePaths = {"subject", "modules"})
    List<Course> findByOwnerIdOrderByCreatedDesc(Long ownerId);

    Optional<Course> findByIdAndOwnerId(Long id, Long ownerId);

    /** 课程连同模块一起取出 */
    @EntityGraph(attributePaths = {"subject", "modules"})
    Optional<Course> findWithModulesById(Long id);
}
