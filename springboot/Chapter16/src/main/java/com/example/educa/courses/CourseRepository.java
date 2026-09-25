package com.example.educa.courses;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.educa.courses.api.PopularCourse;
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

    // ---------------------------------------------------------------- API（第 15 章）

    /** Course.objects.all()：分页查询不抓取 modules 集合，由 default_batch_fetch_size 批量加载（≈ prefetch_related） */
    @EntityGraph(attributePaths = {"subject", "owner"})
    Page<Course> findAllByOrderByCreatedDesc(Pageable pageable);

    /**
     * 每个学科按学生人数排序的课程（SubjectSerializer.get_popular_courses）。
     * 书中对每个学科各查一次（N+1）；这里对一页里的所有学科只查一次，再在 Java 里每个学科取前 3 个。
     */
    @Query("""
            select new com.example.educa.courses.api.PopularCourse(s.id, c.title, count(st))
            from Course c join c.subject s left join c.students st
            where s.id in :subjectIds
            group by s.id, c.id, c.title
            order by count(st) desc, c.title
            """)
    List<PopularCourse> popularCourses(@Param("subjectIds") Collection<Long> subjectIds);

    /** 课程连同模块一起取出 */
    @EntityGraph(attributePaths = {"subject", "modules"})
    Optional<Course> findWithModulesById(Long id);
}
