package com.example.bookmarks.images;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 用 Redis 记录图片浏览数和排行，对应书中 images/views.py 里的 r.incr / r.zincrby / r.zrange。
 * 键名和 Django 版完全一样：image:{id}:views（字符串计数器）、image_ranking（有序集合）。
 */
@Component
public class ImageViews {

    static final String RANKING_KEY = "image_ranking";

    private final StringRedisTemplate redis;

    public ImageViews(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** total_views = r.incr(f'image:{image.id}:views') 以及 r.zincrby('image_ranking', 1, image.id) */
    public long recordView(Long imageId) {
        Long total = redis.opsForValue().increment("image:" + imageId + ":views");
        redis.opsForZSet().incrementScore(RANKING_KEY, imageId.toString(), 1);
        return total == null ? 0 : total;
    }

    /** r.zrange('image_ranking', 0, -1, desc=True)[:10] —— 直接让 Redis 只返回前 N 个 */
    public List<Long> topIds(int count) {
        Set<String> ids = redis.opsForZSet().reverseRange(RANKING_KEY, 0, count - 1L);
        return ids == null ? List.of() : ids.stream().map(Long::valueOf).toList();
    }
}
