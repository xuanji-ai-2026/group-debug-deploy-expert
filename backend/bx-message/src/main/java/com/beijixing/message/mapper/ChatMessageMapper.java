package com.beijixing.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.message.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper接口（统一技术栈 - MariaDB + MyBatis-Plus）
 *
 * @author 苏波（EMP-BE-001）
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}