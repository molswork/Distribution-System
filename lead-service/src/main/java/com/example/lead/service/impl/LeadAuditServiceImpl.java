package com.example.lead.service.impl;

import com.example.common.constants.ErrorCode;
import com.example.common.dto.CommonResult;
import com.example.common.enums.UserRole;
import com.example.common.utils.UserContextHolder;
import com.example.lead.dto.BatchAuditRequest;
import com.example.lead.dto.CustomerLeadDto;
import com.example.lead.dto.LeadAuditRecordDto;
import com.example.lead.dto.PageResult;
import com.example.lead.facade.LeadDataFacade;
import com.example.lead.mapper.LeadAuditRecordMapper;
import com.example.lead.service.LeadAuditService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LeadAuditServiceImpl implements LeadAuditService {

    private final LeadDataFacade facade;
    private final LeadAuditRecordMapper auditRecordMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redis;

    public LeadAuditServiceImpl(LeadDataFacade facade, LeadAuditRecordMapper auditRecordMapper) {
        this.facade = facade;
        this.auditRecordMapper = auditRecordMapper;
    }

    /**
     * 创建审核记录
     */
    private void createAuditRecord(Long leadId, String statusBefore, String statusAfter,
                                  String comment, String rejectReason) {
        try {
            System.out.println("开始创建审核记录 - leadId: " + leadId + ", statusBefore: " + statusBefore + ", statusAfter: " + statusAfter);

            com.example.lead.entity.LeadAuditRecord record = new com.example.lead.entity.LeadAuditRecord();
            record.setLeadId(leadId);

            // 获取当前用户ID，如果是String类型需要转换为Long
            String currentUserId = UserContextHolder.getCurrentUserId();
            System.out.println("当前用户ID: " + currentUserId);

            if (currentUserId != null) {
                try {
                    record.setAuditorId(Long.parseLong(currentUserId));
                } catch (NumberFormatException e) {
                    // 如果转换失败，设置为默认值1（用于测试）
                    System.out.println("用户ID转换失败，使用默认值1: " + e.getMessage());
                    record.setAuditorId(1L);
                }
            } else {
                // 如果没有用户ID，设置为默认值1（用于测试）
                System.out.println("没有找到用户ID，使用默认值1");
                record.setAuditorId(1L);
            }

            record.setStatusBefore(statusBefore);
            record.setStatusAfter(statusAfter);
            record.setComment(comment);
            record.setRejectReason(rejectReason);
            record.setAuditedAt(java.time.LocalDateTime.now());

            System.out.println("准备插入审核记录: " + record);
            int result = auditRecordMapper.insert(record);
            System.out.println("审核记录插入结果: " + result + ", 生成的ID: " + record.getId());

        } catch (Exception e) {
            // 记录审核记录创建失败，但不影响主流程
            System.err.println("创建审核记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean hasAuditPermission(Long salespersonId) {
        String roleCode = UserContextHolder.getCurrentUserRole();
        if (!StringUtils.hasText(roleCode)) return false;
        UserRole role = UserRole.fromCode(roleCode);
        switch (role) {
            case SUPER_ADMIN:
            case DIRECTOR:
            case LEADER:
                return true;
            default:
                return false;
        }
    }

    @Override
    public CommonResult<CustomerLeadDto> auditOne(Long leadId, String decision, String comment, String rejectReason) {
        if (!hasAuditPermission(null)) {
            return CommonResult.error(ErrorCode.LEAD_007.getHttpCode(), "权限不足");
        }
        try {
            // 获取当前客资信息，用于记录审核前状态
            var leadDetails = facade.findDetailsById(leadId);
            if (leadDetails.isEmpty()) {
                return CommonResult.error(ErrorCode.LEAD_001.getHttpCode(), "客资不存在");
            }

            String statusBefore = leadDetails.get().getLeadInfo().getAuditStatus();

            // 修复审核状态逻辑：前端发送APPROVE/REJECT，后端转换为APPROVED/REJECTED
            String statusAfter;
            if ("APPROVE".equalsIgnoreCase(decision) || "APPROVED".equalsIgnoreCase(decision)) {
                statusAfter = "APPROVED";
            } else if ("REJECT".equalsIgnoreCase(decision) || "REJECTED".equalsIgnoreCase(decision)) {
                statusAfter = "REJECTED";
            } else {
                return CommonResult.error(ErrorCode.BAD_REQUEST.getHttpCode(), "无效的审核决定: " + decision);
            }

            // 更新客资审核状态
            boolean ok = facade.updateAuditStatus(leadId, statusAfter);
            if (!ok) return CommonResult.error(ErrorCode.LEAD_001.getHttpCode(), "更新客资状态失败");

            // 创建审核记录
            createAuditRecord(leadId, statusBefore, statusAfter, comment, rejectReason);

            // 使客资列表缓存立即失效（递增版本键）
            if (redis != null) {
                try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
            }

            return facade.findDetailsById(leadId)
                    .map(d -> CommonResult.success(d.getLeadInfo()))
                    .orElseGet(() -> CommonResult.error(ErrorCode.LEAD_001.getHttpCode(), "客资不存在"));
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> batchAudit(BatchAuditRequest request) {
        if (!hasAuditPermission(null)) {
            return CommonResult.error(ErrorCode.LEAD_007.getHttpCode(), "权限不足");
        }
        try {
            // 批量审核时，为每个客资创建审核记录
            for (Long leadId : request.getIds()) {
                var leadDetails = facade.findDetailsById(leadId);
                if (leadDetails.isPresent()) {
                    String statusBefore = leadDetails.get().getLeadInfo().getAuditStatus();
                    // 批量审核时，comment和rejectReason为空，因为BatchAuditRequest没有这些字段
                    createAuditRecord(leadId, statusBefore, request.getAuditStatus(),
                                    null, null);
                }
            }

            boolean ok = facade.batchUpdateAuditStatus(request.getIds(), request.getAuditStatus());
            if (ok) {
                // 批量审核成功，失效列表缓存
                if (redis != null) {
                    try { redis.opsForValue().increment("lead:list:ver"); } catch (Exception ignore) {}
                }
                return CommonResult.success(null);
            }
            return CommonResult.error(ErrorCode.OPERATION_FAILED.getHttpCode(), "批量审核失败");
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<PageResult<CustomerLeadDto>> listPending(int page, int pageSize, String keyword, Long salespersonId) {
        try {
            PageResult<CustomerLeadDto> pr = facade.findPageWithCount(page, pageSize, salespersonId, null, "PENDING_AUDIT", keyword, null, null, null, null, null);
            return CommonResult.success(pr);
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<PageResult<CustomerLeadDto>> listAll(int page, int pageSize, String keyword, String auditStatus, Long salespersonId) {
        try {
            PageResult<CustomerLeadDto> pr = facade.findPageWithCount(page, pageSize, salespersonId, null, auditStatus, keyword, null, null, null, null, null);
            return CommonResult.success(pr);
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<Boolean> checkPermission(Long leadId, Long salespersonId) {
        try {
            return CommonResult.success(hasAuditPermission(salespersonId));
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<Object> auditScope() {
        try {
            String role = UserContextHolder.getCurrentUserRole();
            return CommonResult.success(java.util.Map.of("role", role));
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<PageResult<CustomerLeadDto>> getAuditRecords(int page, int pageSize, String keyword, String auditStatus,
                                                                     Long salespersonId, String startDate, String endDate,
                                                                     String sortBy, String sortOrder) {
        try {
            // 注意：这个方法的返回类型是CustomerLeadDto，但实际应该返回审核记录
            // 为了保持API兼容性，暂时返回空结果
            // 真正的审核记录查询应该使用 getLeadAuditRecords 方法
            PageResult<CustomerLeadDto> emptyResult = new PageResult<>(
                java.util.Collections.emptyList(),
                0L,
                page,
                pageSize
            );
            return CommonResult.success(emptyResult);
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<java.util.List<LeadAuditRecordDto>> getLeadAuditRecords(Long leadId) {
        try {
            if (leadId == null) {
                return CommonResult.error(400, "客资ID不能为空");
            }

            java.util.List<LeadAuditRecordDto> records = auditRecordMapper.selectByLeadId(leadId);
            return CommonResult.success(records);
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "获取审核记录失败: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<java.util.Map<String, Object>> getAuditStatistics(String status, String source, Long salespersonId,
                                                                          String startDate, String endDate) {
        try {
            // 实现真正的统计查询
            long pendingCount = facade.countByConditions(salespersonId, status, "PENDING_AUDIT", null, source, startDate, endDate);
            long approvedCount = facade.countByConditions(salespersonId, status, "APPROVED", null, source, startDate, endDate);
            long rejectedCount = facade.countByConditions(salespersonId, status, "REJECTED", null, source, startDate, endDate);

            // 计算比率（按前端类型定义：0-1 范围）
            long totalAudited = approvedCount + rejectedCount;
            double approvalRate = totalAudited > 0 ? (double) approvedCount / totalAudited : 0.0;
            double rejectionRate = totalAudited > 0 ? (double) rejectedCount / totalAudited : 0.0;

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("pendingCount", pendingCount);
            stats.put("approvedCount", approvedCount);
            stats.put("rejectedCount", rejectedCount);
            stats.put("totalAudited", totalAudited);
            stats.put("approvalRate", approvalRate);
            stats.put("rejectionRate", rejectionRate);
            // 兼容额外字段
            stats.put("pending", pendingCount);
            stats.put("approved", approvedCount);
            stats.put("rejected", rejectedCount);

            return CommonResult.success(stats);
        } catch (Exception e) {
            return CommonResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), "系统错误: " + e.getMessage());
        }
    }
}

