package com.example.educa.courses;

import java.lang.reflect.Field;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PrePersist;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * 实现 {@link OrderField}：JPA 实体监听器在 persist 之前被调用（≈ 自定义字段的 pre_save）。
 * 监听器本身是 Spring Bean（HibernateConfig 让 Hibernate 从 Spring 容器里获取监听器实例），所以能注入依赖。
 */
@Component
public class OrderFieldListener {

    /**
     * 监听器是在 EntityManagerFactory 构建过程中创建的，那时工厂本身还不存在（循环依赖），
     * 所以注入一个 ObjectProvider，等真正用到时再取。
     */
    private final ObjectProvider<EntityManagerFactory> entityManagerFactory;

    public OrderFieldListener(ObjectProvider<EntityManagerFactory> entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PrePersist
    public void assignOrder(Object entity) {
        ReflectionUtils.doWithFields(entity.getClass(), field -> {
            OrderField annotation = field.getAnnotation(OrderField.class);
            ReflectionUtils.makeAccessible(field);
            if (field.get(entity) != null) {
                return;   // 已经指定了顺序，保持不变
            }
            Field scopeField = ReflectionUtils.findField(entity.getClass(), annotation.scope());
            ReflectionUtils.makeAccessible(scopeField);
            Object scope = scopeField.get(entity);
            // SELECT MAX(order) FROM ... WHERE scope = ?；FlushMode.COMMIT 避免在 persist 过程中触发自动 flush
            // 当前事务绑定的 EntityManager（persist 总是在事务里发生）
            EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory.getObject());
            Integer max = entityManager.createQuery(
                            "select max(e.%s) from %s e where e.%s = :scope".formatted(
                                    field.getName(), entity.getClass().getSimpleName(), annotation.scope()),
                            Integer.class)
                    .setParameter("scope", scope)
                    .setFlushMode(FlushModeType.COMMIT)
                    .getSingleResult();
            field.set(entity, max == null ? 0 : max + 1);
        }, field -> field.isAnnotationPresent(OrderField.class));
    }
}
