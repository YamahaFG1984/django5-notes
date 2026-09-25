package com.example.educa.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 打开 Spring 的缓存注解（@Cacheable / @CacheEvict）。缓存后端由 spring.cache.type=redis 决定，
 * 过期时间、键前缀在 application.yaml 的 spring.cache.redis.* 里配置。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 两处调整：
     * <ul>
     *   <li>transactionAware：事务里的缓存写入 / 清除推迟到事务<b>提交之后</b>才作用到 Redis，
     *       否则“先清缓存、后提交”之间，别的请求可能把旧数据重新读进缓存；</li>
     *   <li>immediateWrites：Lettuce 驱动下，清空缓存（allEntries = true）默认是异步执行的，
     *       方法返回时旧数据可能还在；这里改成同步，重定向后的下一个请求一定看到新数据。</li>
     * </ul>
     */
    @Bean
    RedisCacheManagerBuilderCustomizer cacheManagerCustomizer(RedisConnectionFactory connectionFactory) {
        return builder -> builder
                .cacheWriter(RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()))
                .transactionAware();
    }
}
