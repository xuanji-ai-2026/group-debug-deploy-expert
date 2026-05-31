package com.beijixing.bxlead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.bxlead.dto.LeadQueryDTO;
import com.beijixing.bxlead.entity.Lead;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商机Mapper
 * @author 朱怡
 * @since 1.0.0
 */
public interface LeadMapper extends BaseMapper<Lead> {
    
    /**
     * 分页查询商机列表
     */
    IPage<Lead> selectLeadPage(Page<Lead> page, @Param("query") LeadQueryDTO query);
    
    /**
     * 根据状态统计数量
     */
    @Select("SELECT status, COUNT(*) as count FROM bx_lead WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();
    
    /**
     * 根据等级统计数量
     */
    @Select("SELECT level, COUNT(*) as count FROM bx_lead WHERE deleted = 0 GROUP BY level")
    List<Map<String, Object>> countByLevel();
    
    /**
     * 更新跟进信息
     */
    @Update("UPDATE bx_lead SET follow_count = follow_count + 1, last_follow_time = NOW(), update_time = NOW() WHERE id = #{leadId}")
    int updateFollowInfo(@Param("leadId") Long leadId);
    
    /**
     * 统计漏斗数据
     */
    List<Map<String, Object>> selectFunnelStats();
    
    /**
     * 查询可用的销售人员列表（含负责商机数，用于自动分配）
     */
    @Select("SELECT u.id as ownerId, u.nickname as ownerName, u.regions, u.industries, " +
            "COUNT(l.id) as leadCount " +
            "FROM bx_user u " +
            "LEFT JOIN bx_lead l ON l.owner_id = u.id AND l.deleted = 0 AND l.status != 'LOST' " +
            "WHERE u.status = 1 AND u.role_type IN ('SALES', 'SALES_MANAGER') " +
            "GROUP BY u.id")
    List<Map<String, Object>> selectAvailableOwners();
    
    /**
     * 根据ID查询销售人信息
     */
    @Select("SELECT id as ownerId, nickname as ownerName FROM bx_user WHERE id = #{ownerId}")
    Map<String, Object> selectOwnerById(@Param("ownerId") Long ownerId);
    
    /**
     * 查询过期待归档的商机（超过指定天数且状态为终态）
     */
    @Select("SELECT * FROM bx_lead WHERE deleted = 0 AND create_time < #{threshold} " +
            "AND status IN ('WON', 'LOST', 'INVALID')")
    List<Lead> selectExpiredLeadsForArchive(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 查询需要重新计算评分的商机（有需求描述但评分可能过时）
     */
    @Select("SELECT * FROM bx_lead WHERE deleted = 0 AND requirement_desc IS NOT NULL " +
            "AND requirement_desc != '' AND (intent_score IS NULL OR update_time < DATE_SUB(NOW(), INTERVAL 7 DAY)) " +
            "LIMIT 200")
    List<Lead> selectLeadsForScoreRecalculation();
    
    /**
     * 查询未分配的商机
     */
    @Select("SELECT * FROM bx_lead WHERE deleted = 0 AND owner_id IS NULL AND status = 'NEW' LIMIT #{limit}")
    List<Lead> selectUnassignedLeads(@Param("limit") int limit);
}