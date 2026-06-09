package com.deadlock.hellocs.common.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HelloCS API",
                description = "개발자를 위한 CS 학습/퀴즈 플랫폼 HelloCS 백엔드 API 문서",
                version = "v1.0.0"
        ),
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH_SCHEME)
)
@SecurityScheme(
        name = SwaggerConfig.BEARER_AUTH_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenApiCustomizer gatewayServerCustomizer() {
        return openApi -> openApi.setServers(List.of(new Server().url("/")));
    }
}
