package com.example.educa.courses.catalog;

import java.io.Serializable;

/**
 * 学科 + 课程数。放进 Redis 的对象要能序列化：这里用默认的 JDK 序列化，所以实现 Serializable。
 * 缓存 DTO 而不是实体，是为了不把 Hibernate 代理和懒加载集合塞进缓存。
 */
public record SubjectSummary(Long id, String title, String slug, long totalCourses) implements Serializable {
}
