package com.beijixing.bxuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.bxuser.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    List<LoginLog> findRecentByUserId(@Param("userId") Long userId);

    List<LoginLog> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
}
