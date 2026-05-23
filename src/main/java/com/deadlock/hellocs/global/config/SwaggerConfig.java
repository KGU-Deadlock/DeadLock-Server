package com.deadlock.hellocs.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HelloCS API",
                description = "개발자를 위한 CS 학습/퀴즈 플랫폼 HelloCS 백엔드 API 문서",
                version = "v1.0.0"
        ),
        servers = @Server(url = "https://api.hellocs.site", description = "Production"),
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
}
