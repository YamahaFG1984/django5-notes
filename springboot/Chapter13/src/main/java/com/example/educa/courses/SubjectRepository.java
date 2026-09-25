package com.example.educa.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /** Meta.ordering = ['title'] */
    List<Subject> findAllByOrderByTitleAsc();

    Optional<Subject> findBySlug(String slug);

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
