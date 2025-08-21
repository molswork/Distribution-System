package com.example.lead.entity;

import java.time.LocalDateTime;

/**
 * 客资审核记录实体类
 * 
 * <p>对应数据库表 lead_audit_records，用于记录客资审核的历史记录。
 * 该实体记录了每次审核操作的详细信息，包括审核人、审核结果、审核意见等。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>记录客资审核的完整历史</li>
 *   <li>跟踪审核状态的变化过程</li>
 *   <li>存储审核意见和拒绝原因</li>
 *   <li>关联审核人信息</li>
 * </ul>
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-20
 */
public class LeadAuditRecord {
    
    /**
     * 审核记录ID
     */
    private Long id;
    
    /**
     * 关联的客资ID
     */
    private Long leadId;
    
    /**
     * 执行审核的操作员ID
     */
    private Long auditorId;
    
    /**
     * 审核前的状态
     */
    private String statusBefore;
    
    /**
     * 审核后的状态
     */
    private String statusAfter;
    
    /**
     * 审核备注或意见
     */
    private String comment;
    
    /**
     * 如果拒绝，填写拒绝原因
     */
    private String rejectReason;
    
    /**
     * 审核操作发生的时间
     */
    private LocalDateTime auditedAt;
    
    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 记录更新时间
     */
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public Long getAuditorId() { return auditorId; }
    public void setAuditorId(Long auditorId) { this.auditorId = auditorId; }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
