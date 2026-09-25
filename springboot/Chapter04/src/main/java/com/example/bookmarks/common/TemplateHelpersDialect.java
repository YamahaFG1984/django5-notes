package com.example.bookmarks.common;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * 注册两个自定义表达式对象，模板里用法和内置的 #temporals、#strings 一样：
 * <ul>
 *   <li>{@code #text}：linebreaks / truncatewords 等“过滤器”</li>
 *   <li>{@code #querystring}：≈ Django 5.1 的 {% querystring %} 标签，改一个参数、保留其他参数</li>
 * </ul>
 * Spring Boot 会把容器里所有 IDialect 类型的 Bean 自动加进模板引擎。
 */
@Component
public class TemplateHelpersDialect extends AbstractDialect implements IExpressionObjectDialect {

    private final TextFilters filters;

    public TemplateHelpersDialect(TextFilters filters) {
        super("template-helpers");
        this.filters = filters;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Set.of("text", "querystring");
            }

            @Override
            public Object buildObject(IExpressionContext context, String name) {
                if ("querystring".equals(name)) {
                    Map<String, String[]> params = context instanceof IWebContext web
                            ? web.getExchange().getRequest().getParameterMap()
                            : Map.of();
                    return new Querystring(params);
                }
                return filters;
            }

            /** 同一次模板渲染内可以复用；#querystring 依赖当前请求，所以每次渲染都会重建。 */
            @Override
            public boolean isCacheable(String name) {
                return true;
            }
        };
    }
}
