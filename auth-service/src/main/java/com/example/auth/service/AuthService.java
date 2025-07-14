package com.example.auth.service;

import com.example.auth.controller.AuthController.LoginRequest;
import com.example.auth.controller.AuthController.LoginResponse;
import com.example.auth.controller.AuthController.RegisterRequest;
import com.example.auth.entity.User;

/**
 * 认证服务接口
 * 
 * <p>定义用户认证相关的业务逻辑接口，包括注册、登录、Token管理等功能。
 * 该接口是认证模块的核心业务层，负责处理所有与用户身份验证相关的业务逻辑。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>用户注册（手机号验证码注册）</li>
 *   <li>用户登录（JWT Token 生成）</li>
 *   <li>Token 刷新和验证</li>
 *   <li>用户信息查询</li>
 *   <li>退出登录</li>
 * </ul>
 * 
 * <p>设计原则：
 * <ul>
 *   <li>所有涉及密码的操作都应该在实现类中进行加密处理</li>
 *   <li>Token 的生成和验证应该遵循 JWT 标准</li>
 *   <li>异常处理应该抛出明确的业务异常</li>
 * </ul>
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
public interface AuthService {
    
    /**
     * 用户注册
     * 
     * <p>处理用户手机号验证码注册流程：
     * <ol>
     *   <li>验证手机号格式和验证码</li>
     *   <li>检查手机号是否已注册</li>
     *   <li>处理邀请码逻辑（可选）</li>
     *   <li>创建用户账号</li>
     *   <li>生成并返回 JWT Token</li>
     * </ol>
     * 
     * @param request 注册请求，包含手机号、验证码、密码等信息
     * @return 登录响应，包含 JWT Token 和用户基本信息
     * @throws BusinessException 当手机号已注册、验证码错误等情况时抛出
     */
    LoginResponse register(RegisterRequest request);
    
    /**
     * 用户登录
     * 
     * <p>处理用户登录流程：
     * <ol>
     *   <li>验证手机号和密码</li>
     *   <li>检查用户状态是否正常</li>
     *   <li>生成 JWT Token</li>
     *   <li>记录登录日志（可选）</li>
     * </ol>
     * 
     * @param request 登录请求，包含手机号和密码
     * @return 登录响应，包含 JWT Token 和用户信息
     * @throws BusinessException 当用户不存在、密码错误、账号被禁用等情况时抛出
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 获取当前登录用户信息
     * 
     * <p>根据当前请求的 Token 获取用户信息
     * 
     * @param userId 从 Token 中解析出的用户ID
     * @return 用户信息
     * @throws BusinessException 当用户不存在时抛出
     */
    User getCurrentUser(Long userId);
    
    /**
     * 刷新 Token
     * 
     * <p>在 Token 即将过期时刷新，延长用户登录状态
     * 
     * @param oldToken 旧的 Token
     * @return 新的 Token
     * @throws BusinessException 当 Token 无效或已过期时抛出
     */
    String refreshToken(String oldToken);
    
    /**
     * 退出登录
     * 
     * <p>使当前 Token 失效，清理相关缓存
     * 
     * @param token 当前 Token
     * @param userId 用户ID
     */
    void logout(String token, Long userId);
}