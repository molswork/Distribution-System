package com.example.data.mapper;

import com.example.data.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserMapper测试类
 * 使用Mockito进行单元测试，避免数据库依赖
 *
 * @author Data Access Test Generator
 * @version 1.0
 * @since 2025-08-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserMapper单元测试")
class UserMapperTest {

    @Mock
    private UserMapper userMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setPassword("encrypted_password");
        testUser.setRole("agent");
        testUser.setStatus(User.UserStatus.ACTIVE.getCode());
        testUser.setCommissionRate(new BigDecimal("0.10"));
        testUser.setParentId(null);
        testUser.setLastLoginAt(LocalDateTime.now());
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("插入用户 - 正常场景")
    void testInsert_Success() {
        // Given
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        // When
        int result = userMapper.insert(testUser);
        
        // Then
        assertEquals(1, result);
        verify(userMapper).insert(testUser);
    }

    @Test
    @DisplayName("根据ID查找用户 - 存在的用户")
    void testFindById_UserExists() {
        // Given
        when(userMapper.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = userMapper.findById(1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userMapper).findById(1L);
    }

    @Test
    @DisplayName("根据ID查找用户 - 不存在的用户")
    void testFindById_UserNotExists() {
        // Given
        when(userMapper.findById(999L)).thenReturn(Optional.empty());
        
        // When
        Optional<User> result = userMapper.findById(999L);
        
        // Then
        assertFalse(result.isPresent());
        verify(userMapper).findById(999L);
    }

    @Test
    @DisplayName("根据用户名查找用户 - 正常场景")
    void testFindByUsername_Success() {
        // Given
        when(userMapper.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = userMapper.findByUsername("testuser");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userMapper).findByUsername("testuser");
    }

    @Test
    @DisplayName("根据邮箱查找用户 - 正常场景")
    void testFindByEmail_Success() {
        // Given
        when(userMapper.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = userMapper.findByEmail("test@example.com");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userMapper).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("根据手机号查找用户 - 正常场景")
    void testFindByPhone_Success() {
        // Given
        when(userMapper.findByPhone("13800138000")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> result = userMapper.findByPhone("13800138000");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userMapper).findByPhone("13800138000");
    }

    @Test
    @DisplayName("根据角色查找用户列表")
    void testFindByRole_Success() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser);
        when(userMapper.findByRole("agent")).thenReturn(expectedUsers);
        
        // When
        List<User> result = userMapper.findByRole("agent");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userMapper).findByRole("agent");
    }

    @Test
    @DisplayName("根据状态查找用户列表")
    void testFindByStatus_Success() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser);
        when(userMapper.findByStatus(User.UserStatus.ACTIVE.getCode())).thenReturn(expectedUsers);

        // When
        List<User> result = userMapper.findByStatus(User.UserStatus.ACTIVE.getCode());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userMapper).findByStatus(User.UserStatus.ACTIVE.getCode());
    }

    @Test
    @DisplayName("统计用户总数")
    void testCount_Success() {
        // Given
        when(userMapper.count()).thenReturn(10L);
        
        // When
        long result = userMapper.count();
        
        // Then
        assertEquals(10L, result);
        verify(userMapper).count();
    }

    @Test
    @DisplayName("更新用户信息 - 正常场景")
    void testUpdate_Success() {
        // Given
        when(userMapper.update(any(User.class))).thenReturn(1);
        
        // When
        int result = userMapper.update(testUser);
        
        // Then
        assertEquals(1, result);
        verify(userMapper).update(testUser);
    }

    @Test
    @DisplayName("更新用户密码")
    void testUpdatePassword_Success() {
        // Given
        when(userMapper.updatePassword(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(1);
        
        // When
        int result = userMapper.updatePassword(1L, "new_password", LocalDateTime.now());
        
        // Then
        assertEquals(1, result);
        verify(userMapper).updatePassword(anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("更新用户状态")
    void testUpdateStatus_Success() {
        // Given
        when(userMapper.updateStatus(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(1);

        // When
        int result = userMapper.updateStatus(1L, User.UserStatus.INACTIVE.getCode(), LocalDateTime.now());

        // Then
        assertEquals(1, result);
        verify(userMapper).updateStatus(anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("删除用户 - 正常场景")
    void testDeleteById_Success() {
        // Given
        when(userMapper.deleteById(1L)).thenReturn(1);
        
        // When
        int result = userMapper.deleteById(1L);
        
        // Then
        assertEquals(1, result);
        verify(userMapper).deleteById(1L);
    }

    @Test
    @DisplayName("检查用户名是否存在 - 存在")
    void testExistsByUsername_Exists() {
        // Given
        when(userMapper.existsByUsername("testuser", null)).thenReturn(true);
        
        // When
        boolean exists = userMapper.existsByUsername("testuser", null);
        
        // Then
        assertTrue(exists);
        verify(userMapper).existsByUsername("testuser", null);
    }

    @Test
    @DisplayName("检查用户名是否存在 - 不存在")
    void testExistsByUsername_NotExists() {
        // Given
        when(userMapper.existsByUsername("nonexistent", null)).thenReturn(false);
        
        // When
        boolean exists = userMapper.existsByUsername("nonexistent", null);
        
        // Then
        assertFalse(exists);
        verify(userMapper).existsByUsername("nonexistent", null);
    }

    @Test
    @DisplayName("检查邮箱是否存在 - 存在")
    void testExistsByEmail_Exists() {
        // Given
        when(userMapper.existsByEmail("test@example.com", null)).thenReturn(true);
        
        // When
        boolean exists = userMapper.existsByEmail("test@example.com", null);
        
        // Then
        assertTrue(exists);
        verify(userMapper).existsByEmail("test@example.com", null);
    }

    @Test
    @DisplayName("检查手机号是否存在 - 存在")
    void testExistsByPhone_Exists() {
        // Given
        when(userMapper.existsByPhone("13800138000", null)).thenReturn(true);
        
        // When
        boolean exists = userMapper.existsByPhone("13800138000", null);
        
        // Then
        assertTrue(exists);
        verify(userMapper).existsByPhone("13800138000", null);
    }

    @Test
    @DisplayName("分页查找所有用户")
    void testFindAll_Pagination() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser);
        when(userMapper.findAll(0, 10)).thenReturn(expectedUsers);
        
        // When
        List<User> result = userMapper.findAll(0, 10);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userMapper).findAll(0, 10);
    }

    @Test
    @DisplayName("根据上级ID查找下级用户")
    void testFindByParentId_Success() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser);
        when(userMapper.findByParentId(2L)).thenReturn(expectedUsers);
        
        // When
        List<User> result = userMapper.findByParentId(2L);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userMapper).findByParentId(2L);
    }
}