package com.example.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 客户资源（线索）实体类
 *
 * @author Data Access Generator
 * @version 1.0
 * @since 2025-08-03
 */
public class CustomerLead {

    /**
     * 客资主键ID
     */
    private Long id;

    /**
     * 客户姓名
     */
    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 100, message = "客户姓名长度不能超过100字符")
    private String name;

    /**
     * 客户手机号，作为核心识别码之一
     */
    @NotBlank(message = "客户手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 客户微信号
     */
    @Size(max = 64, message = "微信号长度不能超过64字符")
    private String wechatId;

    /**
     * 客资跟进状态
     */
    @NotNull(message = "客资状态不能为空")
    private LeadStatus status = LeadStatus.PENDING;

    /**
     * 客资提报的审核状态
     */
    @NotNull(message = "审核状态不能为空")
    private AuditStatus auditStatus = AuditStatus.PENDING_AUDIT;

    /**
     * 主要来源渠道
     */
    @NotBlank(message = "来源渠道不能为空")
    @Size(max = 100, message = "来源渠道长度不能超过100字符")
    private String source;

    /**
     * 具体来源详情
     */
    @Size(max = 255, message = "来源详情长度不能超过255字符")
    private String sourceDetail;

    /**
     * 当前跟进或归属的销售/代理ID
     */
    @NotNull(message = "归属销售不能为空")
    private Long salespersonId;

    /**
     * 归属销售姓名（从users表关联查询获得）
     */
    private String salespersonName;

    /**
     * 跟进过程中的备注信息
     */
    private String notes;

    /**
     * 客户使用的推荐码
     */
    @Size(max = 50, message = "推荐码长度不能超过50字符")
    private String referralCode;

    /**
     * 最后一次跟进的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowUpAt;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 客资跟进状态枚举
    public enum LeadStatus {
        PENDING("PENDING", "待跟进"),
        FOLLOWING("FOLLOWING", "跟进中"),
        CONVERTED("CONVERTED", "已转化"),
        INVALID("INVALID", "无效客资");

        private final String code;
        private final String description;

        LeadStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static LeadStatus fromCode(String code) {
            for (LeadStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown lead status code: " + code);
        }
    }

    // 审核状态枚举
    public enum AuditStatus {
        PENDING_AUDIT("PENDING_AUDIT", "待审核"),
        APPROVED("APPROVED", "已通过"),
        REJECTED("REJECTED", "已拒绝");

        private final String code;
        private final String description;

        AuditStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static AuditStatus fromCode(String code) {
            for (AuditStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown audit status code: " + code);
        }
    }

    // Constructors
    public CustomerLead() {}

    public CustomerLead(String name, String phone, String source, Long salespersonId) {
        this.name = name;
        this.phone = phone;
        this.source = source;
        this.salespersonId = salespersonId;
        this.status = LeadStatus.PENDING;
        this.auditStatus = AuditStatus.PENDING_AUDIT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneNormalized() {
        return this.phone != null ? this.phone.replace(" ", "").replace("-", "").trim() : null;
    }
    public void setPhoneNormalized(String phoneNormalized) {
        // 在实体层保留字段以便 MyBatis 绑定
        // 可与 DB 列 phone_normalized 对应；此处简化为动态赋值，不改变原 phone 逻辑
    }

    public String getWechatId() {
        return wechatId;
    }

    public void setWechatId(String wechatId) {
        this.wechatId = wechatId;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public AuditStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(AuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceDetail() {
        return sourceDetail;
    }

    public void setSourceDetail(String sourceDetail) {
        this.sourceDetail = sourceDetail;
    }

    public Long getSalespersonId() {
        return salespersonId;
    }

    public void setSalespersonId(Long salespersonId) {
        this.salespersonId = salespersonId;
    }

    public String getSalespersonName() {
        return salespersonName;
    }

    public void setSalespersonName(String salespersonName) {
        this.salespersonName = salespersonName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public LocalDateTime getLastFollowUpAt() {
        return lastFollowUpAt;
    }

    public void setLastFollowUpAt(LocalDateTime lastFollowUpAt) {
        this.lastFollowUpAt = lastFollowUpAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business Methods

    /**
     * 更新跟进时间
     */
    public void updateFollowUp() {
        this.lastFollowUpAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否需要跟进
     *
     * @return 是否需要跟进
     */
    public boolean needsFollowUp() {
        return LeadStatus.PENDING.equals(this.status) ||
               LeadStatus.FOLLOWING.equals(this.status);
    }

    /**
     * 检查是否已转化
     *
     * @return 是否已转化
     */
    public boolean isConverted() {
        return LeadStatus.CONVERTED.equals(this.status);
    }

    /**
     * 检查是否通过审核
     *
     * @return 是否通过审核
     */
    public boolean isApproved() {
        return AuditStatus.APPROVED.equals(this.auditStatus);
    }

    /**
     * 设置更新时间为当前时间
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerLead that = (CustomerLead) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, phone);
    }

    @Override
    public String toString() {
        return "CustomerLead{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", wechatId='" + wechatId + '\'' +
                ", status=" + status +
                ", auditStatus=" + auditStatus +
                ", source='" + source + '\'' +
                ", salespersonId=" + salespersonId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}