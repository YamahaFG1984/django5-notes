package com.example.educa.courses;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /** 连同条目一起取出：Hibernate 用 LEFT JOIN 把 courses_item 和四张子表连起来，直接构造出正确的子类 */
    @EntityGraph(attributePaths = "item")
    List<Content> findByModuleIdOrderByOrderAsc(Long moduleId);
}
