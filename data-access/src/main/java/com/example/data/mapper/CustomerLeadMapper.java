package com.example.data.mapper;

// import com.example.common.annotation.DataTable;
import com.example.data.entity.CustomerLead;
import com.example.data.permission.DataPermission;
import com.example.data.permission.OperationType;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 客户资源数据访问接口
 *
 * @author Data Access Generator
 * @version 1.0
 * @since 2025-08-03
 */
@Repository
@Mapper
// @DataTable("customer_leads")
public interface CustomerLeadMapper {

    /**
     * 插入新客户资源
     *
     * @param customerLead 客户资源实体
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.CREATE, description = "创建客户资源")
    @Insert("INSERT INTO customer_leads (name, phone, wechat_id, status, audit_status, source, source_detail, salesperson_id, notes, last_follow_up_at, created_at, updated_at) " +
            "VALUES (#{name}, #{phone}, #{wechatId}, #{status}, #{auditStatus}, #{source}, #{sourceDetail}, #{salespersonId}, #{notes}, #{lastFollowUpAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CustomerLead customerLead);

    /**
     * 根据ID查找客户资源
     *
     * @param id 客户资源ID
     * @return 客户资源实体
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "根据ID查询客户资源")
    @Select("SELECT * FROM customer_leads WHERE id = #{id}")
    Optional<CustomerLead> findById(@Param("id") Long id);

    /**
     * 根据手机号查找客户资源
     *
     * @param phone 手机号
     * @return 客户资源实体
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "根据手机号查询客户资源")
    @Select("SELECT * FROM customer_leads WHERE phone = #{phone}")
    Optional<CustomerLead> findByPhone(@Param("phone") String phone);

    /**
     * 根据代理ID查找客户资源列表
     *
     * @param agentId 代理ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "根据销售ID查询客户资源")
    @Select("SELECT * FROM customer_leads WHERE salesperson_id = #{salespersonId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CustomerLead> findBySalespersonId(@Param("salespersonId") Long salespersonId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据跟进状态查找客户资源列表
     *
     * @param leadStatus 跟进状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "按跟进状态查询客户资源")
    @Select("SELECT * FROM customer_leads WHERE status = #{leadStatus} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CustomerLead> findByLeadStatus(@Param("leadStatus") String leadStatus,
                                       @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据审核状态查找客户资源列表
     *
     * @param auditStatus 审核状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @Select("SELECT * FROM customer_leads WHERE audit_status = #{auditStatus} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CustomerLead> findByAuditStatus(@Param("auditStatus") String auditStatus,
                                        @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查找需要跟进的客户资源（跟进日期已到或已过）
     *
     * @param currentDate 当前日期
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @Select("SELECT * FROM customer_leads WHERE last_follow_up_at IS NOT NULL AND last_follow_up_at <= #{currentDate} " +
            "AND status != 'CONVERTED' ORDER BY last_follow_up_at ASC LIMIT #{offset}, #{limit}")
    List<CustomerLead> findPendingFollowUp(@Param("currentDate") LocalDateTime currentDate,
                                          @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查找所有客户资源（分页）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @Select("SELECT * FROM customer_leads ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CustomerLead> findAll(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计客户资源总数
     *
     * @return 客户资源总数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.STATS, description = "统计客户资源总数")
    @Select("SELECT COUNT(*) FROM customer_leads")
    long count();

    /**
     * 根据代理ID统计客户资源数量
     *
     * @param agentId 代理ID
     * @return 客户资源数量
     */
    @DataPermission(table = "customer_leads", operation = OperationType.STATS, description = "按销售ID统计客户资源")
    @Select("SELECT COUNT(*) FROM customer_leads WHERE salesperson_id = #{salespersonId}")
    long countBySalespersonId(@Param("salespersonId") Long salespersonId);

    /**
     * 根据条件统计客户资源数量
     *
     * @param agentId 代理ID（可选）
     * @param leadStatus 跟进状态（可选）
     * @param auditStatus 审核状态（可选）
     * @return 客户资源数量
     */
    @DataPermission(table = "customer_leads", operation = OperationType.STATS, description = "按条件统计客户资源")
    long countByConditions(@Param("salespersonId") Long salespersonId, @Param("status") String status,
                          @Param("auditStatus") String auditStatus, @Param("keyword") String keyword,
                          @Param("source") String source, @Param("startDate") String startDate,
                          @Param("endDate") String endDate);

    /**
     * 更新客户资源信息
     *
     * @param customerLead 客户资源实体
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.UPDATE, description = "更新客户资源信息")
    @Update("UPDATE customer_leads SET name = #{name}, phone = #{phone}, wechat_id = #{wechatId}, " +
            "status = #{status}, audit_status = #{auditStatus}, source = #{source}, source_detail = #{sourceDetail}, " +
            "salesperson_id = #{salespersonId}, notes = #{notes}, last_follow_up_at = #{lastFollowUpAt}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(CustomerLead customerLead);

    /**
     * 更新跟进状态和跟进时间
     *
     * @param id 客户资源ID
     * @param leadStatus 新跟进状态
     * @param followUpDate 下次跟进时间
     * @param updatedAt 更新时间
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.UPDATE, description = "更新跟进状态")
    @Update("UPDATE customer_leads SET status = #{leadStatus}, last_follow_up_at = #{lastFollowUpAt}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int updateFollowUp(@Param("id") Long id, @Param("leadStatus") String leadStatus,
                      @Param("lastFollowUpAt") LocalDateTime lastFollowUpAt, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新审核状态
     *
     * @param id 客户资源ID
     * @param auditStatus 新审核状态
     * @param updatedAt 更新时间
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.UPDATE, description = "更新审核状态")
    @Update("UPDATE customer_leads SET audit_status = #{auditStatus}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateAuditStatus(@Param("id") Long id, @Param("auditStatus") String auditStatus,
                         @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据ID删除客户资源（硬删除）
     *
     * @param id 客户资源ID
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.DELETE, description = "删除客户资源")
    @Delete("DELETE FROM customer_leads WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @param excludeId 排除的客户资源ID（用于更新时检查）
     * @return 是否存在
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "检查手机号是否存在")
    @Select("SELECT COUNT(*) > 0 FROM customer_leads WHERE phone = #{phone} " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    boolean existsByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * 根据关键词搜索客户资源
     *
     * @param keyword 关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "搜索客户资源")
    List<CustomerLead> searchCustomerLeads(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据条件查找客户资源列表
     *
     * @param agentId 代理ID（可选）
     * @param leadStatus 跟进状态（可选）
     * @param auditStatus 审核状态（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 客户资源列表
     */
    @DataPermission(table = "customer_leads", operation = OperationType.READ, description = "按条件查询客户资源")
    List<CustomerLead> findByConditions(@Param("salespersonId") Long salespersonId, @Param("status") String status,
                                       @Param("auditStatus") String auditStatus, @Param("keyword") String keyword,
                                       @Param("source") String source, @Param("startDate") String startDate,
                                       @Param("endDate") String endDate, @Param("sortBy") String sortBy,
                                       @Param("sortOrder") String sortOrder, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 批量更新跟进状态
     *
     * @param ids 客户资源ID列表
     * @param leadStatus 新跟进状态
     * @param updatedAt 更新时间
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.UPDATE, description = "批量更新跟进状态")
    int batchUpdateLeadStatus(@Param("ids") List<Long> ids, @Param("leadStatus") String leadStatus,
                             @Param("updatedAt") LocalDateTime updatedAt);
    /**
     * 批量更新审核状态
     *
     * @param ids 客户资源ID列表
     * @param auditStatus 新审核状态
     * @param updatedAt 更新时间
     * @return 影响行数
     */
    @DataPermission(table = "customer_leads", operation = OperationType.UPDATE, description = "批量更新审核状态")
    int batchUpdateAuditStatus(@Param("ids") java.util.List<Long> ids,
                               @Param("auditStatus") String auditStatus,
                               @Param("updatedAt") java.time.LocalDateTime updatedAt);

}