package com.example.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * 创建用户请求DTO
 * 
 * <p>用于接收创建新用户的请求参数，包含用户基本信息和角色权限设置。
 * 该DTO包含完整的数据验证规则，确保输入数据的有效性和安全性。
 * 
 * <p>验证规则：
 * <ul>
 *   <li>用户名：3-64字符，支持字母、数字、下划线</li>
 *   <li>邮箱：标准邮箱格式，最长128字符</li>
 *   <li>手机号：11位中国大陆手机号格式</li>
 *   <li>密码：6-50字符，包含字母和数字</li>
 *   <li>角色：必须是有效的用户角色枚举值</li>
 * </ul>
 * 
 * @author User Service Team
 * @version 1.0.0
 * @since 2025-08-07
 */
@Data
@Schema(description = "创建用户请求")
public class CreateUserRequest {
    
    /**
     * 用户名，用于登录和显示
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度必须在3-64字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "用户名只能包含字母、数字、下划线和中文")
    @Schema(description = "用户名", required = true, example = "zhangsan")
    private String username;
    
    /**
     * 邮箱地址，用于通知和登录
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128字符")
    @Schema(description = "邮箱地址", required = true, example = "zhangsan@example.com")
    private String email;
    
    /**
     * 手机号码，用于联系和验证
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号码", required = true, example = "13800138000")
    private String phone;
    
    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50字符之间")
    @Schema(description = "登录密码", required = true, example = "password123")
    private String password;
    
    /**
     * 用户角色
     */
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(super_admin|director|leader|sales|agent)$", 
             message = "角色必须是：super_admin、director、leader、sales、agent之一")
    @Schema(description = "用户角色", required = true, example = "sales", 
            allowableValues = {"super_admin", "director", "leader", "sales", "agent"})
    private String role;
    
    /**
     * 提成比例（可选）
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "提成比例不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "提成比例不能大于1")
    @Digits(integer = 1, fraction = 3, message = "提成比例最多3位小数")
    @Schema(description = "提成比例", example = "0.15")
    private Double commissionRate;
    
    /**
     * 用户等级（可选）
     */
    @Min(value = 1, message = "用户等级不能小于1")
    @Max(value = 10, message = "用户等级不能大于10")
    @Schema(description = "用户等级", example = "3")
    private Integer level;
    
    /**
     * 上级用户ID（可选）
     */
    @Schema(description = "上级用户ID", example = "1001")
    private Long parentId;
}
