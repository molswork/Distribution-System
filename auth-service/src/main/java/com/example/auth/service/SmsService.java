package com.example.auth.service;

/**
 * 短信服务接口
 * 
 * <p>负责处理短信相关的业务逻辑，包括发送验证码、验证验证码等功能。
 * 该接口定义了系统中所有短信相关的操作，实现类应该对接实际的短信服务商。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>发送短信验证码</li>
 *   <li>验证短信验证码</li>
 *   <li>限制发送频率</li>
 * </ul>
 * 
 * <p>设计考虑：
 * <ul>
 *   <li>验证码应该有有效期限制（如 5 分钟）</li>
 *   <li>同一手机号应该有发送频率限制（如 1 分钟内只能发送一次）</li>
 *   <li>验证码应该有尝试次数限制</li>
 * </ul>
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
public interface SmsService {
    
    /**
     * 发送注册验证码
     * 
     * <p>向指定手机号发送注册验证码，需要进行以下验证：
     * <ol>
     *   <li>手机号格式验证</li>
     *   <li>发送频率限制检查</li>
     *   <li>手机号是否已注册检查</li>
     * </ol>
     * 
     * @param phone 手机号
     * @throws BusinessException 当手机号格式错误、发送太频繁或手机号已注册时抛出
     */
    void sendRegisterCode(String phone);
    
    /**
     * 验证验证码
     * 
     * <p>验证用户输入的验证码是否正确
     * 
     * @param phone 手机号
     * @param code 验证码
     * @return 验证是否通过
     */
    boolean verifyCode(String phone, String code);
}