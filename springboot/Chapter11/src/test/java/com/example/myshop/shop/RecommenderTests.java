package com.example.myshop.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;


class RecommenderTests extends IntegrationTest {

    @Autowired
    Recommender recommender;

    @Autowired
    ProductRepository products;

    @Autowired
    StringRedisTemplate redis;


    Map<String, Product> bySlug;

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();
        bySlug = products.findAll().stream().collect(Collectors.toMap(Product::getSlug, Function.identity()));
    }

    private Long id(String slug) {
        return bySlug.get(slug).getId();
    }

    @Test
    void suggestsProductsBoughtTogether() {
        // 书中的示例：绿茶常和红茶、茶粉一起买
        recommender.productsBought(List.of(id("green-tea"), id("red-tea")));
        recommender.productsBought(List.of(id("green-tea"), id("tea-powder")));
        recommender.productsBought(List.of(id("green-tea"), id("red-tea")));
        recommender.productsBought(List.of(id("red-tea"), id("espresso")));

        assertThat(recommender.suggestProductsFor(List.of(id("green-tea")), 4))
                .extracting(Product::getSlug).containsExactly("red-tea", "tea-powder");

        // 多个商品：合并分数，并排除自己
        assertThat(recommender.suggestProductsFor(List.of(id("green-tea"), id("red-tea")), 4))
                .extracting(Product::getSlug).containsExactlyInAnyOrder("tea-powder", "espresso");
        assertThat(redis.keys("tmp_*")).isEmpty();   // 临时键已删除

        assertThat(recommender.suggestProductsFor(List.of(id("espresso")), 1))
                .extracting(Product::getSlug).containsExactly("red-tea");
        assertThat(recommender.suggestProductsFor(List.of(), 4)).isEmpty();
    }
}
