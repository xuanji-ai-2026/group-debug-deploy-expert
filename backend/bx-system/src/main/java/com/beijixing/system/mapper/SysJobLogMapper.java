package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务执行日志 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {

    /**
     * 查询任务最近执行日志
     *
     * @param jobId 任务ID
     * @param limit 返回数量
     * @return 日志列表
     */
    List<SysJobLog> selectRecentByJobId(@Param("jobId") Long jobId, @Param("limit") Integer limit);

    /**
     * 查询失败日志
     *
     * @param limit 返回数量
     * @return 失败日志列表
     */
    List<SysJobLog> selectFailedLogs(@Param("limit") Integer limit);
}
