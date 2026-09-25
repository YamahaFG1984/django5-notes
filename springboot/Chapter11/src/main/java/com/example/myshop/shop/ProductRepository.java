package com.example.myshop.shop;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 注意实体图都用了 LOAD 类型：默认的 FETCH 类型会把图里<b>没列出</b>的属性一律当作 LAZY，
 * 包括映射成 EAGER 的 translations —— 结果模板里访问商品名称时抛 LazyInitializationException。
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByAvailableTrue();

    List<Product> findByAvailableTrueAndCategory(Category category);

    /** 商品详情：id + 当前语言的 slug + 已上架 */
    @EntityGraph(attributePaths = "category", type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
            select p from Product p join p.translations t
            where p.id = :id and p.available = true and t.languageCode = :language and t.slug = :slug
            """)
    Optional<Product> findAvailableByTranslatedSlug(@Param("id") Long id, @Param("language") String language,
                                                    @Param("slug") String slug);

    List<Product> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "category", type = EntityGraph.EntityGraphType.LOAD)
    List<Product> findAllBy();

    /** 翻译后的名字只能在取出后排序（parler 模型上的 ordering 也因此被书中注释掉了） */
    static List<Product> sortedByName(List<Product> products) {
        return products.stream().sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }
}
