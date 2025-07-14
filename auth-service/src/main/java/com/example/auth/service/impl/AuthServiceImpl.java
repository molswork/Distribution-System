package com.example.auth.service.impl;

import com.example.auth.controller.AuthController.LoginRequest;
import com.example.auth.controller.AuthController.LoginResponse;
import com.example.auth.controller.AuthController.RegisterRequest;
import com.example.auth.entity.User;
import com.example.auth.mapper.UserMapper;
import com.example.auth.service.AuthService;
import com.example.common.enums.UserRole;
import com.example.common.exception.BusinessException;
import com.example.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * 
 * <p>实现用户认证相关的业务逻辑，包括注册、登录、Token管理等功能。
 * 使用 Spring Security 进行密码加密，使用 JWT 进行身份验证，使用 Redis 存储验证码和 Token 信息。
 * 
 * <p>技术特点：
 * <ul>
 *   <li>密码使用 BCrypt 加密存储</li>
 *   <li>使用 JWT 生成无状态 Token</li>
 *   <li>验证码存储在 Redis 中，有效期 5 分钟</li>
 *   <li>支持 Token 刷新机制</li>
 * </ul>
 * 
 * <p>业务规则：
 * <ul>
 *   <li>手机号作为唯一登录标识</li>
 *   <li>新用户默认角色为 agent</li>
 *   <li>邀请码可选，但如果提供必须有效</li>
 *   <li>被禁用的账号无法登录</li>
 * </ul>
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 验证码 Redis key 前缀
     */
    private static final String SMS_CODE_PREFIX = "sms:code:";
    
    /**
     * Token 黑名单 Redis key 前缀
     */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    
    /**
     * 验证码有效期（分钟）
     */
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        log.info("用户注册请求：phone={}", request.getPhone());
        
        // 1. 验证手机号格式
        if (!isValidPhone(request.getPhone())) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 2. 验证验证码
        String cacheKey = SMS_CODE_PREFIX + request.getPhone();
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null || !cachedCode.equals(request.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        
        // 3. 检查手机号是否已注册
        if (userMapper.existsByPhone(request.getPhone())) {
            throw new BusinessException("该手机号已注册");
        }
        
        // 4. 处理邀请码（可选）
        Long inviterId = null;
        if (StringUtils.hasText(request.getInviteCode())) {
            User inviter = userMapper.selectByInviteCode(request.getInviteCode());
            if (inviter == null) {
                throw new BusinessException("邀请码无效");
            }
            inviterId = inviter.getId();
        }
        
        // 5. 创建用户
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(7));
        user.setInviteCode(generateInviteCode());
        user.setRole(UserRole.AGENT); // 默认角色为代理
        user.setInviterId(inviterId);
        user.setStatus("active");
        user.setTotalGmv(BigDecimal.ZERO);
        
        userMapper.insert(user);
        
        // 6. 删除已使用的验证码
        redisTemplate.delete(cacheKey);
        
        // 7. 生成 Token
        String token = JwtUtils.generateToken(user.getId().toString(), user.getRole().name());
        
        // 8. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole().name());
        
        log.info("用户注册成功：userId={}, phone={}", user.getId(), user.getPhone());
        return response;
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求：phone={}", request.getPhone());
        
        // 1. 查询用户
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        // 3. 检查用户状态
        if ("banned".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        
        // 4. 生成 Token
        String token = JwtUtils.generateToken(user.getId().toString(), user.getRole().name());
        
        // 5. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole().name());
        
        log.info("用户登录成功：userId={}, phone={}", user.getId(), user.getPhone());
        return response;
    }
    
    @Override
    public User getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
    
    @Override
    public String refreshToken(String oldToken) {
        // 1. 验证旧 Token
        if (!JwtUtils.validateToken(oldToken)) {
            throw new BusinessException("Token 无效或已过期");
        }
        
        // 2. 检查是否在黑名单中
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + oldToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new BusinessException("Token 已失效");
        }
        
        // 3. 解析旧 Token 获取用户ID和角色
        String userId = JwtUtils.getUserIdFromToken(oldToken);
        String role = JwtUtils.getRoleFromToken(oldToken);
        
        // 4. 生成新 Token
        String newToken = JwtUtils.generateToken(userId, role);
        
        // 5. 将旧 Token 加入黑名单
        long expireTime = JwtUtils.getExpirationFromToken(oldToken) - System.currentTimeMillis();
        if (expireTime > 0) {
            redisTemplate.opsForValue().set(blacklistKey, "1", expireTime, TimeUnit.MILLISECONDS);
        }
        
        log.info("Token 刷新成功：userId={}", userId);
        return newToken;
    }
    
    @Override
    public void logout(String token, Long userId) {
        // 将 Token 加入黑名单
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        long expireTime = JwtUtils.getExpirationFromToken(token) - System.currentTimeMillis();
        if (expireTime > 0) {
            redisTemplate.opsForValue().set(blacklistKey, "1", expireTime, TimeUnit.MILLISECONDS);
        }
        
        log.info("用户退出登录：userId={}", userId);
    }
    
    /**
     * 验证手机号格式
     * 
     * @param phone 手机号
     * @return 是否合法
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
    
    /**
     * 生成唯一邀请码
     * 
     * @return 邀请码
     */
    private String generateInviteCode() {
        // 简单实现，实际应该使用更复杂的算法
        String code;
        do {
            code = "INV" + String.format("%06d", (int)(Math.random() * 1000000));
        } while (userMapper.selectByInviteCode(code) != null);
        return code;
    }
}