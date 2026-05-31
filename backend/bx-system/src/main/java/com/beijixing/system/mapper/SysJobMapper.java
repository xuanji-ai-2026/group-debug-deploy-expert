package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 定时任务 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {

    /**
     * 查询运行中的任务
     *
     * @return 运行中的任务列表
     */
    List<SysJob> selectRunningJobs();

    /**
     * 查询暂停的任务
     *
     * @return 暂停的任务列表
     */
    List<SysJob> selectPausedJobs();

    /**
     * 根据任务分组查询
     *
     * @param jobGroup 任务分组
     * @return 任务列表
     */
    List<SysJob> selectByJobGroup(@Param("jobGroup") String jobGroup);
}
