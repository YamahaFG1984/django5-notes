package com.example.educa.courses;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    /** 课程连同模块一起取出 */
    @EntityGraph(attributePaths = {"subject", "modules"})
    Optional<Course> findWithModulesById(Long id);
}
