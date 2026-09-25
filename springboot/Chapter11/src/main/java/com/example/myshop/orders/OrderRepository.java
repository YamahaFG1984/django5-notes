package com.example.myshop.orders;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** 订单连同订单项和商品一起取出（≈ prefetch_related('items__product')） */
    @EntityGraph(attributePaths = {"items", "items.product", "coupon"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Order> findWithItemsById(Long id);

    /** 后台列表：按 paid 过滤，最新的在前 */
    List<Order> findAllByOrderByCreatedDesc();

    List<Order> findByPaidOrderByCreatedDesc(boolean paid);

    List<Order> findByIdInOrderByIdAsc(Collection<Long> ids);
}
