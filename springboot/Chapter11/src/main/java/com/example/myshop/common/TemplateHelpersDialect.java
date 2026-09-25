package com.example.myshop.common;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * 模板里的 #text（linebreaks 等“过滤器”）和 #fmt（按当前语言格式化金额）。
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
                return Set.of("text", "fmt");
            }

            @Override
            public Object buildObject(IExpressionContext context, String name) {
                return "fmt".equals(name) ? new Formats(context.getLocale()) : filters;
            }

            @Override
            public boolean isCacheable(String name) {
                return true;   // 每次模板渲染内缓存；#fmt 依赖本次渲染的语言，所以每次渲染重建
            }
        };
    }
}
