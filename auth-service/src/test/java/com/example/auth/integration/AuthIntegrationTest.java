package com.example.auth.integration;

import com.example.auth.AuthServiceApplication;
import com.example.auth.controller.AuthController;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.entity.User;
import com.example.auth.mapper.UserMapper;
import com.example.common.dto.ApiResponse;
import com.example.common.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证功能集成测试
 * 
 * <p>测试完整的认证流程，包括登录、获取用户信息等
 * 
 * @author mols
 * @date 2025-07-14
 */
@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private User testUser;
    
    @BeforeEach
    public void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setPhone("13800138000");
        testUser.setPassword(passwordEncoder.encode("123456"));
        testUser.setUsername("测试销售");
        testUser.setRole(UserRole.SALES);
        testUser.setStatus("active");
        userMapper.insert(testUser);
    }
    
    /**
     * 测试完整的登录流程
     */
    @Test
    public void testLoginFlow() throws Exception {
        // 1. 用户登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13800138000");
        loginRequest.setPassword("123456");
        
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.role").value("SALES"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // 解析响应获取token
        ApiResponse<LoginResponse> loginResponse =
                objectMapper.readValue(response, objectMapper.getTypeFactory()
                        .constructParametricType(ApiResponse.class, LoginResponse.class));
        String token = loginResponse.getData().getToken();
        
        // 2. 使用token获取用户信息
        mockMvc.perform(get("/api/auth/current")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.role").value("sales"))
                .andExpect(jsonPath("$.data.nickname").value("测试销售"))
                .andExpect(jsonPath("$.data.status").value("active"));
    }
    
    /**
     * 测试登录失败场景 - 密码错误
     */
    @Test
    public void testLogin_WrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13800138000");
        loginRequest.setPassword("wrongpassword");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }
    
    /**
     * 测试登录失败场景 - 用户不存在
     */
    @Test
    public void testLogin_UserNotExist() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13900139000");
        loginRequest.setPassword("123456");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
    
    /**
     * 测试未登录访问受保护接口
     */
    @Test
    public void testAccessProtectedAPI_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或token已过期"));
    }
}