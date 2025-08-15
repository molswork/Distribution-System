package com.example.common.exception;

import com.example.common.constants.ErrorCode;
import com.example.common.dto.CommonResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.ObjectError;

import javax.validation.ConstraintViolationException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("全局异常处理器单元测试")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("测试处理业务异常")
    void testHandleBusinessException() {
        String errorMessage = "业务处理失败";
        BusinessException exception = new BusinessException(ErrorCode.OPERATION_FAILED);

        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<Object>> response = globalExceptionHandler.handleBusinessException(exception);
        com.example.common.dto.CommonResult<Object> result = response.getBody();

        assertNotNull(result);
        assertEquals(ErrorCode.OPERATION_FAILED.getCode(), result.getCode());
        assertEquals(ErrorCode.OPERATION_FAILED.getMessage(), result.getMessage());
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("OPERATION_FAILED", data.get("error_code"));
    }

    @Test
    @DisplayName("测试处理认证异常")
    void testHandleAuthenticationException() {
        String errorMessage = "认证失败";
        AuthenticationException exception = new AuthenticationException(errorMessage);
        
        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<java.util.Map<String, Object>>> response = globalExceptionHandler.handleAuthenticationException(exception);
        com.example.common.dto.CommonResult<java.util.Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(401, result.getCode());
        assertEquals("未授权访问", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("测试处理授权异常")
    void testHandleAuthorizationException() {
        String errorMessage = "权限不足";
        AuthorizationException exception = new AuthorizationException(errorMessage);
        
        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<java.util.Map<String, Object>>> response = globalExceptionHandler.handleAuthorizationException(exception);
        com.example.common.dto.CommonResult<java.util.Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(403, result.getCode());
        assertEquals("权限不足", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("测试处理方法参数校验异常")
    void testHandleValidationException() {
        // Create actual objects using Spring's validation infrastructure
        org.springframework.validation.BeanPropertyBindingResult bindingResult = 
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "user");
        bindingResult.addError(new FieldError("user", "phone", "手机号格式不正确"));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            null, bindingResult);
        
        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<String>> response = globalExceptionHandler.handleValidationException(exception);
        com.example.common.dto.CommonResult<String> result = response.getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("手机号格式不正确", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("测试处理参数绑定异常")
    void testHandleBindException() {
        // Create actual objects using Spring's validation infrastructure
        org.springframework.validation.BeanPropertyBindingResult bindingResult = 
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "user");
        bindingResult.addError(new FieldError("user", "email", "邮箱格式不正确"));
        
        BindException exception = new BindException(bindingResult);
        
        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<String>> response = globalExceptionHandler.handleBindException(exception);
        com.example.common.dto.CommonResult<String> result = response.getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("邮箱格式不正确", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("测试处理约束校验异常")
    void testHandleConstraintViolationException() {
        String constraintMessage = "参数不能为空";
        ConstraintViolationException exception = new ConstraintViolationException(constraintMessage, Set.of());
        
        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<String>> response = globalExceptionHandler.handleConstraintViolationException(exception);
        com.example.common.dto.CommonResult<String> result = response.getBody();

        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals(constraintMessage, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("测试处理通用异常")
    void testHandleException() {
        String errorMessage = "系统内部错误";
        Exception exception = new RuntimeException(errorMessage);

        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<java.util.Map<String, Object>>> response = globalExceptionHandler.handleException(exception);
        com.example.common.dto.CommonResult<java.util.Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), result.getMessage());
        assertNotNull(result.getData());
        assertEquals("INTERNAL_SERVER_ERROR", result.getData().get("error_code"));
    }

    @Test
    @DisplayName("测试业务异常不同错误码")
    void testBusinessExceptionDifferentCodes() {
        // 测试不同的业务异常码
        BusinessException badRequestException = new BusinessException(ErrorCode.BAD_REQUEST);
        com.example.common.dto.CommonResult<Object> r1 = globalExceptionHandler.handleBusinessException(badRequestException).getBody();
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), r1.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), r1.getMessage());
        // 验证data字段包含error_code
        assertNotNull(r1.getData());
        assertTrue(r1.getData() instanceof Map);

        BusinessException notFoundException = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        com.example.common.dto.CommonResult<Object> r2 = globalExceptionHandler.handleBusinessException(notFoundException).getBody();
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), r2.getCode());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getMessage(), r2.getMessage());
        // 验证data字段包含error_code
        assertNotNull(r2.getData());
        assertTrue(r2.getData() instanceof Map);

        BusinessException unauthorizedException = new BusinessException(ErrorCode.UNAUTHORIZED);
        com.example.common.dto.CommonResult<Object> r3 = globalExceptionHandler.handleBusinessException(unauthorizedException).getBody();
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), r3.getCode());
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), r3.getMessage());
        // 验证data字段包含error_code
        assertNotNull(r3.getData());
        assertTrue(r3.getData() instanceof Map);
    }

    @Test
    @DisplayName("测试空消息处理")
    void testEmptyMessageHandling() {
        BusinessException exception = new BusinessException(500, null);
        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<Object>> response = globalExceptionHandler.handleBusinessException(exception);
        com.example.common.dto.CommonResult<Object> result = response.getBody();

        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertNull(result.getMessage());
    }

    @Test
    @DisplayName("测试异常链处理")
    void testExceptionChain() {
        Exception rootCause = new IllegalArgumentException("参数错误");
        Exception exception = new RuntimeException("操作失败", rootCause);

        org.springframework.http.ResponseEntity<com.example.common.dto.CommonResult<java.util.Map<String, Object>>> response = globalExceptionHandler.handleException(exception);
        com.example.common.dto.CommonResult<java.util.Map<String, Object>> result = response.getBody();

        assertNotNull(result);
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
    }
}