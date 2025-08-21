package com.example.lead.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "更新客资请求")
public class UpdateLeadRequest implements Serializable {
    @Schema(description = "姓名", example = "张三-更新")
    private String name;
    @Schema(description = "手机号", example = "13900139010")
    private String phone;
    @Schema(description = "微信号", example = "zhangsan_new")
    private String wechatId;
    @Schema(description = "邮箱（预留）")
    private String email;
    @Schema(description = "备注", example = "第二次回访安排周五")
    private String notes;
    @Schema(description = "客资状态", example = "FOLLOWING", allowableValues = {"PENDING", "FOLLOWING", "CONVERTED", "INVALID"})
    private String status;
    @Schema(description = "来源渠道", example = "微信")
    private String source;
    @Schema(description = "来源详情", example = "朋友圈广告")
    private String sourceDetail;
    @Schema(description = "归属销售ID", example = "123")
    private Long salespersonId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWechatId() { return wechatId; }
    public void setWechatId(String wechatId) { this.wechatId = wechatId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceDetail() { return sourceDetail; }
    public void setSourceDetail(String sourceDetail) { this.sourceDetail = sourceDetail; }
    public Long getSalespersonId() { return salespersonId; }
    public void setSalespersonId(Long salespersonId) { this.salespersonId = salespersonId; }
}

