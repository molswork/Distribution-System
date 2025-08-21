package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 网关配置类
 * 配置路由规则、CORS、负载均衡等
 */
@Configuration
public class GatewayConfig {
    
    /**
     * CORS配置
     * 
     * @return CORS过滤器
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
    
    /**
     * 路由配置
     * 定义各个微服务的路由规则
     * 
     * @param builder 路由构建器
     * @return 路由定位器
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 认证服务路由（注意：auth-service 在 docker 下 context-path=/auth，需要前缀）
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.prefixPath("/auth"))
                        .uri("http://auth-service:8081"))
                
                // 客资服务路由
                .route("lead-service", r -> r.path("/api/leads/**")
                        .uri("lb://lead-service"))
                
                // 成交服务路由
                .route("deal-service", r -> r.path("/api/deals/**")
                        .uri("lb://deal-service"))
                
                // 商品服务路由
                .route("product-service", r -> r.path("/api/products/**")
                        .uri("lb://product-service"))

                // 用户服务路由
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))

                // 推广服务路由
                .route("promotion-service", r -> r.path("/api/promotions/**")
                        .uri("lb://promotion-service"))
                
                // 等级服务路由
                .route("level-service", r -> r.path("/api/levels/**")
                        .uri("lb://level-service"))
                
                // Swagger文档聚合
                .route("swagger-docs", r -> r.path("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/swagger-resources/**")
                        .uri("lb://api-docs"))
                
                // 健康检查
                .route("health", r -> r.path("/actuator/health")
                        .uri("lb://auth-service"))
                
                .build();
    }
}