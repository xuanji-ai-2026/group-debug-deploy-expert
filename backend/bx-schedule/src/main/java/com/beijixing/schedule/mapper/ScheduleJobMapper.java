package com.beijixing.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.schedule.entity.ScheduleJob;
import com.beijixing.schedule.vo.JobVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScheduleJobMapper extends BaseMapper<ScheduleJob> {

    List<JobVO> selectJobList();

    JobVO selectJobById(@Param("id") Long id);
}
