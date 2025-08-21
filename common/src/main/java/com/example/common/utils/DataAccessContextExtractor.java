package com.example.common.utils;

import com.example.common.annotation.DataTable;
import com.example.common.dto.DataAccessContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据访问上下文提取器
 * 
 * <p>负责从AOP切点中提取数据访问的上下文信息，包括服务名、表名、操作类型等。
 * 该工具类是数据访问拦截器的重要组成部分，提供了统一的上下文信息提取逻辑。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>从Mapper方法中提取表名和操作类型</li>
 *   <li>获取当前服务名和用户信息</li>
 *   <li>提取HTTP请求的上下文信息</li>
 *   <li>生成唯一的请求标识</li>
 * </ul>
 * 
 * <p>提取规则：
 * <ul>
 *   <li>表名：从Mapper接口名或方法注解中提取</li>
 *   <li>操作类型：从方法名前缀推断（select/insert/update/delete）</li>
 *   <li>服务名：从应用配置或包名中提取</li>
 *   <li>用户信息：从当前会话或安全上下文中获取</li>
 * </ul>
 * 
 * @author Edom
 * @date 2025-08-01
 * @since 1.0.0
 */
@Component
public class DataAccessContextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DataAccessContextExtractor.class);

    private static final String DEFAULT_SERVICE_NAME = "unknown-service";

    @Autowired(required = false)
    private Environment environment;
    
    /**
     * 从AOP切点提取数据访问上下文
     * 
     * @param joinPoint AOP切点
     * @return 数据访问上下文
     */
    public DataAccessContext extractContext(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            // 提取基本信息
            String serviceName = extractServiceName(joinPoint);
            String tableName = extractTableName(joinPoint);
            String operationType = extractOperationType(method);
            String methodName = method.getName();
            
            // 提取用户和请求信息
            Long userId = extractUserId();
            String ipAddress = extractIpAddress();
            String userAgent = extractUserAgent();
            String requestId = generateRequestId();
            
            // 构建上下文
            DataAccessContext context = DataAccessContext.builder()
                .requestId(requestId)
                .serviceName(serviceName)
                .tableName(tableName)
                .operationType(operationType)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .methodName(methodName)
                .startTime(LocalDateTime.now())
                .build();
            
            log.debug("提取数据访问上下文: {}", context.getOperationSummary());
            
            return context;
            
        } catch (Exception e) {
            log.error("提取数据访问上下文失败", e);
            return createDefaultContext();
        }
    }
    
    /**
     * 提取服务名称
     * 
     * @param joinPoint AOP切点
     * @return 服务名称
     */
    private String extractServiceName(ProceedingJoinPoint joinPoint) {
        try {
            // 优先使用应用名
            if (environment != null) {
                String appName = environment.getProperty("spring.application.name");
                if (StringUtils.hasText(appName)) {
                    return appName;
                }
            }
            // 备选：从方法声明类型的包名推导，例如 com.example.auth.mapper -> auth-service
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Package pkg = signature.getDeclaringType().getPackage();
            if (pkg != null) {
                String packageName = pkg.getName();
                if (packageName.contains(".auth.")) return "auth-service";
                if (packageName.contains(".lead.")) return "lead-service";
                if (packageName.contains(".user.")) return "user-service";
                if (packageName.contains(".common.")) return "common";
            }
            return DEFAULT_SERVICE_NAME;
        } catch (Exception e) {
            log.warn("提取服务名称失败", e);
            return DEFAULT_SERVICE_NAME;
        }
    }
    
    /**
     * 提取表名
     * 
     * @param joinPoint AOP切点
     * @return 表名
     */
    private String extractTableName(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Class<?> declaringType = signature.getDeclaringType();
            // 1) 注解优先：如果 Mapper 接口上标注了 @DataTable，则使用显式表名
            if (declaringType != null) {
                DataTable anno = declaringType.getAnnotation(DataTable.class);
                if (anno != null && StringUtils.hasText(anno.value())) {
                    return anno.value();
                }
            }
            // 2) 回退：根据接口简单类名推断（去掉 Mapper 后缀 + 驼峰转下划线）
            String declaringSimple = declaringType != null ? declaringType.getSimpleName() : null;
            if (declaringSimple != null && declaringSimple.endsWith("Mapper")) {
                String tableName = declaringSimple.substring(0, declaringSimple.length() - 6);
                return camelToSnakeCase(tableName);
            }
            // 3) 再退：target class 名称
            String className = joinPoint.getTarget().getClass().getSimpleName();
            if (className.endsWith("Mapper")) {
                String tableName = className.substring(0, className.length() - 6);
                return camelToSnakeCase(tableName);
            }
            return "unknown_table";
        } catch (Exception e) {
            log.warn("提取表名失败", e);
            return "unknown_table";
        }
    }

    /**
     * 提取操作类型
     * 
     * @param method 方法对象
     * @return 操作类型
     */
    private String extractOperationType(Method method) {
        String methodName = method.getName().toLowerCase();
        
        if (methodName.startsWith("select") || methodName.startsWith("find") || 
            methodName.startsWith("get") || methodName.startsWith("query") ||
            methodName.startsWith("count") || methodName.startsWith("exists")) {
            return "SELECT";
        } else if (methodName.startsWith("insert") || methodName.startsWith("add") ||
                   methodName.startsWith("create") || methodName.startsWith("save")) {
            return "INSERT";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify") ||
                   methodName.startsWith("edit") || methodName.startsWith("change")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove") ||
                   methodName.startsWith("drop")) {
            return "DELETE";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 提取用户ID
     * 
     * @return 用户ID
     */
    private Long extractUserId() {
        try {
            // TODO: 从安全上下文或会话中获取用户ID
            // 这里需要根据实际的用户认证机制来实现
            // 例如：从JWT token、Session、SecurityContext等获取
            
            // 临时实现：从请求头中获取
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String userIdHeader = request.getHeader("X-User-Id");
                if (StringUtils.hasText(userIdHeader)) {
                    return Long.parseLong(userIdHeader);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.debug("提取用户ID失败", e);
            return null;
        }
    }
    
    /**
     * 提取客户端IP地址
     * 
     * @return IP地址
     */
    private String extractIpAddress() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }
            
            // 尝试从各种代理头中获取真实IP
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For可能包含多个IP，取第一个
                return ip.split(",")[0].trim();
            }
            
            ip = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            
            ip = request.getHeader("Proxy-Client-IP");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            
            ip = request.getHeader("WL-Proxy-Client-IP");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            
            return request.getRemoteAddr();
            
        } catch (Exception e) {
            log.debug("提取IP地址失败", e);
            return null;
        }
    }
    
    /**
     * 提取用户代理信息
     * 
     * @return User-Agent
     */
    private String extractUserAgent() {
        try {
            HttpServletRequest request = getCurrentRequest();
            return request != null ? request.getHeader("User-Agent") : null;
        } catch (Exception e) {
            log.debug("提取User-Agent失败", e);
            return null;
        }
    }
    
    /**
     * 生成请求唯一标识
     * 
     * @return 请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 获取当前HTTP请求
     * 
     * @return HTTP请求对象
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 驼峰命名转下划线命名
     * 
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    private String camelToSnakeCase(String camelCase) {
        if (!StringUtils.hasText(camelCase)) {
            return camelCase;
        }
        
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * 创建默认上下文
     * 
     * @return 默认数据访问上下文
     */
    private DataAccessContext createDefaultContext() {
        return DataAccessContext.builder()
            .requestId(generateRequestId())
            .serviceName(DEFAULT_SERVICE_NAME)
            .tableName("unknown_table")
            .operationType("UNKNOWN")
            .startTime(LocalDateTime.now())
            .build();
    }
}