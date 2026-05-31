package com.beijixing.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.content.entity.ContentAuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内容审核记录Mapper
 * @author 胡云 (EMP-CONTENT-001)
 */
@Mapper
public interface ContentAuditRecordMapper extends BaseMapper<ContentAuditRecord> {

    /**
     * 查询内容的审核记录
     */
    @Select("SELECT * FROM content_audit_record WHERE content_id = #{contentId} ORDER BY create_time DESC")
    List<ContentAuditRecord> selectByContentId(@Param("contentId") Long contentId);

    /**
     * 查询最新的审核记录
     */
    @Select("SELECT * FROM content_audit_record WHERE content_id = #{contentId} ORDER BY create_time DESC LIMIT 1")
    ContentAuditRecord selectLatestByContentId(@Param("contentId") Long contentId);
}
