package com.example.lead.service.impl;

import com.example.lead.dto.CreateLeadRequest;
import com.example.lead.dto.CustomerLeadDto;
import com.example.lead.dto.LeadDetailsDto;
import com.example.lead.facade.LeadDataFacade;
import com.example.lead.service.LeadService;
import com.example.common.dto.CommonResult;
import com.example.common.constants.ErrorCode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 客资管理服务实现类
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-05
 */
@Service
@Transactional
public class LeadServiceImpl implements LeadService {
    
    @Autowired
    private LeadDataFacade leadDataFacade;
    @Autowired(required = false)
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redis;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public CommonResult<CustomerLeadDto> createLead(CreateLeadRequest request) {
        try {
            String phoneNorm = request.getPhone() == null ? null : request.getPhone().replace(" ", "").replace("-", "").trim();
            String lockKey = "lead:create:" + (phoneNorm == null ? "" : phoneNorm);
            // 幂等键
            String idemKey = null;
            try { idemKey = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null ?
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes().getAttribute("Idempotency-Key", 0).toString() : null; } catch (Exception ignore) {}
            if (redis != null) {
                try {
                    Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", java.time.Duration.ofSeconds(10));
                    if (Boolean.FALSE.equals(locked)) {
                        return CommonResult.error(ErrorCode.LEAD_002.getHttpCode(), ErrorCode.LEAD_002.getMessage());
                    }
                } catch (Exception ex) {
                    // Redis 不可用时跳过幂等锁，避免请求直接失败
                    // 可按需记录日志：log.warn("Redis lock skipped: {}", ex.getMessage());
                }
            }
            try {
                // 检查重复
                boolean exists = leadDataFacade.existsByPhone(request.getPhone(), null);
                if (exists) {
                    return CommonResult.error(ErrorCode.LEAD_002.getHttpCode(), ErrorCode.LEAD_002.getMessage());
                }
                // 创建客资
                CustomerLeadDto dto = leadDataFacade.create(request);
                if (redis != null) {
                    try { redis.delete("lead:exists:phone:" + request.getPhone()); } catch (Exception ignore) {}
                    try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                }
                return CommonResult.success(dto);
            } finally {
                if (redis != null) {
                    try { redis.delete(lockKey); } catch (Exception ignore) {}
                }
            }
        } catch (org.springframework.dao.DuplicateKeyException dke) {
            return CommonResult.error(ErrorCode.LEAD_002.getHttpCode(), ErrorCode.LEAD_002.getMessage());
        } catch (Exception e) {
            // 添加详细的异常日志
            System.err.println("创建客资时发生异常: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
    
    @Override
    public CommonResult<LeadDetailsDto> getLeadById(Long id) {
        try {
            return leadDataFacade.findDetailsById(id)
                    .map(CommonResult::success)
                    .orElseGet(() -> CommonResult.error(ErrorCode.LEAD_001.getHttpCode(), "客资不存在"));
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
    
    @Override
    public CommonResult<com.example.lead.dto.PageResult<CustomerLeadDto>> getLeadList(int page, int size, Long salespersonId,
            String status, String auditStatus, String keyword, String source,
            String startDate, String endDate, String sortBy, String sortOrder) {
        try {
            com.example.lead.dto.PageResult<CustomerLeadDto> pageResult = leadDataFacade.findPageWithCount(page, size, salespersonId,
                    status, auditStatus, keyword, source, startDate, endDate, sortBy, sortOrder);
            return CommonResult.success(pageResult);
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
    
    @Override
    public CommonResult<Boolean> checkDuplicate(String phone) {
        try {
            return CommonResult.success(leadDataFacade.existsByPhone(phone, null));
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
    
    @Override
    public CommonResult<Void> updateLeadStatus(Long id, String status) {
        try {
            boolean ok = leadDataFacade.updateStatus(id, status);
            if (ok) {
                if (redis != null) try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                return CommonResult.success(null);
            } else {
                return CommonResult.error(ErrorCode.OPERATION_FAILED.getHttpCode(), "更新客资状态失败");
            }
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
    
    @Override
    public CommonResult<Void> batchAuditLeads(List<Long> ids, String auditStatus) {
        try {
            boolean ok = leadDataFacade.batchUpdateAuditStatus(ids, auditStatus);
            if (ok) {
                if (redis != null) try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                return CommonResult.success(null);
            } else {
                return CommonResult.error(ErrorCode.OPERATION_FAILED.getHttpCode(), "批量审核失败");
            }
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> updateLead(Long id, com.example.lead.dto.UpdateLeadRequest request) {
        try {
            boolean ok = leadDataFacade.updateLead(id, request);
            if (ok) {
                if (redis != null) try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                return CommonResult.success(null);
            }
            return CommonResult.error(ErrorCode.LEAD_001.getHttpCode(), "客资不存在");
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> deleteLead(Long id) {
        try {
            boolean ok = leadDataFacade.deleteLead(id);
            if (ok) {
                if (redis != null) try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                return CommonResult.success(null);
            }
            // 未删除：可能是不存在或存在业务关联
            return CommonResult.error(409, "客资不存在或已有关联记录，无法删除");
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> batchDeleteLeads(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return CommonResult.error(ErrorCode.BAD_REQUEST.getHttpCode(), "客资ID列表不能为空");
            }

            boolean ok = leadDataFacade.batchDeleteLeads(ids);
            if (ok) {
                if (redis != null) try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                return CommonResult.success(null);
            }
            return CommonResult.error(ErrorCode.LEAD_001.getHttpCode(), "部分客资删除失败");
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
    

}