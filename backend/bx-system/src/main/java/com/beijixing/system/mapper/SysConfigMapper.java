package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置Key查询
     *
     * @param configKey 配置Key
     * @return SysConfig
     */
    SysConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 根据分组查询配置列表
     *
     * @param groupCode 分组编码
     * @return 配置列表
     */
    List<SysConfig> selectByGroupCode(@Param("groupCode") String groupCode);
}
