package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 注册请求
 */
@Schema(description = "用户注册请求")
public class RegisterRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", required = true, example = "13800138000")
    private String phone;
    
    // 完全移除验证码验证，生产环境不需要验证码
    @Schema(description = "短信验证码（已禁用）", required = false, example = "")
    private String code;
    
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码（6-20位）", required = true, example = "123456")
    private String password;
    
    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色（sales/agent）", required = true, example = "sales")
    private String role;
    
    @Schema(description = "邀请码（使用 invitation_codes 表中的 code）", example = "INV123ABC")
    private String inviteCode;

    @Schema(description = "用户名（将写入 users.username）", required = true, example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "邮箱（可选），为空时写入 NULL", example = "zhangsan@example.com")
    private String email;

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
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}