package com.example.educa.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.educa.courses.catalog.SubjectSummary;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /** Meta.ordering = ['title'] */
    List<Subject> findAllByOrderByTitleAsc();

    Optional<Subject> findBySlug(String slug);

    /** ≈ Subject.objects.annotate(total_courses=Count('courses'))（Subject 上没有 courses 集合，用实体 join + on） */
    @Query("""
            select new com.example.educa.courses.catalog.SubjectSummary(s.id, s.title, s.slug, count(c))
            from Subject s left join Course c on c.subject = s
            group by s.id, s.title, s.slug
            order by s.title
            """)
    List<SubjectSummary> summaries();

    /** API 用的分页版本；带 group by 的查询要自己给出 countQuery */
    @Query(value = """
            select new com.example.educa.courses.catalog.SubjectSummary(s.id, s.title, s.slug, count(c))
            from Subject s left join Course c on c.subject = s
            group by s.id, s.title, s.slug
            order by s.title
            """, countQuery = "select count(s) from Subject s")
    Page<SubjectSummary> summaries(Pageable pageable);

    @Query("""
            select new com.example.educa.courses.catalog.SubjectSummary(s.id, s.title, s.slug, count(c))
            from Subject s left join Course c on c.subject = s
            where s.id = :id
            group by s.id, s.title, s.slug
            """)
    Optional<SubjectSummary> summary(@Param("id") Long id);

    /**
     * 按主键“有则更新、无则插入”（fixture 里带着 pk）。MERGE 是 SQL 标准语法，H2 和 PostgreSQL 15+ 都支持。
     */
    @Modifying(clearAutomatically = true)
    @NativeQuery("""
            MERGE INTO courses_subject s
            USING (VALUES (CAST(:id AS BIGINT), CAST(:title AS VARCHAR(200)), CAST(:slug AS VARCHAR(200)))) AS v(id, title, slug)
            ON s.id = v.id
            WHEN MATCHED THEN UPDATE SET title = v.title, slug = v.slug
            WHEN NOT MATCHED THEN INSERT (id, title, slug) VALUES (v.id, v.title, v.slug)
            """)
    void upsert(@Param("id") Long id, @Param("title") String title, @Param("slug") String slug);
}
