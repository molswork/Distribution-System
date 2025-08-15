package com.example.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "创建代理请求DTO")
public class AgentCreateRequest {

    @Schema(description = "姓名(作为用户名)", example = "张三")
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度需在2-50之间")
    private String name;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "等级", example = "standard")
    @NotBlank(message = "等级不能为空")
    private String level;

    @Schema(description = "上级代理UserId", example = "1001")
    private Long parentAgentId;

    @Schema(description = "佣金比例[0,1]", example = "0.10")
    @DecimalMin(value = "0.0000", message = "佣金比例不能为负数")
    @DecimalMax(value = "1.0000", message = "佣金比例不能超过100%")
    private BigDecimal commissionRate;

    @Schema(description = "初始密码(可选)", example = "Init@12345")
    private String password;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Long getParentAgentId() { return parentAgentId; }
    public void setParentAgentId(Long parentAgentId) { this.parentAgentId = parentAgentId; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

