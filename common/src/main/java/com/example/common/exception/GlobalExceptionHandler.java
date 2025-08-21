package com.example.common.exception;

import com.example.common.dto.CommonResult;
import com.example.common.constants.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResult<Object>> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());

        CommonResult<Object> body;
        if (e.getErrorCode() != null) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error_code", e.getErrorCode());
            body = new CommonResult<>(e.getCode(), false, e.getMessage(), errorData);
        } else {
            body = CommonResult.error(e.getCode(), e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.valueOf(body.getCode())).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResult<Map<String, Object>>> handleAuthenticationException(AuthenticationException e) {
        logger.warn("认证异常: {}", e.getMessage());
        CommonResult<Map<String, Object>> body = CommonResult.unauthorizedWithErrorCode();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<CommonResult<Map<String, Object>>> handleAuthorizationException(AuthorizationException e) {
        logger.warn("授权异常: {}", e.getMessage());
        CommonResult<Map<String, Object>> body = CommonResult.forbiddenWithErrorCode();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(DataAccessDeniedException.class)
    public ResponseEntity<CommonResult<Map<String, Object>>> handleDataAccessDeniedException(DataAccessDeniedException e) {
        logger.warn("数据访问受限: {}", e.getDetailedMessage());
        Map<String, Object> data = new HashMap<>();
        data.put("error_code", e.getErrorCode());
        data.put("operation", e.getOperationKey());
        CommonResult<Map<String, Object>> body = CommonResult.error(ErrorCode.FORBIDDEN, data);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResult<Map<String, Object>>> handleValidationException(MethodArgumentNotValidException e) {
        var fieldErrors = e.getBindingResult().getFieldErrors();
        var errors = new java.util.ArrayList<Map<String, Object>>(fieldErrors.size());
        for (var fe : fieldErrors) {
            Map<String, Object> item = new HashMap<>();
            item.put("field", fe.getField());
            item.put("rejectedValue", fe.getRejectedValue());
            item.put("message", fe.getDefaultMessage());
            errors.add(item);
        }
        logger.warn("参数校验异常: {}", errors);
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonResult<>(HttpStatus.BAD_REQUEST.value(), false, "参数校验失败", data));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<CommonResult<Map<String, Object>>> handleBindException(BindException e) {
        var fieldErrors = e.getBindingResult().getFieldErrors();
        var errors = new java.util.ArrayList<Map<String, Object>>(fieldErrors.size());
        for (var fe : fieldErrors) {
            Map<String, Object> item = new HashMap<>();
            item.put("field", fe.getField());
            item.put("rejectedValue", fe.getRejectedValue());
            item.put("message", fe.getDefaultMessage());
            errors.add(item);
        }
        logger.warn("参数绑定异常: {}", errors);
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CommonResult<>(HttpStatus.BAD_REQUEST.value(), false, "参数绑定失败", data));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResult<String>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        logger.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResult.badRequest("请求体格式错误"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResult<String>> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("约束校验异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResult.badRequest(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResult<Map<String, Object>>> handleException(Exception e) {
        // 记录更详细的异常信息以便快速定位
        String simple = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "");
        logger.error("系统异常: {}", simple, e);
        Map<String, Object> data = new HashMap<>();
        data.put("error_code", ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode());
        data.put("exception", e.getClass().getName());
        String top = null;
        if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
            StackTraceElement ste = e.getStackTrace()[0];
            top = ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber();
        }
        if (top != null) {
            data.put("top_stack", top);
        }
        CommonResult<Map<String, Object>> body = CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR, data);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}