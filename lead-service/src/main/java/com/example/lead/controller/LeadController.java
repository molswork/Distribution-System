package com.example.lead.controller;

import com.example.lead.dto.CreateLeadRequest;
import com.example.lead.dto.CustomerLeadDto;
import com.example.lead.dto.LeadDetailsDto;
import com.example.lead.service.LeadService;
import com.example.lead.service.SourceDetectionService;
import com.example.common.dto.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 客资管理控制器
 *
 * @author System
 * @version 1.0
 * @since 2025-08-05
 */
@RestController
@RequestMapping("/api/leads")
@Tag(name = "客资管理", description = "客资管理相关接口")
public class LeadController {

    @Autowired
    private LeadService leadService;

    @Autowired
    private SourceDetectionService sourceDetectionService;

    @PostMapping("/create")
    @Operation(summary = "创建客资", description = "创建新的客户线索")
    public CommonResult<CustomerLeadDto> createLead(@Valid @RequestBody CreateLeadRequest request) {
        return leadService.createLead(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取客资详情", description = "根据ID获取客资详细信息")
    public CommonResult<LeadDetailsDto> getLeadById(
            @Parameter(description = "客资ID") @PathVariable Long id) {
        return leadService.getLeadById(id);
    }

    @GetMapping("/all")
    @Operation(summary = "获取客资列表", description = "分页查询客资列表（对齐API规范）")
    public CommonResult<com.example.lead.dto.PageResult<CustomerLeadDto>> getLeadList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "pageSize", defaultValue = "20") int size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "审核状态") @RequestParam(required = false) String auditStatus,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "来源") @RequestParam(required = false) String source,
            @Parameter(description = "销售ID") @RequestParam(required = false) Long salespersonId,
            @Parameter(description = "开始日期(YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期(YYYY-MM-DD)") @RequestParam(required = false) String endDate,
            @Parameter(description = "排序字段") @RequestParam(required = false) String sortBy,
            @Parameter(description = "排序方向(asc/desc)") @RequestParam(required = false) String sortOrder) {
        return leadService.getLeadList(page, size, salespersonId, status, auditStatus, keyword, source, startDate, endDate, sortBy, sortOrder);
    }

    @PostMapping("/check-duplicate")
    @Operation(summary = "检查重复", description = "根据手机号/姓名/微信号检查重复")
    public CommonResult<Boolean> checkDuplicate(@Valid @RequestBody com.example.lead.dto.DuplicateCheckRequest req) {
        // 当前 Service 仅支持 phone，后续扩展 DuplicateCheckService
        return leadService.checkDuplicate(req.getPhone());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新客资状态", description = "更新客资跟进状态")
    public CommonResult<Void> updateLeadStatus(
            @Parameter(description = "客资ID") @PathVariable Long id,
            @Valid @RequestBody com.example.lead.dto.UpdateLeadStatusRequest req) {
        return leadService.updateLeadStatus(id, req.getStatus());
    }

    @PostMapping("/batch-audit")
    @Operation(summary = "批量审核", description = "批量审核客资")
    public CommonResult<Void> batchAuditLeads(@Valid @RequestBody com.example.lead.dto.BatchAuditRequest req) {
        return leadService.batchAuditLeads(req.getIds(), req.getAuditStatus());
    }
    @PutMapping("/{id}")
    @Operation(summary = "更新客资", description = "更新客资信息（部分字段）")
    public CommonResult<Void> updateLead(
            @Parameter(description = "客资ID") @PathVariable Long id,
            @Valid @RequestBody com.example.lead.dto.UpdateLeadRequest request) {
        return leadService.updateLead(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除客资", description = "删除客资记录（当前为硬删除，后续改软删）")
    public CommonResult<Void> deleteLead(@Parameter(description = "客资ID") @PathVariable Long id) {
        return leadService.deleteLead(id);
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除客资", description = "批量删除多个客资")
    public CommonResult<Void> batchDeleteLeads(@Valid @RequestBody com.example.lead.dto.BatchDeleteRequest req) {
        return leadService.batchDeleteLeads(req.getIds());
    }

    @PostMapping("/detect-source")
    @Operation(summary = "来源检测")
    public CommonResult<java.util.Map<String,Object>> detectSource(@RequestBody com.example.lead.dto.SourceDetectionRequest request) {
        return sourceDetectionService.detectSource(request);
    }

    @PostMapping("/validate-source")
    @Operation(summary = "来源验证")
    public CommonResult<java.util.Map<String,Object>> validateSource(@RequestBody com.example.lead.dto.SourceValidationRequest request) {
        return sourceDetectionService.validateSource(request);
    }

    @GetMapping("/source-suggestions")
    @Operation(summary = "来源建议")
    public CommonResult<java.util.Map<String,Object>> getSourceSuggestions(@RequestParam(required = false) String keyword) {
        return sourceDetectionService.getSourceSuggestions(keyword);
    }

    @GetMapping("/statistics")
    @Operation(summary = "客资统计")
    public CommonResult<java.util.Map<String,Object>> getLeadStatistics(
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return sourceDetectionService.getLeadStatistics(auditStatus, status, source, salespersonId, startDate, endDate);
    }
}
