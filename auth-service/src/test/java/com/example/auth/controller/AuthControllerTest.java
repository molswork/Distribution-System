package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.service.AuthService;
import com.example.auth.service.SmsService;
import com.example.common.dto.CommonResult;
import com.example.common.utils.JwtUtils;
import com.example.common.utils.UserContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器测试类
 * 
 * <p>测试用户认证相关的接口，包括登录、获取用户信息等功能
 * 
 * @author mols
 * @date 2025-07-14
 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    
    private MockMvc mockMvc;
    
    @Mock
    private AuthService authService;
    
    @Mock
    private SmsService smsService;
    
    @InjectMocks
    private AuthController authController;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new com.example.common.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        UserContextHolder.clear();
    }
    
    /**
     * 测试用户登录 - 正常场景
     */
    @Test
    public void testLogin_Success() throws Exception {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("123456");

        LoginResponse response = new LoginResponse();
        response.setToken("mock-jwt-token");
        response.setUserId(1L);
        response.setPhone("13800138000");
        response.setRole("SALES");
        response.setNickname("测试用户");

        // 模拟服务层返回
        when(authService.login(any(LoginRequest.class))).thenReturn(response);
        
        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.role").value("SALES"));
    }
    
    /**
     * 测试用户登录 - 密码错误
     */
    @Test
    public void testLogin_WrongPassword() throws Exception {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("wrongpassword");

        // 模拟服务层抛出异常
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new com.example.common.exception.BusinessException("密码错误"));
        
        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试获取当前用户信息 - 正常场景
     */
    @Test
    public void testGetCurrentUser_Success() throws Exception {
        // 模拟已登录状态
        String mockToken = JwtUtils.generateToken("1", "SALES");
        UserContextHolder.setContext(new UserContextHolder.UserContext("1", "SALES"));
        
        // 准备测试数据
        com.example.auth.entity.User user = new com.example.auth.entity.User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setRole(com.example.common.enums.UserRole.SALES);
        user.setUsername("测试用户");
        user.setStatus("active");
        
        // 模拟服务层返回
        when(authService.getCurrentUser(1L)).thenReturn(user);
        
        // 执行测试
        mockMvc.perform(get("/api/auth/current")
                .header("Authorization", "Bearer " + mockToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.role").value("sales"))
                .andExpect(jsonPath("$.data.status").value("active"));
    }
    
    /**
     * 测试获取当前用户信息 - 未登录场景
     */
    @Test
    public void testGetCurrentUser_Unauthorized() throws Exception {
        // 不设置UserContext，模拟未登录状态
        
        // 执行测试
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或token已过期"));
    }
}