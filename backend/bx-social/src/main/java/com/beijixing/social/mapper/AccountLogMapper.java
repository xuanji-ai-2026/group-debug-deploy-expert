package com.beijixing.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.social.entity.AccountLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号操作日志Mapper
 */
@Mapper
public interface AccountLogMapper extends BaseMapper<AccountLog> {
}
