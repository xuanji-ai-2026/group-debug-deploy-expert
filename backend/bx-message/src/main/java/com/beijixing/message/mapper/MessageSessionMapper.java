package com.beijixing.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.message.entity.MessageSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息会话Mapper接口（统一技术栈 - MariaDB + MyBatis-Plus）
 *
 * @author 苏波（EMP-BE-001）
 */
@Mapper
public interface MessageSessionMapper extends BaseMapper<MessageSession> {
}