package com.example.educa.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.educa.courses.catalog.CourseSummary;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** OwnerMixin.get_queryset：qs.filter(owner=self.request.user) */
    @EntityGraph(attributePaths = {"subject", "modules"})
    List<Course> findByOwnerIdOrderByCreatedDesc(Long ownerId);

    Optional<Course> findByIdAndOwnerId(Long id, Long ownerId);

    // ---------------------------------------------------------------- 公开目录（第 14 章）

    /** CourseDetailView：课程详情要显示学科、讲师和模块数 */
    @EntityGraph(attributePaths = {"subject", "owner", "modules"})
    Optional<Course> findWithDetailsBySlug(String slug);

    /**
     * 课程列表 + 模块数，≈ Course.objects.annotate(total_modules=Count('modules'))。
     * 直接查成 DTO（record），结果可以放进缓存，不会带着 Hibernate 代理对象。
     */
    @Query("""
            select new com.example.educa.courses.catalog.CourseSummary(
                c.id, c.title, c.slug, s.title, s.slug, o.username, o.firstName, o.lastName, count(m))
            from Course c join c.subject s join c.owner o left join c.modules m
            where :subjectSlug is null or s.slug = :subjectSlug
            group by c.id, c.title, c.slug, s.title, s.slug, o.username, o.firstName, o.lastName, c.created
            order by c.created desc
            """)
    List<CourseSummary> summaries(@Param("subjectSlug") String subjectSlug);

    // ---------------------------------------------------------------- 学生（第 14 章）

    boolean existsByIdAndStudentsId(Long id, Long studentId);

    /** StudentCourseListView：qs.filter(students__in=[self.request.user]) */
    List<Course> findByStudentsIdOrderByCreatedDesc(Long studentId);

    @EntityGraph(attributePaths = "modules")
    Optional<Course> findByIdAndStudentsId(Long id, Long studentId);

    /** 课程连同模块一起取出 */
    @EntityGraph(attributePaths = {"subject", "modules"})
    Optional<Course> findWithModulesById(Long id);
}
