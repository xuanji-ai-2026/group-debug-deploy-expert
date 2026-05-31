package com.beijixing.social.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.social.message.entity.MessageTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplate> {

    @Select("SELECT * FROM message_template WHERE platform_code = #{platformCode} AND intent_level = #{intentLevel} AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order ASC, use_count DESC")
    List<MessageTemplate> selectByPlatformAndIntent(@Param("platformCode") String platformCode, @Param("intentLevel") String intentLevel);

    @Select("SELECT * FROM message_template WHERE is_default = 1 AND is_enabled = 1 AND deleted = 0 LIMIT 1")
    MessageTemplate selectDefault();

    @Select("SELECT * FROM message_template WHERE template_type = #{templateType} AND is_enabled = 1 AND deleted = 0 ORDER BY success_rate DESC LIMIT #{limit}")
    List<MessageTemplate> selectTopByType(@Param("templateType") String templateType, @Param("limit") int limit);
}
