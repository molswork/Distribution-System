package com.example.lead.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客资审核记录DTO
 * 用于客资审核记录的数据传输
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-20
 */
@Schema(description = "客资审核记录")
public class LeadAuditRecordDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 审核记录ID
     */
    @Schema(description = "审核记录ID", example = "1")
    private Long id;
    
    /**
     * 关联的客资ID
     */
    @Schema(description = "客资ID", example = "123")
    private Long leadId;
    
    /**
     * 执行审核的操作员ID
     */
    @Schema(description = "审核员ID", example = "5")
    private Long auditorId;
    
    /**
     * 审核员姓名
     */
    @Schema(description = "审核员姓名", example = "张三")
    private String auditorName;
    
    /**
     * 审核员角色
     */
    @Schema(description = "审核员角色", example = "director")
    private String auditorRole;
    
    /**
     * 审核前的状态
     */
    @Schema(description = "审核前状态", example = "PENDING_AUDIT")
    private String statusBefore;
    
    /**
     * 审核后的状态
     */
    @Schema(description = "审核后状态", example = "APPROVED")
    private String statusAfter;
    
    /**
     * 审核备注或意见
     */
    @Schema(description = "审核意见", example = "客资质量良好，符合审核标准")
    private String comment;
    
    /**
     * 如果拒绝，填写拒绝原因
     */
    @Schema(description = "拒绝原因", example = "客资信息不完整")
    private String rejectReason;
    
    /**
     * 审核操作发生的时间
     */
    @Schema(description = "审核时间", example = "2025-08-20T15:30:00")
    private LocalDateTime auditedAt;
    
    /**
     * 记录创建时间
     */
    @Schema(description = "创建时间", example = "2025-08-20T15:30:00")
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public Long getAuditorId() { return auditorId; }
    public void setAuditorId(Long auditorId) { this.auditorId = auditorId; }

    public String getAuditorName() { return auditorName; }
    public void setAuditorName(String auditorName) { this.auditorName = auditorName; }

    public String getAuditorRole() { return auditorRole; }
    public void setAuditorRole(String auditorRole) { this.auditorRole = auditorRole; }

    public String getStatusBefore() { return statusBefore; }
    public void setStatusBefore(String statusBefore) { this.statusBefore = statusBefore; }

    public String getStatusAfter() { return statusAfter; }
    public void setStatusAfter(String statusAfter) { this.statusAfter = statusAfter; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getAuditedAt() { return auditedAt; }
    public void setAuditedAt(LocalDateTime auditedAt) { this.auditedAt = auditedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
