package com.example.educa.courses.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * springdoc 扫描控制器生成 OpenAPI 文档：/v3/api-docs（JSON），/swagger-ui.html（可交互的页面）。
 * DRF 的“可浏览 API”是给每个端点渲染一个 HTML 页面；Swagger UI 则是一份完整的接口文档，也能直接发请求试用。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI educaOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Educa API").version("1.0"))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
