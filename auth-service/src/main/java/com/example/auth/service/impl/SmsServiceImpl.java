package com.example.auth.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.example.auth.config.AliyunSmsConfig;
import com.example.auth.service.SmsService;
import com.example.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信服务实现类 - 使用阿里云SMS服务
 * 
 * <p>实现短信验证码的发送和验证功能。
 * 使用阿里云SMS服务发送短信，Redis存储验证码和控制发送频率。
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private Client smsClient;
    
    @Autowired
    private AliyunSmsConfig smsConfig;
    
    private static final String SMS_CODE_KEY = "sms:code:";
    private static final String SMS_LIMIT_KEY = "sms:limit:";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int LIMIT_EXPIRE_SECONDS = 60;
    
    @Override
    public void sendRegisterCode(String phone) {
        // 1. 验证手机号格式
        if (!isValidPhone(phone)) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 2. 检查发送频率限制
        String limitKey = SMS_LIMIT_KEY + phone;
        String limitValue = redisTemplate.opsForValue().get(limitKey);
        if (limitValue != null) {
            throw new BusinessException("验证码发送太频繁，请稍后再试");
        }
        
        // 3. 生成6位数字验证码
        String code = generateCode();
        
        // 4. 存储验证码到Redis（5分钟过期）
        String codeKey = SMS_CODE_KEY + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // 5. 设置发送频率限制（60秒内不能重复发送）
        redisTemplate.opsForValue().set(limitKey, "1", LIMIT_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        // 6. 发送短信验证码
        try {
            sendSmsViaAliyun(phone, code);
            log.info("阿里云短信验证码发送成功，手机号: {}", phone);
        } catch (Exception e) {
            log.error("阿里云短信发送失败，手机号: {}, 错误: {}", phone, e.getMessage());
            // 发送失败时清除限制，允许重新发送
            redisTemplate.delete(limitKey);
            redisTemplate.delete(codeKey);
            throw new BusinessException("短信发送失败，请稍后重试");
        }
    }
    
    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        
        String codeKey = SMS_CODE_KEY + phone;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        
        if (storedCode == null) {
            log.warn("验证码已过期或不存在，手机号: {}", phone);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.delete(codeKey);
            log.info("验证码验证成功，手机号: {}", phone);
        } else {
            log.warn("验证码错误，手机号: {}, 输入验证码: {}", phone, code);
        }
        
        return isValid;
    }
    
    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
    
    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 通过阿里云SMS发送验证码
     */
    private void sendSmsViaAliyun(String phone, String code) throws Exception {
        SendSmsRequest sendSmsRequest = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(smsConfig.getSignName())
                .setTemplateCode(smsConfig.getRegisterTemplateCode())
                .setTemplateParam("{\"code\":\"" + code + "\"}");
        
        SendSmsResponse response = smsClient.sendSms(sendSmsRequest);
        
        if (!"OK".equals(response.getBody().getCode())) {
            log.error("阿里云短信发送失败，手机号: {}, 错误码: {}, 错误信息: {}", 
                    phone, response.getBody().getCode(), response.getBody().getMessage());
            throw new RuntimeException("短信发送失败: " + response.getBody().getMessage());
        }
        
        log.info("阿里云短信发送成功，手机号: {}, RequestId: {}", 
                phone, response.getBody().getRequestId());
    }
}