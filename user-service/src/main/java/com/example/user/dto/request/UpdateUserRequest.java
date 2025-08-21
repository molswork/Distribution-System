package com.example.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * 更新用户请求DTO
 * 
 * <p>用于接收更新用户信息的请求参数，所有字段都是可选的，只更新提供的字段。
 * 该DTO支持部分更新，允许客户端只传递需要修改的字段。
 * 
 * <p>更新规则：
 * <ul>
 *   <li>所有字段都是可选的，null值表示不更新该字段</li>
 *   <li>邮箱和手机号更新时需要验证唯一性</li>
 *   <li>角色更新需要相应的权限验证</li>
 *   <li>状态更新会影响用户的登录和功能使用</li>
 * </ul>
 * 
 * @author User Service Team
 * @version 1.0.0
 * @since 2025-08-07
 */
@Data
@Schema(description = "更新用户请求")
public class UpdateUserRequest {
    
    /**
     * 用户名
     */
    @Size(min = 3, max = 64, message = "用户名长度必须在3-64字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "用户名只能包含字母、数字、下划线和中文")
    @Schema(description = "用户名", example = "张三")
    private String username;
    
    /**
     * 邮箱地址
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128字符")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;
    
    /**
     * 手机号码
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;
    
    /**
     * 用户角色
     */
    @Pattern(regexp = "^(super_admin|director|leader|sales|agent)$", 
             message = "角色必须是：super_admin、director、leader、sales、agent之一")
    @Schema(description = "用户角色", example = "sales", 
            allowableValues = {"super_admin", "director", "leader", "sales", "agent"})
    private String role;
    
    /**
     * 用户状态
     */
    @Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED)$", 
             message = "状态必须是：ACTIVE、INACTIVE、SUSPENDED之一")
    @Schema(description = "用户状态", example = "ACTIVE", 
            allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
    private String status;
    
    /**
     * 用户等级
     */
    @Min(value = 1, message = "用户等级不能小于1")
    @Max(value = 10, message = "用户等级不能大于10")
    @Schema(description = "用户等级", example = "3")
    private Integer level;
    
    /**
     * 提成比例
     */
    @DecimalMin(value = "0.0", message = "提成比例不能小于0")
    @DecimalMax(value = "1.0", message = "提成比例不能大于1")
    @Digits(integer = 1, fraction = 3, message = "提成比例最多3位小数")
    @Schema(description = "提成比例", example = "0.15")
    private Double commissionRate;
    
    /**
     * 上级用户ID
     */
    @Schema(description = "上级用户ID", example = "1001")
    private Long parentId;
}
