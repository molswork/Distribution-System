package com.example.auth.controller;

import com.example.auth.service.AuthService;
import com.example.auth.service.SmsService;
import com.example.common.annotation.RequireRole;
import com.example.common.dto.ApiResponse;
import com.example.common.enums.UserRole;
import com.example.common.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "用户注册、登录、token刷新等认证相关接口")
public class AuthController {
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private AuthService authService;
    
    @Operation(summary = "发送注册验证码", description = "向手机号发送注册验证码")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发送成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "手机号格式错误或发送太频繁")
    })
    @PostMapping("/send-code")
    public com.example.common.dto.ApiResponse<String> sendRegisterCode(
            @Valid @RequestBody SendCodeRequest request) {
        smsService.sendRegisterCode(request.getPhone());
        return com.example.common.dto.ApiResponse.success("验证码发送成功");
    }
    
    @Operation(summary = "用户注册", description = "新用户注册，支持销售和代理角色注册")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或手机号已存在")
    })
    @PostMapping("/register")
    public com.example.common.dto.ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        
        // 1. 验证验证码
        if (!smsService.verifyCode(request.getPhone(), request.getCode())) {
            return com.example.common.dto.ApiResponse.error(400, "验证码错误或已过期");
        }
        
        // TODO: 2. 检查手机号是否已注册
        // TODO: 3. 验证邀请码（如果是代理注册）
        // TODO: 4. 创建用户账户
        // TODO: 5. 分配默认角色和权限
        
        RegisterResponse response = new RegisterResponse();
        response.setUserId(1L);
        response.setPhone(request.getPhone());
        response.setRole(request.getRole());
        response.setMessage("注册成功");
        
        return com.example.common.dto.ApiResponse.success(response);
    }
    
    @Operation(summary = "用户登录", description = "用户登录获取JWT令牌")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public com.example.common.dto.ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        // TODO: 实现登录逻辑，验证用户名密码
        
        // 生成JWT token
        String token = JwtUtils.generateToken("1", UserRole.SALES.name());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(1L);
        response.setPhone(request.getPhone());
        response.setRole(UserRole.SALES.name());
        response.setNickname("测试用户");
        
        return com.example.common.dto.ApiResponse.success(response);
    }
    
    @Operation(summary = "获取当前用户信息", description = "根据token获取当前登录用户信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token已过期")
    })
    @GetMapping("/current")
    @SecurityRequirement(name = "JWT")
    public com.example.common.dto.ApiResponse<UserInfo> getCurrentUser() {
        // TODO: 从UserContextHolder获取当前用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(1L);
        userInfo.setPhone("13800138000");
        userInfo.setRole(UserRole.SALES.name());
        userInfo.setNickname("测试用户");
        userInfo.setStatus("active");
        
        return com.example.common.dto.ApiResponse.success(userInfo);
    }
    
    @Operation(summary = "刷新Token", description = "使用旧token换取新token")
    @PostMapping("/refresh")
    @SecurityRequirement(name = "JWT")
    public com.example.common.dto.ApiResponse<RefreshTokenResponse> refreshToken() {
        // TODO: 实现token刷新逻辑
        String newToken = JwtUtils.generateToken("1", UserRole.SALES.name());
        
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setToken(newToken);
        response.setExpiresIn(86400L); // 24小时
        
        return com.example.common.dto.ApiResponse.success(response);
    }
    
    @Operation(summary = "退出登录", description = "退出登录，使token失效")
    @PostMapping("/logout")
    @SecurityRequirement(name = "JWT")
    public com.example.common.dto.ApiResponse<Void> logout() {
        // TODO: 实现退出逻辑，将token加入黑名单
        return com.example.common.dto.ApiResponse.success(null);
    }
    
    /**
     * 注册请求
     */
    @Schema(description = "用户注册请求")
    public static class RegisterRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "手机号", required = true, example = "13800138000")
        private String phone;
        
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
        @Schema(description = "短信验证码（6位数字）", required = true, example = "123456")
        private String code;
        
        @NotBlank(message = "密码不能为空")
        @Schema(description = "密码（6-20位）", required = true, example = "123456")
        private String password;
        
        @NotBlank(message = "角色不能为空")
        @Schema(description = "角色（sales/agent）", required = true, example = "sales")
        private String role;
        
        @Schema(description = "邀请码（代理注册时必填）", example = "INVITE123")
        private String inviteCode;
        
        @Schema(description = "昵称", example = "张三")
        private String nickname;
        
        // Getters and setters
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getInviteCode() { return inviteCode; }
        public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }
    
    /**
     * 注册响应
     */
    @Schema(description = "注册响应")
    public static class RegisterResponse {
        @Schema(description = "用户ID")
        private Long userId;
        
        @Schema(description = "手机号")
        private String phone;
        
        @Schema(description = "角色")
        private String role;
        
        @Schema(description = "消息")
        private String message;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 登录请求
     */
    @Schema(description = "用户登录请求")
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        @Schema(description = "手机号", required = true, example = "13800138000")
        private String phone;
        
        @NotBlank(message = "密码不能为空")
        @Schema(description = "密码", required = true, example = "123456")
        private String password;
        
        // Getters and setters
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * 登录响应
     */
    @Schema(description = "登录响应")
    public static class LoginResponse {
        @Schema(description = "JWT令牌")
        private String token;
        
        @Schema(description = "用户ID")
        private Long userId;
        
        @Schema(description = "手机号")
        private String phone;
        
        @Schema(description = "角色")
        private String role;
        
        @Schema(description = "昵称")
        private String nickname;
        
        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }
    
    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long userId;
        
        @Schema(description = "手机号")
        private String phone;
        
        @Schema(description = "角色")
        private String role;
        
        @Schema(description = "昵称")
        private String nickname;
        
        @Schema(description = "状态")
        private String status;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * 刷新Token响应
     */
    @Schema(description = "刷新Token响应")
    public static class RefreshTokenResponse {
        @Schema(description = "新的JWT令牌")
        private String token;
        
        @Schema(description = "过期时间（秒）")
        private Long expiresIn;
        
        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    }
    
    /**
     * 发送验证码请求
     */
    @Schema(description = "发送验证码请求")
    public static class SendCodeRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "手机号", required = true, example = "13800138000")
        private String phone;
        
        // Getters and setters
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}