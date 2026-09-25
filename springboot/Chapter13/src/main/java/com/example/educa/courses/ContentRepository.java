package com.example.educa.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /** 连同条目一起取出：Hibernate 用 LEFT JOIN 把 courses_item 和四张子表连起来，直接构造出正确的子类 */
    @EntityGraph(attributePaths = "item")
    List<Content> findByModuleIdOrderByOrderAsc(Long moduleId);

    @EntityGraph(attributePaths = {"item", "module"})
    Optional<Content> findByIdAndModuleCourseOwnerId(Long id, Long ownerId);

    /** 编辑某个条目时，找到它所在的内容（并确认属于当前讲师） */
    @EntityGraph(attributePaths = "item")
    Optional<Content> findByItemIdAndModuleIdAndModuleCourseOwnerId(Long itemId, Long moduleId, Long ownerId);

    @Modifying
    @Query("update Content c set c.order = :order where c.id = :id and c.module.course.owner.id = :ownerId")
    int updateOrder(@Param("id") Long id, @Param("order") int order, @Param("ownerId") Long ownerId);
}
