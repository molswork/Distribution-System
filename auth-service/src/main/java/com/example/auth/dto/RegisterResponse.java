package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 注册响应
 */
@Schema(description = "注册响应")
public class RegisterResponse {
    @Schema(description = "用户信息")
    private UserInfo user;

    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "用户权限列表")
    private List<String> permissions;

    @Schema(description = "消息")
    private String message;

    // 内部用户信息类
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "角色")
        private String role;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "昵称")
        private String nickname;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    // Getters and setters
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}