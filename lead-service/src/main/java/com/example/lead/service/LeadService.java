package com.example.lead.service;

import com.example.lead.dto.CreateLeadRequest;
import com.example.lead.dto.CustomerLeadDto;
import com.example.lead.dto.LeadDetailsDto;
import com.example.common.dto.CommonResult;

import java.util.List;

/**
 * 客资管理服务接口
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-05
 */
public interface LeadService {
    
    /**
     * 创建客资
     * 
     * @param request 创建请求
     * @return 创建结果
     */
    CommonResult<CustomerLeadDto> createLead(CreateLeadRequest request);
    
    /**
     * 根据ID获取客资详情
     * 
     * @param id 客资ID
     * @return 客资详情
     */
    CommonResult<LeadDetailsDto> getLeadById(Long id);
    
    /**
     * 分页查询客资列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param salespersonId 销售ID (可选)
     * @param status 状态 (可选)
     * @return 客资列表
     */
    CommonResult<com.example.lead.dto.PageResult<CustomerLeadDto>> getLeadList(int page, int size, Long salespersonId,
            String status, String auditStatus, String keyword, String source,
            String startDate, String endDate, String sortBy, String sortOrder);
    
    /**
     * 检查客资重复
     * 
     * @param phone 手机号
     * @return 检查结果
     */
    CommonResult<Boolean> checkDuplicate(String phone);
    
    /**
     * 更新客资状态
     * 
     * @param id 客资ID
     * @param status 新状态
     * @return 更新结果
     */
    CommonResult<Void> updateLeadStatus(Long id, String status);
    
    /**
     * 批量审核客资
     * 
     * @param ids 客资ID列表
     * @param auditStatus 审核状态
     * @return 审核结果
     */
    CommonResult<Void> batchAuditLeads(List<Long> ids, String auditStatus);

    /**
     * 更新客资信息（部分字段）
     */
    CommonResult<Void> updateLead(Long id, com.example.lead.dto.UpdateLeadRequest request);

    /**
     * 删除客资
     */
    CommonResult<Void> deleteLead(Long id);

    /**
     * 批量删除客资
     */
    CommonResult<Void> batchDeleteLeads(List<Long> ids);
}