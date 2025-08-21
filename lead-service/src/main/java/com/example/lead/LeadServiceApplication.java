package com.example.lead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 客资管理服务启动类
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-05
 */
@SpringBootApplication(scanBasePackages = {"com.example.lead", "com.example.common"})
@EnableDiscoveryClient
@MapperScan({"com.example.data.mapper", "com.example.common.mapper", "com.example.lead.mapper"})
public class LeadServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LeadServiceApplication.class, args);
    }
}