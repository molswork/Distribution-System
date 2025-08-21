package com.example.lead.mapper;

import com.example.lead.dto.LeadAuditRecordDto;
import com.example.lead.entity.LeadAuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客资审核记录数据访问层接口
 * 
 * <p>负责客资审核记录表（lead_audit_records）的数据访问操作，
 * 提供审核记录的增删改查功能。使用 MyBatis 框架实现数据持久化。
 * 
 * @author System
 * @version 1.0
 * @since 2025-08-20
 */
@Mapper
public interface LeadAuditRecordMapper {
    
    /**
     * 插入审核记录
     *
     * @param record 审核记录信息
     * @return 影响行数
     */
    int insert(LeadAuditRecord record);
    
    /**
     * 根据ID查询审核记录
     *
     * @param id 审核记录ID
     * @return 审核记录信息
     */
    LeadAuditRecord selectById(@Param("id") Long id);
    
    /**
     * 根据客资ID查询审核记录列表
     *
     * @param leadId 客资ID
     * @return 审核记录列表
     */
    List<LeadAuditRecordDto> selectByLeadId(@Param("leadId") Long leadId);
    
    /**
     * 分页查询审核记录列表（带审核员信息）
     *
     * @param offset 偏移量
     * @param size 页面大小
     * @param leadId 客资ID（可选）
     * @param auditorId 审核员ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 审核记录列表
     */
    List<LeadAuditRecordDto> selectAuditRecords(@Param("offset") int offset,
                                               @Param("size") int size,
                                               @Param("leadId") Long leadId,
                                               @Param("auditorId") Long auditorId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);
    
    /**
     * 统计审核记录数量
     *
     * @param leadId 客资ID（可选）
     * @param auditorId 审核员ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 记录数量
     */
    int countAuditRecords(@Param("leadId") Long leadId,
                         @Param("auditorId") Long auditorId,
                         @Param("startDate") String startDate,
                         @Param("endDate") String endDate);
    
    /**
     * 更新审核记录
     *
     * @param record 审核记录信息
     * @return 影响行数
     */
    int update(LeadAuditRecord record);
    
    /**
     * 删除审核记录
     *
     * @param id 审核记录ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
