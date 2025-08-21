package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.entity.User;
import com.example.auth.service.AuthService;
import com.example.auth.service.SmsService;
import com.example.common.annotation.RequireRole;
import com.example.common.dto.CommonResult;
import com.example.common.enums.UserRole;
import com.example.common.utils.JwtUtils;
import com.example.common.utils.UserContextHolder;
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
    public CommonResult<String> sendRegisterCode(
            @Valid @RequestBody SendCodeRequest request) {
        smsService.sendRegisterCode(request.getPhone());
        return CommonResult.success("验证码发送成功");
    }
    
    @Operation(summary = "用户注册", description = "新用户注册，支持销售和代理角色注册")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "唯一性校验失败或参数错误",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.common.dto.CommonResult.class),
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "用户名已存在",
                            summary = "用户名冲突",
                            value = "{\"code\":400,\"success\":false,\"message\":\"用户名已存在\",\"data\":null,\"timestamp\":1712345678901}"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "邮箱已被使用",
                            summary = "邮箱冲突",
                            value = "{\"code\":400,\"success\":false,\"message\":\"邮箱已被使用\",\"data\":null,\"timestamp\":1712345678901}"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "手机号已注册",
                            summary = "手机号冲突",
                            value = "{\"code\":400,\"success\":false,\"message\":\"该手机号已注册\",\"data\":null,\"timestamp\":1712345678901}"
                        )
                    }
                )
            )
    })
    @PostMapping("/register")
    public CommonResult<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return CommonResult.success(response);
    }
    
    @Operation(summary = "用户登录", description = "用户登录获取JWT令牌")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return CommonResult.success(response);
    }
    
    @Operation(summary = "获取当前用户信息", description = "根据token获取当前登录用户信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token已过期")
    })
    @GetMapping("/current")
    @SecurityRequirement(name = "JWT")
    public CommonResult<UserInfo> getCurrentUser() {
        // 从UserContextHolder获取当前用户信息
        String userId = UserContextHolder.getCurrentUserId();
        if (userId == null) {
            return CommonResult.error(401, "未登录或token已过期");
        }
        
        User user = authService.getCurrentUser(Long.valueOf(userId));
        
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole().getCode());
        userInfo.setNickname(user.getUsername());
        userInfo.setStatus(user.getStatus());
        
        return CommonResult.success(userInfo);
    }
    
    @Operation(summary = "刷新Token", description = "使用旧token换取新token")
    @PostMapping("/refresh")
    @SecurityRequirement(name = "JWT")
    public CommonResult<RefreshTokenResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CommonResult.error(401, "无效的Authorization header");
        }
        
        String oldToken = authHeader.substring(7);
        String newToken = authService.refreshToken(oldToken);
        
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setToken(newToken);
        response.setExpiresIn(86400L); // 24小时
        
        return CommonResult.success(response);
    }
    
    @Operation(summary = "退出登录", description = "退出登录，使token失效")
    @PostMapping("/logout")
    @SecurityRequirement(name = "JWT")
    public CommonResult<Void> logout(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CommonResult.error(401, "无效的Authorization header");
        }
        
        String token = authHeader.substring(7);
        String userId = UserContextHolder.getCurrentUserId();
        if (userId != null) {
            authService.logout(token, Long.valueOf(userId));
        }
        
        return CommonResult.success(null);
    }
    
    /**
     * 快速创建下级用户
     * 
     * <p>销售及以上角色专用接口，用于快速创建下级用户账号。
     * 该接口无需短信验证，自动使用创建者的邀请码建立邀请关系。
     * 
     * <p>权限要求：
     * <ul>
     *   <li>SALES - 可创建 AGENT</li>
     *   <li>LEADER - 可创建 SALES, AGENT</li>
     *   <li>DIRECTOR - 可创建 LEADER, SALES, AGENT</li>
     *   <li>SUPER_ADMIN - 可创建任何角色</li>
     * </ul>
     * 
     * <p>业务规则：
     * <ul>
     *   <li>只能创建比自己权限低的角色</li>
     *   <li>自动绑定创建者为邀请人</li>
     *   <li>无需短信验证码</li>
     *   <li>手机号必须唯一</li>
     * </ul>
     * 
     * @param request 创建用户请求，包含手机号、密码、昵称、角色等信息
     * @return 创建成功的用户账号信息
     */
    @Operation(summary = "快速创建下级用户", 
               description = "销售及以上角色可通过手机号快速创建下级用户账号，无需短信验证，自动绑定邀请关系。只能创建比自己权限低的角色。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或手机号已存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足，不能创建同级或更高级别的角色")
    })
    @PostMapping("/create-subordinate")
    @RequireRole(value = {UserRole.SALES, UserRole.LEADER, UserRole.DIRECTOR, UserRole.SUPER_ADMIN})
    @SecurityRequirement(name = "JWT")
    public CommonResult<CreateSubordinateResponse> createSubordinate(
            @Valid @RequestBody CreateSubordinateRequest request) {
        return authService.createSubordinateBySuperior(request);
    }
    
}