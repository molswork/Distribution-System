package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 网关安全配置
 *
 * 说明：
 * - 默认引入 spring-boot-starter-security 会在 Gateway 层启用 CSRF 保护，
 *   对 POST/PUT 等请求要求 CSRF Token，导致 403 (An expected CSRF token cannot be found)。
 * - 我们在网关层仅做 JWT 校验/透传，不需要会话与 CSRF，故在此禁用 CSRF 并放行所有请求。
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange()
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().permitAll()
                .and()
                .build();
    }
}

