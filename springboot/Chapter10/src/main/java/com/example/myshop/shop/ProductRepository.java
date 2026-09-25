package com.example.myshop.shop;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Product.objects.filter(available=True) */
    List<Product> findByAvailableTrueOrderByNameAsc();

    List<Product> findByAvailableTrueAndCategoryOrderByNameAsc(Category category);

    /** get_object_or_404(Product, id=id, slug=slug, available=True)，顺带取出分类 */
    @EntityGraph(attributePaths = "category")
    Optional<Product> findByIdAndSlugAndAvailableTrue(Long id, String slug);

    /** 购物车：Product.objects.filter(id__in=product_ids) */
    List<Product> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByNameAsc();
}
