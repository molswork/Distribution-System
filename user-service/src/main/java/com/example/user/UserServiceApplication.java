package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

/**
 * 用户管理服务启动类
 *
 * <p>用户管理微服务的主启动类，提供用户CRUD操作、层级关系管理、统计功能等核心业务。
 * 该服务与认证服务分离，专注于用户信息管理和权限控制。
 *
 * <p>主要功能：
 * <ul>
 *   <li>用户基础信息管理（CRUD操作）</li>
 *   <li>用户层级关系维护（上下级关系）</li>
 *   <li>用户权限和角色管理</li>
 *   <li>用户统计和分析功能</li>
 *   <li>批量操作和数据导出</li>
 * </ul>
 *
 * <p>技术特点：
 * <ul>
 *   <li>集成Nacos服务发现和配置管理</li>
 *   <li>使用data-access模块进行数据访问</li>
 *   <li>严格的权限控制和数据隔离</li>
 *   <li>事件驱动架构集成</li>
 *   <li>完整的Swagger API文档</li>
 * </ul>
 *
 * @author User Service Team
 * @version 1.0.0
 * @since 2025-08-07
 */
@SpringBootApplication(exclude = { MybatisAutoConfiguration.class })
@EnableDiscoveryClient
@ComponentScan(
    basePackages = {
        "com.example.user",
        "com.example.common.constants",
        "com.example.common.dto",
        "com.example.common.enums",
        "com.example.common.exception",
        "com.example.common.utils",
        "com.example.data.entity"
    }
)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
