package com.example.auth.entity;

import com.example.common.enums.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * <p>对应数据库表 users，包含用户基本信息、角色、邀请关系等核心字段。
 * 该实体用于系统中所有用户相关的数据操作，包括注册、登录、权限验证等场景。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>存储用户基本信息（手机号、昵称、密码等）</li>
 *   <li>管理用户角色和权限（super_admin、director、leader、sales、agent）</li>
 *   <li>维护邀请关系链（inviterId、inviteCode）</li>
 *   <li>记录业务数据（totalGmv等）</li>
 * </ul>
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class User {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邀请码（唯一）
     */
    private String inviteCode;
    
    /**
     * 微信openid（唯一）
     */
    private String openid;
    
    /**
     * 手机号（唯一）
     */
    private String phone;
    
    /**
     * 密码（加密后）
     */
    private String password;
    
    /**
     * 用户角色
     */
    private UserRole role;
    
    /**
     * 邀请人ID
     */
    private Long inviterId;
    
    /**
     * 用户状态: active-正常, banned-禁用
     */
    private String status;
    
    /**
     * 累计GMV
     */
    private BigDecimal totalGmv;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}