package com.beijixing.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.schedule.entity.ScheduleJobRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScheduleJobRegistryMapper extends BaseMapper<ScheduleJobRegistry> {

    List<ScheduleJobRegistry> selectOnlineRegistries(@Param("appName") String appName);
}
