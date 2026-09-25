package com.example.mysite.common;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * 把 {@link TextFilters} 注册成 Thymeleaf 的表达式对象 {@code #text}，
 * 模板里就能写 {@code ${#text.linebreaks(post.body)}}，用法和内置的 #temporals、#strings 一样。
 * 这相当于 Django 的“自定义模板过滤器”（templatetags 模块）。
 * Spring Boot 会把容器里所有 IDialect 类型的 Bean 自动加进模板引擎。
 */
@Component
public class TextDialect extends AbstractDialect implements IExpressionObjectDialect {

    private final TextFilters filters;

    public TextDialect(TextFilters filters) {
        super("text");
        this.filters = filters;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Set.of("text");
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return filters;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}
