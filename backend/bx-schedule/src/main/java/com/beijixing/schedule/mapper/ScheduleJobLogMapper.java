package com.beijixing.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.schedule.entity.ScheduleJobLog;
import com.beijixing.schedule.vo.JobLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScheduleJobLogMapper extends BaseMapper<ScheduleJobLog> {

    List<JobLogVO> selectJobLogList(@Param("jobId") Long jobId,
                                    @Param("status") Integer status,
                                    @Param("startTime") String startTime,
                                    @Param("endTime") String endTime);

    JobLogVO selectJobLogById(@Param("id") Long id);

    List<JobLogVO> selectRecentLogs(@Param("limit") Integer limit);
}
