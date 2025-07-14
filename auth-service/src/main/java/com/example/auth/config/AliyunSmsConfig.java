package com.example.auth.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信服务配置
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.sms")
@Data
public class AliyunSmsConfig {
    
    /**
     * 阿里云AccessKey ID
     */
    private String accessKeyId;
    
    /**
     * 阿里云AccessKey Secret
     */
    private String accessKeySecret;
    
    /**
     * 短信签名
     */
    private String signName;
    
    /**
     * 注册验证码模板ID
     */
    private String registerTemplateCode;
    
    /**
     * 区域节点
     */
    private String endpoint = "dysmsapi.aliyuncs.com";
    
    /**
     * 创建阿里云SMS客户端
     */
    @Bean
    public Client smsClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }
}