package com.example.data.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User实体类测试
 * 
 * @author Data Access Test Generator
 * @version 1.0
 * @since 2025-08-03
 */
@DisplayName("User实体类测试")
class UserTest {
    
    private Validator validator;
    private User user;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setPassword("password123");
        user.setRole("agent");
        user.setStatus(User.UserStatus.ACTIVE.getCode());
        user.setCommissionRate(new BigDecimal("0.1000"));
        user.setParentId(2L);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("创建用户 - 正常情况")
    void testCreateUser_Success() {
        User newUser = new User("newuser", "new@example.com", "13900139000", "password", "agent");
        
        assertNotNull(newUser);
        assertEquals("newuser", newUser.getUsername());
        assertEquals("new@example.com", newUser.getEmail());
        assertEquals("13900139000", newUser.getPhone());
        assertEquals("password", newUser.getPassword());
        assertEquals("agent", newUser.getRole());
        assertEquals(User.UserStatus.ACTIVE.getCode(), newUser.getStatus());
        assertEquals(BigDecimal.ZERO, newUser.getCommissionRate());
        assertNotNull(newUser.getCreatedAt());
        assertNotNull(newUser.getUpdatedAt());
    }
    
    @Test
    @DisplayName("用户名验证 - 空值")
    void testUsername_Blank() {
        user.setUsername("");
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("用户名不能为空")));
    }
    
    @Test
    @DisplayName("用户名验证 - 长度不足")
    void testUsername_TooShort() {
        user.setUsername("ab");
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("用户名长度必须在3-64字符之间")));
    }
    
    @Test
    @DisplayName("邮箱验证 - 格式错误")
    void testEmail_InvalidFormat() {
        user.setEmail("invalid-email");
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("邮箱格式不正确")));
    }
    
    @Test
    @DisplayName("手机号验证 - 格式错误")
    void testPhone_InvalidFormat() {
        user.setPhone("12345678901");
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("手机号格式不正确")));
    }
    
    @Test
    @DisplayName("佣金比例验证 - 负数")
    void testCommissionRate_Negative() {
        user.setCommissionRate(new BigDecimal("-0.1"));
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("佣金比例不能为负数")));
    }
    
    @Test
    @DisplayName("佣金比例验证 - 超过100%")
    void testCommissionRate_TooHigh() {
        user.setCommissionRate(new BigDecimal("1.1"));
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("佣金比例不能超过100%")));
    }
    
    @Test
    @DisplayName("检查用户状态 - 活跃状态")
    void testIsActive_ActiveStatus() {
        user.setStatus(User.UserStatus.ACTIVE.getCode());

        assertTrue(user.isActive());
        assertFalse(user.isBanned());
    }
    
    @Test
    @DisplayName("检查用户状态 - 封禁状态")
    void testIsBanned_BannedStatus() {
        user.setStatus(User.UserStatus.BANNED.getCode());

        assertFalse(user.isActive());
        assertTrue(user.isBanned());
    }
    
    @Test
    @DisplayName("检查用户状态 - 未激活状态")
    void testStatus_InactiveStatus() {
        user.setStatus(User.UserStatus.INACTIVE.getCode());

        assertFalse(user.isActive());
        assertFalse(user.isBanned());
    }
    
    @Test
    @DisplayName("检查用户状态 - 待审核状态")
    void testStatus_PendingStatus() {
        user.setStatus(User.UserStatus.PENDING.getCode());

        assertFalse(user.isActive());
        assertFalse(user.isBanned());
    }
    
    @Test
    @DisplayName("更新最后登录时间")
    void testUpdateLastLogin() {
        LocalDateTime beforeUpdate = LocalDateTime.now().minusMinutes(1);
        
        user.updateLastLogin();
        
        assertNotNull(user.getLastLoginAt());
        assertTrue(user.getLastLoginAt().isAfter(beforeUpdate));
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(beforeUpdate));
    }
    
    @Test
    @DisplayName("更新时间戳")
    void testUpdateTimestamp() {
        LocalDateTime beforeUpdate = LocalDateTime.now().minusMinutes(1);
        
        user.updateTimestamp();
        
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(beforeUpdate));
    }
    
    @Test
    @DisplayName("用户状态枚举 - fromCode正常")
    void testUserStatus_FromCode_Success() {
        assertEquals(User.UserStatus.ACTIVE, User.UserStatus.fromCode("active"));
        assertEquals(User.UserStatus.INACTIVE, User.UserStatus.fromCode("inactive"));
        assertEquals(User.UserStatus.BANNED, User.UserStatus.fromCode("banned"));
        assertEquals(User.UserStatus.PENDING, User.UserStatus.fromCode("pending"));
    }
    
    @Test
    @DisplayName("用户状态枚举 - fromCode异常")
    void testUserStatus_FromCode_Exception() {
        assertThrows(IllegalArgumentException.class, () -> {
            User.UserStatus.fromCode("invalid");
        });
    }
    
    @Test
    @DisplayName("用户状态枚举 - 获取描述")
    void testUserStatus_GetDescription() {
        assertEquals("正常", User.UserStatus.ACTIVE.getDescription());
        assertEquals("未激活", User.UserStatus.INACTIVE.getDescription());
        assertEquals("已封禁", User.UserStatus.BANNED.getDescription());
        assertEquals("待审核", User.UserStatus.PENDING.getDescription());
    }
    
    @Test
    @DisplayName("equals和hashCode测试")
    void testEqualsAndHashCode() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("test");
        user1.setEmail("test@example.com");
        
        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("test");
        user2.setEmail("test@example.com");
        
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
    
    @Test
    @DisplayName("toString测试")
    void testToString() {
        String userString = user.toString();
        
        assertNotNull(userString);
        assertTrue(userString.contains("User{"));
        assertTrue(userString.contains("id=1"));
        assertTrue(userString.contains("username='testuser'"));
        assertTrue(userString.contains("email='test@example.com'"));
        assertTrue(userString.contains("status=active"));
    }
    
    @Test
    @DisplayName("所有getters和setters测试")
    void testGettersAndSetters() {
        Long id = 100L;
        String username = "newuser";
        String email = "new@example.com";
        String phone = "13900139001";
        String password = "newpassword";
        String role = "director";
        String status = User.UserStatus.PENDING.getCode();
        BigDecimal commissionRate = new BigDecimal("0.15");
        Long parentId = 5L;
        LocalDateTime now = LocalDateTime.now();
        
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(password);
        user.setRole(role);
        user.setStatus(status);
        user.setCommissionRate(commissionRate);
        user.setParentId(parentId);
        user.setLastLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        assertEquals(id, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(phone, user.getPhone());
        assertEquals(password, user.getPassword());
        assertEquals(role, user.getRole());
        assertEquals(status, user.getStatus());
        assertEquals(commissionRate, user.getCommissionRate());
        assertEquals(parentId, user.getParentId());
        assertEquals(now, user.getLastLoginAt());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }
}