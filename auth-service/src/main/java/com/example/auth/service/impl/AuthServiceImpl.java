package com.example.auth.service.impl;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.dto.RegisterResponse;
import com.example.auth.dto.CreateSubordinateRequest;
import com.example.auth.dto.CreateSubordinateResponse;
import com.example.auth.entity.User;
import com.example.auth.mapper.UserMapper;
import com.example.auth.service.AuthService;
import com.example.auth.mapper.InvitationCodeMapper;
import com.example.auth.mapper.InvitationRecordMapper;
import com.example.auth.entity.InvitationCode;
import com.example.auth.entity.InvitationRecord;
import com.example.common.dto.CommonResult;
import com.example.common.enums.UserRole;
import com.example.common.exception.BusinessException;
import com.example.common.utils.JwtUtils;
import com.example.common.utils.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
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

    @Autowired
    private InvitationCodeMapper invitationCodeMapper;

    @Autowired
    private InvitationRecordMapper invitationRecordMapper;

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
    public RegisterResponse register(RegisterRequest request) {
        log.info("用户注册请求：phone={}", request.getPhone());
        
        // 1. 验证手机号格式
        if (!isValidPhone(request.getPhone())) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 2. 验证验证码 - 暂时注释，跳过验证码验证
        // String cacheKey = SMS_CODE_PREFIX + request.getPhone();
        // String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        // if (cachedCode == null || !cachedCode.equals(request.getCode())) {
        //     throw new BusinessException("验证码错误或已过期");
        // }
        
        // 3. 基础唯一性检查（手机号/用户名/邮箱）
        if (userMapper.existsByPhone(request.getPhone())) {
            throw new BusinessException("该手机号已注册");
        }
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (StringUtils.hasText(request.getEmail()) && userMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }
        
        // 4/5. 校验/解析邀请码（可选）并确定上级 parentId
        Long parentId = null;
        if (StringUtils.hasText(request.getInviteCode())) {
            parentId = validateAndResolveInviterId(request.getInviteCode(), request.getRole());
        }

        // 6. 创建用户（对齐 v3 DDL）
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail() : null);
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.fromCode(request.getRole().toLowerCase()));
        user.setStatus("active");
        user.setParentId(parentId);

        try {
            userMapper.insert(user);
        } catch (org.springframework.dao.DuplicateKeyException dke) {
            String msg = dke.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("users.username")) {
                    throw new BusinessException("用户名已存在");
                } else if (lower.contains("users.email")) {
                    throw new BusinessException("邮箱已被使用");
                } else if (lower.contains("users.phone")) {
                    throw new BusinessException("该手机号已注册");
                }
            }
            throw new BusinessException("注册失败，唯一约束冲突");
        }

        // 7. 删除已使用的验证码 - 暂时注释，因为跳过了验证码验证
        // redisTemplate.delete(cacheKey);

        // 8. 生成 Token
        String token = JwtUtils.generateToken(user.getId().toString(), user.getRole().name());

        // 9. 若使用邀请码，记录 invitation_records 并更新 codes 使用计数
        if (StringUtils.hasText(request.getInviteCode())) {
            postRegisterRecordInvitation(user.getId(), parentId, request.getInviteCode());
        }

        // 10. 构建响应
        RegisterResponse response = new RegisterResponse();

        // 构建用户信息
        RegisterResponse.UserInfo userInfo = new RegisterResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole().name());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getUsername()); // 暂时使用用户名作为昵称

        response.setUser(userInfo);
        response.setToken(token);
        response.setPermissions(List.of("basic_access")); // 基础权限
        response.setMessage("注册成功");

        log.info("用户注册成功：userId={}, phone={}", user.getId(), user.getPhone());
        return response;
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("[v2] 用户登录请求：phone={}", request.getPhone());

        // 1. 查询用户
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            log.warn("登录失败：用户不存在，phone={}", request.getPhone());
            throw new BusinessException(com.example.common.constants.ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败：密码不匹配，phone={}", request.getPhone());
            throw new BusinessException(com.example.common.constants.ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 3. 检查用户状态
        if ("banned".equals(user.getStatus())) {
            log.warn("登录失败：账号被禁用，phone={}", request.getPhone());
            throw new BusinessException(com.example.common.constants.ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        // 4. 生成 Token
        String token;
        try {
            token = JwtUtils.generateToken(user.getId().toString(), user.getRole().name());
        } catch (Exception e) {
            log.error("生成Token失败: userId={}, phone={}, err={}", user.getId(), user.getPhone(), e.getMessage(), e);
            throw new BusinessException(com.example.common.constants.ErrorCode.INTERNAL_SERVER_ERROR, "生成Token失败");
        }
        
        // 5. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setNickname(user.getUsername());
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
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<CreateSubordinateResponse> createSubordinateBySuperior(CreateSubordinateRequest request) {
        log.info("快速创建下级用户请求：phone={}, targetRole={}", request.getPhone(), request.getRole());
        
        // 1. 获取当前登录用户信息
        String currentUserId = UserContextHolder.getCurrentUserId();
        String currentUserRole = UserContextHolder.getCurrentUserRole();
        
        if (currentUserId == null || currentUserRole == null) {
            throw new BusinessException("获取当前用户信息失败");
        }
        
        User currentUser = userMapper.selectById(Long.valueOf(currentUserId));
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }
        
        // 2. 验证权限：检查是否有权创建目标角色
        UserRole creatorRole = UserRole.fromCode(currentUserRole.toLowerCase());
        UserRole targetRole = UserRole.fromCode(request.getRole().toLowerCase());
        
        if (!canCreateRole(creatorRole, targetRole)) {
            throw new BusinessException("权限不足，无法创建该角色的用户");
        }
        
        // 3. 验证手机号格式
        if (!isValidPhone(request.getPhone())) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 4. 检查手机号是否已注册
        if (userMapper.existsByPhone(request.getPhone())) {
            throw new BusinessException("该手机号已注册");
        }
        
        // 5. 创建用户（对齐 v3 DDL）
        User newUser = new User();
        newUser.setUsername("user" + request.getPhone().substring(7)); // 临时用户名，可按需来自请求
        newUser.setEmail(null);
        newUser.setPhone(request.getPhone());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(targetRole);
        newUser.setParentId(currentUser.getId()); // 上级为创建者
        newUser.setStatus("active");

        userMapper.insert(newUser);

        // 6. 构建响应（去除与昵称/邀请码相关字段）
        CreateSubordinateResponse response = new CreateSubordinateResponse();
        response.setUserId(newUser.getId());
        response.setPhone(newUser.getPhone());
        response.setNickname(newUser.getUsername());
        response.setRole(newUser.getRole().getCode());
        response.setInviteCode(null);
        response.setInviterId(currentUser.getId());
        response.setInviterNickname(currentUser.getUsername());
        
        log.info("下级用户创建成功：userId={}, phone={}, role={}, inviterId={}", 
                newUser.getId(), newUser.getPhone(), newUser.getRole(), currentUser.getId());
        
        return CommonResult.success(response);
    }
    
    /**
     * 检查创建者是否有权限创建目标角色
     * 
     * @param creatorRole 创建者角色
     * @param targetRole 目标角色
     * @return 是否有权限
     */
    private boolean canCreateRole(UserRole creatorRole, UserRole targetRole) {
        switch (creatorRole) {
            case SUPER_ADMIN:
                // 超级管理员可以创建任何角色
                return true;
            case DIRECTOR:
                // 销售总监可以创建：组长、销售、代理
                return targetRole == UserRole.LEADER || targetRole == UserRole.SALES || targetRole == UserRole.AGENT;
            case LEADER:
                // 销售组长可以创建：销售、代理
                return targetRole == UserRole.SALES || targetRole == UserRole.AGENT;
            case SALES:
                // 销售只能创建：代理
                return targetRole == UserRole.AGENT;
            case AGENT:
                // 代理不能创建任何用户
                return false;
            default:
                return false;
        }
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
    private Long validateAndResolveInviterId(String code, String targetRoleRaw) {
        InvitationCode ic = invitationCodeMapper.selectByCodeForUpdate(code);
        if (ic == null) {
            throw new BusinessException("邀请码不存在");
        }
        if (!"active".equalsIgnoreCase(ic.getStatus())) {
            throw new BusinessException("邀请码不可用");
        }
        if (ic.getExpiresAt() != null && ic.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("邀请码已过期");
        }
        if (ic.getMaxUsage() != null && ic.getUsageCount() != null && ic.getUsageCount() >= ic.getMaxUsage()) {
            throw new BusinessException("邀请码已达最大使用次数");
        }
        // 验证角色是否匹配（若 codes 限定目标角色）
        if (ic.getTargetRole() != null) {
            String targetRole = targetRoleRaw == null ? null : targetRoleRaw.toLowerCase();
            if (targetRole != null && !ic.getTargetRole().equalsIgnoreCase(targetRole)) {
                throw new BusinessException("邀请码不支持该角色注册");
            }
        }
        return ic.getUserId();
    }

    private void postRegisterRecordInvitation(Long inviteeId, Long inviterId, String code) {
        try {
            // 写入邀请记录
            com.example.auth.entity.InvitationRecord rec = new com.example.auth.entity.InvitationRecord();
            rec.setInviterId(inviterId);
            rec.setInviteeId(inviteeId);
            rec.setInviteCode(code);
            rec.setStatus("success");
            rec.setRegisteredAt(java.time.LocalDateTime.now());
            // TODO: 从请求上下文注入 IP/UA
            rec.setIpAddress(null);
            rec.setUserAgent(null);
            invitationRecordMapper.insert(rec);

            // 增加使用次数，必要时禁用
            invitationCodeMapper.increaseUsage(code);
            InvitationCode ic = invitationCodeMapper.selectByCodeForUpdate(code);
            if (ic.getMaxUsage() != null && ic.getUsageCount() != null && ic.getUsageCount() >= ic.getMaxUsage()) {
                invitationCodeMapper.deactivate(code);
            }
        } catch (Exception e) {
            log.warn("记录邀请码使用失败，但不影响注册流程: {}", e.getMessage());
        }
    }
}