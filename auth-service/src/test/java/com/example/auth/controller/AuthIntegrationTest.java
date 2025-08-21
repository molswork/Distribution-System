package com.example.auth.controller;

import com.example.auth.AuthServiceApplication;
import com.example.auth.dto.LoginRequest;
import com.example.auth.entity.User;
import com.example.auth.mapper.UserMapper;
import com.example.common.enums.UserRole;
import com.example.common.utils.SecurityUtils;
import com.example.common.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证服务集成测试
 * 测试完整的认证流程
 */
@SpringBootTest(classes = AuthServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    /**
     * 测试完整的注册登录流程
     */
    @Test
    public void testRegisterAndLoginFlow() {
        String testPhone = "13800138000";
        String testPassword = "123456";

        // 1. 创建测试用户
        User user = new User();
        user.setPhone(testPhone);
        user.setPassword(SecurityUtils.encodePassword(testPassword));
        user.setRole(UserRole.AGENT);
        user.setStatus("active");
        user.setUsername("测试用户");
        userMapper.insert(user);

        // 2. 测试登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(testPhone);
        loginRequest.setPassword(testPassword);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                getBaseUrl() + "/login",
                loginRequest,
                String.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).contains("token");

        // 3. 提取token测试访问当前用户信息
        String token = extractTokenFromResponse(loginResponse.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                getBaseUrl() + "/current",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(userInfoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userInfoResponse.getBody()).contains(testPhone);
    }

    /**
     * 测试Token刷新
     */
    @Test
    public void testTokenRefresh() {
        // 创建测试用户
        User user = new User();
        user.setPhone("13800138001");
        user.setPassword(SecurityUtils.encodePassword("123456"));
        user.setRole(UserRole.AGENT);
        user.setStatus("active");
        userMapper.insert(user);

        // 生成测试token
        String oldToken = JwtUtils.generateToken(user.getId().toString(), user.getRole().name());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + oldToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> refreshResponse = restTemplate.exchange(
                getBaseUrl() + "/refresh",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).contains("token");
    }

    /**
     * 测试退出登录
     */
    @Test
    public void testLogout() {
        // 创建测试用户
        User user = new User();
        user.setPhone("13800138002");
        user.setPassword(SecurityUtils.encodePassword("123456"));
        user.setRole(UserRole.AGENT);
        user.setStatus("active");
        userMapper.insert(user);

        String token = JwtUtils.generateToken(user.getId().toString(), user.getRole().name());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> logoutResponse = restTemplate.exchange(
                getBaseUrl() + "/logout",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String extractTokenFromResponse(String responseBody) {
        try {
            // 简单提取token，实际应该使用JSON解析
            String tokenKey = "\"token\":\"";
            int startIndex = responseBody.indexOf(tokenKey) + tokenKey.length();
            int endIndex = responseBody.indexOf("\"", startIndex);
            return responseBody.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "";
        }
    }
}