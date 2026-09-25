package com.example.educa.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourseIdOrderByOrderAsc(Long courseId);

    /** get_object_or_404(Module, id=module_id, course__owner=request.user) */
    @EntityGraph(attributePaths = "course")
    Optional<Module> findByIdAndCourseOwnerId(Long id, Long ownerId);

    /** Module.objects.filter(id=id, course__owner=request.user).update(order=order)：只能改自己课程的模块 */
    @Modifying
    @Query("update Module m set m.order = :order where m.id = :id and m.course.owner.id = :ownerId")
    int updateOrder(@Param("id") Long id, @Param("order") int order, @Param("ownerId") Long ownerId);
}
