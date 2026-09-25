package com.example.myshop.shop;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * “买了这个的人也买了”推荐引擎，对应 shop/recommender.py。
 * 每个商品一个有序集合 product:{id}:purchased_with，成员是一起买过的商品 id，分数是一起买的次数。
 */
@Component
public class Recommender {

    private final StringRedisTemplate redis;
    private final ProductRepository products;

    public Recommender(StringRedisTemplate redis, ProductRepository products) {
        this.redis = redis;
        this.products = products;
    }

    static String key(Long productId) {
        return "product:" + productId + ":purchased_with";
    }

    /** 同一笔订单里的商品两两 ZINCRBY */
    public void productsBought(Collection<Long> productIds) {
        for (Long productId : productIds) {
            for (Long withId : productIds) {
                if (!productId.equals(withId)) {
                    redis.opsForZSet().incrementScore(key(productId), withId.toString(), 1);
                }
            }
        }
    }

    public List<Product> suggestProductsFor(Collection<Long> productIds, int maxResults) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = List.copyOf(productIds);
        Set<String> suggestions;
        if (ids.size() == 1) {
            suggestions = redis.opsForZSet().reverseRange(key(ids.getFirst()), 0, maxResults - 1L);
        } else {
            // 多个商品：ZUNIONSTORE 把它们的推荐分数加起来，存进临时键。
            // 书中用商品 id 拼接做临时键名（tmp_123），并发请求可能互相覆盖，而且 1+23 和 12+3 会撞名；这里用 UUID。
            String tmpKey = "tmp_" + UUID.randomUUID();
            List<String> keys = ids.stream().map(Recommender::key).toList();
            redis.opsForZSet().unionAndStore(keys.getFirst(), keys.subList(1, keys.size()), tmpKey);
            redis.opsForZSet().remove(tmpKey, ids.stream().map(String::valueOf).toArray());   // 去掉购物车里已有的
            suggestions = redis.opsForZSet().reverseRange(tmpKey, 0, maxResults - 1L);
            redis.delete(tmpKey);
        }
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        List<Long> suggestedIds = suggestions.stream().map(Long::valueOf).toList();
        Map<Long, Product> byId = products.findByIdIn(suggestedIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return suggestedIds.stream().filter(byId::containsKey).map(byId::get).toList();   // 保持 Redis 给出的顺序
    }

    public void clearPurchases() {
        products.findAll().forEach(p -> redis.delete(key(p.getId())));
    }
}
