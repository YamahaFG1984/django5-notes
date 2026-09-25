package com.example.educa.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.hibernate.SpringBeanContainer;

/**
 * 让 Hibernate 从 Spring 容器里获取 JPA 实体监听器（比如 OrderFieldListener），
 * 这样监听器里才能注入 EntityManager 等 Bean。不配置的话，Hibernate 会用反射直接 new 一个实例，注入的字段都是 null。
 */
@Configuration
public class HibernateConfig {

    @Bean
    HibernatePropertiesCustomizer springBeanContainer(ConfigurableListableBeanFactory beanFactory) {
        return properties -> properties.put(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory));
    }
}
