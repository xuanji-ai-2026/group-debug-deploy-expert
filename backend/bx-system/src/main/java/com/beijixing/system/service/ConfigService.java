package com.beijixing.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 *
 * 功能：SM-001 参数配置（系统参数、动态配置）
 *
 * @author bx-system
 */
public interface ConfigService {

    /**
     * 分页查询配置列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param groupCode 分组编码
     * @param configKey 配置Key（模糊搜索）
     * @param configName 配置名称（模糊搜索）
     * @param status 状态
     * @return 分页结果
     */
    Page<SysConfig> pageConfigs(Integer page, Integer size, String groupCode, String configKey, String configName, Integer status);

    /**
     * 根据ID获取配置
     *
     * @param id 配置ID
     * @return 配置实体
     */
    SysConfig getById(Long id);

    /**
     * 根据配置Key获取值
     *
     * @param configKey 配置Key
     * @return 配置值
     */
    String getValueByKey(String configKey);

    /**
     * 根据配置Key获取值，带默认值
     *
     * @param configKey 配置Key
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getValueByKey(String configKey, String defaultValue);

    /**
     * 根据分组获取所有配置
     *
     * @param groupCode 分组编码
     * @return 配置列表
     */
    List<SysConfig> listByGroup(String groupCode);

    /**
     * 根据分组获取配置Map
     *
     * @param groupCode 分组编码
     * @return 配置Map（key -> value）
     */
    Map<String, String> getConfigMap(String groupCode);

    /**
     * 创建配置
     *
     * @param config 配置实体
     * @return 配置ID
     */
    Long create(SysConfig config);

    /**
     * 更新配置
     *
     * @param id 配置ID
     * @param config 配置实体
     */
    void update(Long id, SysConfig config);

    /**
     * 删除配置
     *
     * @param id 配置ID
     */
    void delete(Long id);

    /**
     * 根据Key更新配置值
     *
     * @param configKey 配置Key
     * @param configValue 配置值
     */
    void updateByKey(String configKey, String configValue);

    /**
     * 刷新配置缓存
     *
     * @param groupCode 分组编码（为空则刷新所有）
     */
    void refreshCache(String groupCode);
}
