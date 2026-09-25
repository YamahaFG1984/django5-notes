package com.example.mysite.blog.templatetags;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * 把 {@link BlogTags} 注册为表达式对象 {@code #blog}。
 * Django 需要在模板顶部写 {% load blog_tags %}；Thymeleaf 的方言是全局注册的，所有模板直接可用。
 */
@Component
public class BlogDialect extends AbstractDialect implements IExpressionObjectDialect {

    private final BlogTags tags;

    public BlogDialect(BlogTags tags) {
        super("blog");
        this.tags = tags;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Set.of("blog");
            }

            @Override
            public Object buildObject(IExpressionContext context, String name) {
                return tags;
            }

            @Override
            public boolean isCacheable(String name) {
                return true;
            }
        };
    }
}
