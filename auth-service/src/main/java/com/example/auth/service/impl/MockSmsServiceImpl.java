package com.example.auth.service.impl;

import com.example.auth.service.SmsService;
import com.example.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信服务 Mock 实现（开发环境）
 *
 * 在 dev 或 docker profile 且 sms.mock.enabled=true 时启用。
 * 不调用真实阿里云短信服务，直接将验证码写入 Redis 并输出到控制台日志。
 */
@Slf4j
@Service
@Primary
@Profile({"dev", "docker"})
@ConditionalOnProperty(prefix = "sms.mock", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MockSmsServiceImpl implements SmsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

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

        // 2. 频率限制
        String limitKey = SMS_LIMIT_KEY + phone;
        String limited = redisTemplate.opsForValue().get(limitKey);
        if (limited != null) {
            throw new BusinessException("验证码发送太频繁，请稍后再试");
        }

        // 3. Mock环境：使用固定验证码 123456，方便前端测试
        String code = "123456";
        String codeKey = SMS_CODE_KEY + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", LIMIT_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 4. 控制台输出（开发专用）
        log.info("[MockSMS][dev] 向 {} 发送注册验证码：{}（有效期 {} 分钟）", phone, code, CODE_EXPIRE_MINUTES);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        String codeKey = SMS_CODE_KEY + phone;
        String stored = redisTemplate.opsForValue().get(codeKey);
        if (stored == null) {
            log.warn("[MockSMS] 验证码已过期或不存在，手机号: {}", phone);
            return false;
        }
        boolean ok = stored.equals(code);
        if (ok) {
            redisTemplate.delete(codeKey);
            log.info("[MockSMS] 验证码验证成功，手机号: {}", phone);
        } else {
            log.warn("[MockSMS] 验证码错误，手机号: {}，输入: {}", phone, code);
        }
        return ok;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}

