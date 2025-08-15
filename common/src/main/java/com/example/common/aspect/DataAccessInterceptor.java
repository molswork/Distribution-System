package com.example.common.aspect;

import com.example.common.dto.DataAccessContext;
import com.example.common.exception.DataAccessDeniedException;
import com.example.common.service.DataOperationLogger;
import com.example.common.service.ServicePermissionChecker;
import com.example.common.utils.DataAccessContextExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 数据访问拦截器
 *
 * <p>基于AOP的数据访问拦截器，是权限控制系统的核心组件。
 * 该拦截器拦截所有Mapper方法的调用，执行权限检查和审计日志记录。
 *
 * <p>主要功能：
 * <ul>
 *   <li>拦截Mapper方法调用</li>
 *   <li>提取数据访问上下文信息</li>
 *   <li>执行权限验证检查</li>
 *   <li>记录操作审计日志</li>
 * </ul>
 *
 * <p>拦截流程：
 * <ol>
 *   <li>提取数据访问上下文（服务名、表名、操作类型等）</li>
 *   <li>执行权限检查，验证服务是否有权限访问</li>
 *   <li>如果有权限，执行原始方法</li>
 *   <li>记录操作结果的审计日志</li>
 *   <li>如果无权限，抛出权限拒绝异常并记录日志</li>
 * </ol>
 *
 * <p>切点定义：
 * <ul>
 *   <li>拦截所有以Mapper结尾的接口方法</li>
 *   <li>拦截com.example.*.mapper包下的所有方法</li>
 * </ul>
 *
 * @author Edom
 * @date 2025-08-01
 * @since 1.0.0
 */
@Aspect
@Component
@ConditionalOnProperty(name = "data.access.interceptor.enabled", havingValue = "true", matchIfMissing = true)
public class DataAccessInterceptor {
    @org.springframework.beans.factory.annotation.Value("${data.access.interceptor.enabled:true}")
    private boolean enabled = true;

    @org.springframework.beans.factory.annotation.Value("${spring.application.name:}")
    private String applicationName;
    @org.springframework.beans.factory.annotation.Value("${data.access.interceptor.exclude-services:}")
    private String excludeServices;

    private static final Logger logger = LoggerFactory.getLogger(DataAccessInterceptor.class);
    private static final ThreadLocal<Boolean> IN_GUARD = ThreadLocal.withInitial(() -> false);

    @Autowired
    private ServicePermissionChecker permissionChecker;

    @Autowired
    private DataOperationLogger operationLogger;

    @Autowired
    private DataAccessContextExtractor contextExtractor;

    /**
     * 拦截Mapper方法调用
     *
     * <p>拦截所有Mapper接口的方法调用，执行权限检查和审计日志记录。
     * 切点表达式匹配所有以Mapper结尾的接口中的方法。
     *
     * @param joinPoint AOP连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行过程中的异常
     */
    @Around("execution(* com.example..mapper..*.*(..)) && !execution(* com.example.common.mapper..*.*(..)) && !execution(* com.example.auth.mapper..*.*(..)) && !@annotation(com.example.common.annotation.DataAccessIgnore) && !@within(com.example.common.annotation.DataAccessIgnore)")
    public Object interceptDataAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!isInterceptorEnabled()) {
            return joinPoint.proceed();
        }
        if (Boolean.TRUE.equals(IN_GUARD.get()) || isExcluded(joinPoint)) {
            return joinPoint.proceed();
        }
        IN_GUARD.set(true);
        DataAccessContext context = null;

        try {
            // 1. 提取数据访问上下文
            context = contextExtractor.extractContext(joinPoint);

            logger.debug("拦截数据访问: {}", context.getOperationSummary());

            // 2. 执行权限检查
            if (!permissionChecker.hasPermission(context)) {
                String reason = String.format("服务 [%s] 无权限对表 [%s] 执行 [%s] 操作",
                        context.getServiceName(), context.getTableName(), context.getOperationType());

                // 记录权限拒绝日志
                operationLogger.logDenied(context, reason);

                // 抛出权限拒绝异常
                throw new DataAccessDeniedException(context, reason);
            }

            // 3. 执行原始方法
            Object result = joinPoint.proceed();

            // 4. 标记操作完成
            context.markCompleted();
            context.setResult(result);

            // 5. 记录操作成功日志
            operationLogger.logSuccess(context, result);

            logger.debug("数据访问成功: {} -> {}", context.getOperationSummary(),
                    result != null ? result.getClass().getSimpleName() : "null");

            return result;

        } catch (DataAccessDeniedException e) {
            // 权限拒绝异常已经在上面处理过了，直接重新抛出
            throw e;

        } catch (Exception e) {
            // 6. 处理其他异常
            if (context != null) {
                context.markFailed(e);
                operationLogger.logFailure(context, e);
            }

            logger.error("数据访问失败: {}",
                    context != null ? context.getOperationSummary() : "unknown", e);

            throw e;
        } finally {
            // 确保守卫标志在任何情况下都能复位，避免递归拦截导致的栈溢出
            IN_GUARD.set(false);
        }
    }

    /**
     * 拦截特定的查询方法（可选的额外拦截点）
     *
     * <p>为特定的查询方法提供额外的拦截逻辑，例如慢查询监控。
     *
     * @param joinPoint AOP连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行过程中的异常
     */
    @Around("(execution(* com.example..mapper..*.select*(..)) || " +
            "execution(* com.example..mapper..*.find*(..)) || " +
            "execution(* com.example..mapper..*.get*(..))) && !execution(* com.example.common.mapper..*.*(..)) && !execution(* com.example.auth.mapper..*.*(..)) && !@annotation(com.example.common.annotation.DataAccessIgnore) && !@within(com.example.common.annotation.DataAccessIgnore)")
    public Object interceptQueryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!isInterceptorEnabled()) {
            return joinPoint.proceed();
        }
        if (Boolean.TRUE.equals(IN_GUARD.get()) || isExcluded(joinPoint)) {
            return joinPoint.proceed();
        }
        IN_GUARD.set(true);
        DataAccessContext context = null;
        long startTime = System.currentTimeMillis();

        try {
            // 提取上下文
            context = contextExtractor.extractContext(joinPoint);

            // 执行方法
            Object result = joinPoint.proceed();

            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            context.setExecutionTime((int) executionTime);

            // 检查是否为慢查询（超过1秒）
            if (executionTime > 1000) {
                logger.warn("检测到慢查询: {} 执行时间: {}ms",
                        context.getOperationSummary(), executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            if (context != null) {
                context.setExecutionTime((int) executionTime);
            }

            logger.error("查询方法执行失败: {} 执行时间: {}ms",
                    context != null ? context.getOperationSummary() : "unknown", executionTime, e);

            throw e;
        } finally {
            IN_GUARD.set(false);
        }
    }

    /**
     * 拦截修改操作方法
     *
     * <p>为INSERT、UPDATE、DELETE操作提供特殊的拦截逻辑，
     * 例如记录数据变更前后的状态。
     *
     * @param joinPoint AOP连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行过程中的异常
     */
    @Around("(execution(* com.example..mapper..*.insert*(..)) || " +
            "execution(* com.example..mapper..*.update*(..)) || " +
            "execution(* com.example..mapper..*.delete*(..))) && !execution(* com.example.common.mapper..*.*(..)) && !execution(* com.example.auth.mapper..*.*(..)) && !@annotation(com.example.common.annotation.DataAccessIgnore) && !@within(com.example.common.annotation.DataAccessIgnore)")
    public Object interceptModifyMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!isInterceptorEnabled()) {
            return joinPoint.proceed();
        }
        if (Boolean.TRUE.equals(IN_GUARD.get()) || isExcluded(joinPoint)) {
            return joinPoint.proceed();
        }
        IN_GUARD.set(true);
        DataAccessContext context = null;

        try {
            // 提取上下文
            context = contextExtractor.extractContext(joinPoint);

            // 对于修改操作，可以在这里记录操作前的数据状态
            // Object beforeData = getBeforeData(joinPoint); // 可选实现

            // 执行方法
            Object result = joinPoint.proceed();

            // 记录影响的行数
            if (result instanceof Number) {
                context.setAffectedRows(((Number) result).intValue());
            }

            // 对于修改操作，记录详细的数据变更日志
            if (context.isModifyOperation()) {
                // operationLogger.logWithDataChange(context, beforeData, afterData, result);
                logger.info("数据修改操作: {} 影响行数: {}",
                        context.getOperationSummary(),
                        result instanceof Number ? result : "unknown");
            }

            return result;

        } catch (Exception e) {
            if (context != null) {
                logger.error("修改操作失败: {}", context.getOperationSummary(), e);
            }

            throw e;
        } finally {
            IN_GUARD.set(false);
        }
    }

    /**
     * 检查拦截器是否启用
     *
     * @return 是否启用拦截器
     */
    public boolean isInterceptorEnabled() {
        if (!enabled) return false;
        try {
            if (excludeServices != null && !excludeServices.isEmpty() && applicationName != null && !applicationName.isEmpty()) {
                for (String s : excludeServices.split(",")) {
                    if (applicationName.equalsIgnoreCase(s.trim())) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return true;
    }

    private boolean isExcluded(org.aspectj.lang.JoinPoint joinPoint) {
        try {
            org.aspectj.lang.reflect.MethodSignature ms = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
            Class<?> decl = ms.getMethod().getDeclaringClass();
            String name = decl != null ? decl.getName() : null;
            if (name == null) return false;
            return name.startsWith("com.example.common.mapper") || name.contains("ServicePermissionMapper") || name.startsWith("com.example.auth.mapper");
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * 获取拦截器统计信息
     *
     * @return 拦截器统计信息
     */
    public String getInterceptorStats() {
        return String.format("DataAccessInterceptor[enabled=%s]", isInterceptorEnabled());
    }
}
