package com.example.common.integration;

import com.example.common.constants.ErrorCode;
import com.example.common.dto.CommonResult;
import com.example.common.exception.AuthenticationException;
import com.example.common.exception.AuthorizationException;
import com.example.common.exception.BusinessException;
import com.example.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.validation.ConstraintViolationException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 错误处理集成测试
 * 验证端到端的错误处理流程
 */
@DisplayName("错误处理集成测试")
class ErrorHandlingIntegrationTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("验证错误码传播流程 - 业务异常")
    void testErrorCodePropagationBusinessException() {
        // 创建业务异常
        BusinessException exception = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        
        // 通过全局异常处理器处理
        var response = globalExceptionHandler.handleBusinessException(exception);
        CommonResult<Object> result = response.getBody();
        
        // 验证错误码正确传播
        assertNotNull(result);
        assertEquals(404, result.getCode());
        assertEquals("资源不存在", result.getMessage());
        assertEquals(false, result.getSuccess());
        
        // 验证错误码在响应中正确包含
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("RESOURCE_NOT_FOUND", data.get("error_code"));
    }

    @Test
    @DisplayName("验证错误码传播流程 - 操作失败异常")
    void testErrorCodePropagationOperationFailed() {
        // 创建操作失败异常
        BusinessException exception = new BusinessException(ErrorCode.OPERATION_FAILED);
        
        // 通过全局异常处理器处理
        var response = globalExceptionHandler.handleBusinessException(exception);
        CommonResult<Object> result = response.getBody();
        
        // 验证错误码正确传播
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("操作失败", result.getMessage());
        assertEquals(false, result.getSuccess());
        
        // 验证错误码在响应中正确包含
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("OPERATION_FAILED", data.get("error_code"));
    }

    @Test
    @DisplayName("验证HTTP状态码映射一致性")
    void testHttpStatusCodeMappingConsistency() {
        // 测试各种错误码的HTTP状态码映射
        
        // 401 - 未授权
        AuthenticationException authException = new AuthenticationException("认证失败");
        var authResp = globalExceptionHandler.handleAuthenticationException(authException);
        CommonResult<java.util.Map<String, Object>> authResult = authResp.getBody();
        assertEquals(401, authResult.getCode());
        
        // 403 - 权限不足
        AuthorizationException authzException = new AuthorizationException("权限不足");
        var authzResp = globalExceptionHandler.handleAuthorizationException(authzException);
        CommonResult<java.util.Map<String, Object>> authzResult = authzResp.getBody();
        assertEquals(403, authzResult.getCode());
        
        // 404 - 资源不存在
        BusinessException notFoundException = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        var notFoundResp = globalExceptionHandler.handleBusinessException(notFoundException);
        CommonResult<Object> notFoundResult = notFoundResp.getBody();
        assertEquals(404, notFoundResult.getCode());
        
        // 500 - 内部服务器错误
        Exception internalException = new RuntimeException("系统异常");
        var internalResp = globalExceptionHandler.handleException(internalException);
        CommonResult<Map<String, Object>> internalResult = internalResp.getBody();
        assertEquals(500, internalResult.getCode());
    }

    @Test
    @DisplayName("验证错误响应格式标准化")
    void testErrorResponseFormatStandardization() {
        // 测试新格式错误响应
        BusinessException businessException = new BusinessException(ErrorCode.BAD_REQUEST);
        var businessResp = globalExceptionHandler.handleBusinessException(businessException);
        CommonResult<Object> businessResult = businessResp.getBody();
        
        // 验证响应结构
        assertNotNull(businessResult.getCode());
        assertNotNull(businessResult.getSuccess());
        assertNotNull(businessResult.getMessage());
        assertNotNull(businessResult.getData());
        assertNotNull(businessResult.getTimestamp());
        
        // 验证错误响应格式
        assertEquals(false, businessResult.getSuccess());
        assertTrue(businessResult.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) businessResult.getData();
        assertTrue(data.containsKey("error_code"));
        assertEquals("BAD_REQUEST", data.get("error_code"));
        
        // 测试向后兼容格式
        AuthenticationException authException = new AuthenticationException("认证失败");
        var authResp = globalExceptionHandler.handleAuthenticationException(authException);
        CommonResult<java.util.Map<String, Object>> authResult = authResp.getBody();
        
        // 验证向后兼容响应格式
        assertEquals(false, authResult.getSuccess());
        assertNull(authResult.getData()); // 向后兼容格式data为null
    }

    @Test
    @DisplayName("验证参数校验异常处理")
    void testValidationExceptionHandling() {
        // 创建参数校验异常
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "testField", "字段验证失败"));
        
        MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(null, bindingResult);
        
        // 处理异常
        var resp = globalExceptionHandler.handleValidationException(validationException);
        CommonResult<Map<String, Object>> result = resp.getBody();

        // 验证结果
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("参数校验失败", result.getMessage());
        assertEquals(false, result.getSuccess());
        assertNotNull(result.getData()); // 参数校验异常返回错误详情
        assertTrue(result.getData().containsKey("errors"));
    }

    @Test
    @DisplayName("验证约束校验异常处理")
    void testConstraintViolationExceptionHandling() {
        // 创建约束校验异常
        ConstraintViolationException constraintException = new ConstraintViolationException("约束违反", null);
        
        // 处理异常
        var resp = globalExceptionHandler.handleConstraintViolationException(constraintException);
        CommonResult<String> result = resp.getBody();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("约束违反", result.getMessage());
        assertEquals(false, result.getSuccess());
        assertNull(result.getData()); // 约束校验异常使用向后兼容格式
    }

    @Test
    @DisplayName("验证异常处理的完整性")
    void testExceptionHandlingCompleteness() {
        // 测试所有主要异常类型都有对应的处理器
        
        // 业务异常
        BusinessException businessException = new BusinessException(ErrorCode.UNAUTHORIZED);
        assertDoesNotThrow(() -> globalExceptionHandler.handleBusinessException(businessException).getBody());
        
        // 认证异常
        AuthenticationException authException = new AuthenticationException("认证失败");
        assertDoesNotThrow(() -> globalExceptionHandler.handleAuthenticationException(authException).getBody());
        
        // 授权异常
        AuthorizationException authzException = new AuthorizationException("权限不足");
        assertDoesNotThrow(() -> globalExceptionHandler.handleAuthorizationException(authzException).getBody());
        
        // 通用异常
        Exception generalException = new RuntimeException("通用异常");
        assertDoesNotThrow(() -> globalExceptionHandler.handleException(generalException).getBody());
        
        // 参数校验异常
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
        bindingResult.addError(new FieldError("test", "field", "错误"));
        MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(null, bindingResult);
        assertDoesNotThrow(() -> globalExceptionHandler.handleValidationException(validationException).getBody());
        
        // 约束校验异常
        ConstraintViolationException constraintException = new ConstraintViolationException("约束错误", null);
        assertDoesNotThrow(() -> globalExceptionHandler.handleConstraintViolationException(constraintException).getBody());
    }

    @Test
    @DisplayName("验证错误码查找功能")
    void testErrorCodeLookupFunctionality() {
        // 测试根据错误码字符串查找ErrorCode枚举
        ErrorCode foundResourceNotFound = ErrorCode.fromErrorCode("RESOURCE_NOT_FOUND");
        assertNotNull(foundResourceNotFound);
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, foundResourceNotFound);
        
        ErrorCode foundOperationFailed = ErrorCode.fromErrorCode("OPERATION_FAILED");
        assertNotNull(foundOperationFailed);
        assertEquals(ErrorCode.OPERATION_FAILED, foundOperationFailed);
        
        // 测试不存在的错误码
        ErrorCode notFound = ErrorCode.fromErrorCode("NON_EXISTENT_CODE");
        assertNull(notFound);
        
        // 测试根据HTTP状态码查找
        ErrorCode foundByHttpCode = ErrorCode.fromHttpCode(404);
        assertNotNull(foundByHttpCode);
        assertEquals(ErrorCode.NOT_FOUND, foundByHttpCode);
    }

    @Test
    @DisplayName("验证时间戳一致性")
    void testTimestampConsistency() {
        long beforeTime = System.currentTimeMillis();
        
        // 创建多个不同类型的错误响应
        BusinessException businessException = new BusinessException(ErrorCode.BAD_REQUEST);
        var bizResp = globalExceptionHandler.handleBusinessException(businessException);
        CommonResult<Object> businessResult = bizResp.getBody();

        AuthenticationException authException = new AuthenticationException("认证失败");
        var authResp2 = globalExceptionHandler.handleAuthenticationException(authException);
        CommonResult<java.util.Map<String, Object>> authResult = authResp2.getBody();
        
        long afterTime = System.currentTimeMillis();
        
        // 验证时间戳在合理范围内
        assertNotNull(businessResult.getTimestamp());
        assertTrue(businessResult.getTimestamp() >= beforeTime);
        assertTrue(businessResult.getTimestamp() <= afterTime);
        
        assertNotNull(authResult.getTimestamp());
        assertTrue(authResult.getTimestamp() >= beforeTime);
        assertTrue(authResult.getTimestamp() <= afterTime);
    }
}
