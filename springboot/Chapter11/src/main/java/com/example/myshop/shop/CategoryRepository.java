package com.example.myshop.shop;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * get_object_or_404(Category, translations__language_code=language, translations__slug=category_slug)
     * 按“当前语言的 slug”查找 —— /es/te/ 和 /en/tea/ 找到的是同一个分类。
     */
    @Query("select c from Category c join c.translations t where t.languageCode = :language and t.slug = :slug")
    Optional<Category> findByTranslatedSlug(@Param("language") String language, @Param("slug") String slug);

    @Query("select count(t) > 0 from CategoryTranslation t where t.languageCode = :language and t.slug = :slug")
    boolean existsTranslatedSlug(@Param("language") String language, @Param("slug") String slug);

    /** 按当前语言的名字排序：排序依赖译文，在内存里做（分类很少，没问题） */
    default List<Category> findAllOrderedByName() {
        return findAll().stream().sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }
}
