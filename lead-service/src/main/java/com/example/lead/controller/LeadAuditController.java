package com.example.lead.controller;

import com.example.common.dto.CommonResult;
import com.example.lead.dto.BatchAuditRequest;
import com.example.lead.dto.CustomerLeadDto;
import com.example.lead.dto.LeadAuditRecordDto;
import com.example.lead.dto.PageResult;
import com.example.lead.service.LeadAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/leads/audit")
@Tag(name = "客资审核", description = "客资审核相关接口")
public class LeadAuditController {

    private final LeadAuditService auditService;

    public LeadAuditController(LeadAuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/{id}")
    @Operation(summary = "审核单个客资")
    public CommonResult<CustomerLeadDto> auditOne(@PathVariable("id") Long id, @RequestBody java.util.Map<String, String> body) {
        String decision = body.get("decision");
        String comment = body.get("comment");
        String rejectReason = body.get("rejectReason");
        return auditService.auditOne(id, decision, comment, rejectReason);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量审核")
    public CommonResult<Void> batchAudit(@Valid @RequestBody BatchAuditRequest request) {
        return auditService.batchAudit(request);
    }

    @GetMapping("/pending")
    @Operation(summary = "待审核列表")
    public CommonResult<PageResult<CustomerLeadDto>> pending(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Long salespersonId) {
        return auditService.listPending(page, pageSize, keyword, salespersonId);
    }

    @GetMapping("/all")
    @Operation(summary = "审核列表")
    public CommonResult<PageResult<CustomerLeadDto>> all(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String auditStatus,
                                                         @RequestParam(required = false) Long salespersonId) {
        return auditService.listAll(page, pageSize, keyword, auditStatus, salespersonId);
    }

    @PostMapping("/check-permission")
    @Operation(summary = "审核权限检查")
    public CommonResult<Boolean> checkPermission(@RequestBody java.util.Map<String, Object> body) {
        Long leadId = body.get("leadId") == null ? null : Long.valueOf(body.get("leadId").toString());
        Long salespersonId = body.get("salespersonId") == null ? null : Long.valueOf(body.get("salespersonId").toString());
        return auditService.checkPermission(leadId, salespersonId);
    }

    @GetMapping("/records")
    @Operation(summary = "审核记录")
    public CommonResult<PageResult<CustomerLeadDto>> records(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String auditStatus,
                                                             @RequestParam(required = false) Long salespersonId,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate,
                                                             @RequestParam(required = false) String sortBy,
                                                             @RequestParam(required = false) String sortOrder) {
        return auditService.getAuditRecords(page, pageSize, keyword, auditStatus, salespersonId, startDate, endDate, sortBy, sortOrder);
    }

    @GetMapping("/statistics")
    @Operation(summary = "审核统计")
    public CommonResult<java.util.Map<String,Object>> statistics(@RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String source,
                                                                  @RequestParam(required = false) Long salespersonId,
                                                                  @RequestParam(required = false) String startDate,
                                                                  @RequestParam(required = false) String endDate) {
        return auditService.getAuditStatistics(status, source, salespersonId, startDate, endDate);
    }

    @GetMapping("/scope")
    @Operation(summary = "审核范围")
    public CommonResult<Object> scope() {
        return auditService.auditScope();
    }

    @GetMapping("/records/{leadId}")
    @Operation(summary = "获取指定客资的审核记录")
    public CommonResult<java.util.List<LeadAuditRecordDto>> getLeadAuditRecords(@PathVariable Long leadId) {
        return auditService.getLeadAuditRecords(leadId);
    }
}

