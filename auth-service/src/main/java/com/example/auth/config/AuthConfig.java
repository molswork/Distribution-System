package com.example.auth.config;

import com.example.common.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 认证服务配置类
 * 
 * <p>配置认证服务所需的各种 Bean 和组件，包括密码编码器、拦截器等。
 * 该配置类是认证服务的核心配置，确保所有安全相关的组件正确初始化。
 * 
 * <p>主要配置：
 * <ul>
 *   <li>密码编码器（BCrypt）</li>
 *   <li>JWT 拦截器配置</li>
 *   <li>跨域配置（如需要）</li>
 * </ul>
 * 
 * <p>注意事项：
 * <ul>
 *   <li>登录和注册接口不需要 JWT 验证</li>
 *   <li>其他接口需要通过 JWT 验证</li>
 *   <li>密码使用 BCrypt 加密，强度为默认值 10</li>
 * </ul>
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class AuthConfig implements WebMvcConfigurer {
    
    private final JwtInterceptor jwtInterceptor;
    
    public AuthConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }
    
    /**
     * 配置密码编码器
     * 
     * <p>使用 BCrypt 算法进行密码加密，该算法：
     * <ul>
     *   <li>自带盐值，每次加密结果不同</li>
     *   <li>防止彩虹表攻击</li>
     *   <li>计算强度可调，默认为 10</li>
     * </ul>
     * 
     * @return BCrypt 密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * PasswordEncoder别名Bean，保持向后兼容
     */
    @Bean
    public PasswordEncoder passwordEncoderAlias() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Spring Security 过滤器链配置
     * 
     * <p>配置系统的安全策略，开放认证接口和Swagger文档访问
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 禁用CSRF，因为使用JWT无状态认证
                .csrf().disable()
                // 设置Session创建策略为无状态
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 配置URL访问权限
                .authorizeRequests(authorize -> authorize
                        // 允许认证相关接口公开访问
                        .antMatchers("/api/auth/login", "/api/auth/register", "/api/auth/send-code").permitAll()
                        // 允许Swagger文档相关接口公开访问
                        .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 允许静态资源访问
                        .antMatchers("/static/**", "/webjars/**").permitAll()
                        // 允许健康检查接口访问
                        .antMatchers("/actuator/health").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                // 禁用默认的登录表单
                .formLogin().disable()
                // 禁用默认的HTTP Basic认证
                .httpBasic().disable()
                .build();
    }
    
    /**
     * 配置拦截器
     * 
     * <p>配置 JWT 拦截器的拦截规则：
     * <ul>
     *   <li>拦截所有 /api/** 路径的请求</li>
     *   <li>排除登录、注册等不需要认证的接口</li>
     *   <li>排除 Swagger 文档相关路径</li>
     * </ul>
     * 
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/send-code",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }
}