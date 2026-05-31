package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    /**
     * 查询最近的日志
     *
     * @param limit 返回数量
     * @return 日志列表
     */
    List<SysOperLog> selectRecent(@Param("limit") Integer limit);

    /**
     * 根据时间范围查询日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<SysOperLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户操作日志
     *
     * @param userId 用户ID
     * @param limit 返回数量
     * @return 日志列表
     */
    List<SysOperLog> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
}
