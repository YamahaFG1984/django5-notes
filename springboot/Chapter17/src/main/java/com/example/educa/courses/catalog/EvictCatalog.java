package com.example.educa.courses.catalog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

/**
 * 组合注解：课程增删改后清空“学科列表”和“课程列表”缓存。
 * 书中的缓存没有任何失效逻辑，讲师新建课程后，首页最多要等 15 分钟才看得到。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.SUBJECTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.COURSES, allEntries = true)
})
public @interface EvictCatalog {
}
