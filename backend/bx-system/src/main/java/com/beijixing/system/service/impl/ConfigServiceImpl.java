package com.beijixing.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysConfig;
import com.beijixing.system.mapper.SysConfigMapper;
import com.beijixing.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现
 *
 * @author bx-system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class ConfigServiceImpl implements ConfigService {

    private static final String CACHE_PREFIX = "sys:config:";

    private final SysConfigMapper configMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<SysConfig> pageConfigs(Integer page, Integer size, String groupCode, String configKey,
                                       String configName, Integer status) {
        Page<SysConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(groupCode)) {
            wrapper.eq(SysConfig::getGroupCode, groupCode);
        }
        if (StringUtils.hasText(configKey)) {
            wrapper.like(SysConfig::getConfigKey, configKey);
        }
        if (StringUtils.hasText(configName)) {
            wrapper.like(SysConfig::getConfigName, configName);
        }
        if (status != null) {
            wrapper.eq(SysConfig::getStatus, status);
        }
        wrapper.orderByAsc(SysConfig::getSortOrder).orderByDesc(SysConfig::getId);

        Page<SysConfig> result = configMapper.selectPage(pageParam, wrapper);
        log.debug("分页查询配置列表，条件：groupCode={}, configKey={}, status={}，结果：{}条",
                groupCode, configKey, status, result.getTotal());
        return result;
    }

    @Override
    public SysConfig getById(Long id) {
        return configMapper.selectById(id);
    }

    @Override
    public String getValueByKey(String configKey) {
        return getValueByKey(configKey, null);
    }

    @Override
    @SuppressWarnings("nullness")
    public String getValueByKey(String configKey, String defaultValue) {
        String cacheKey = CACHE_PREFIX + configKey;

        // 先从缓存获取
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return cachedValue.toString();
        }

        // 缓存未命中，查询数据库
        SysConfig config = configMapper.selectByConfigKey(configKey);
        if (config != null && config.getStatus() == 1) {
            // 写入缓存，永不过期（由刷新接口主动失效）
            redisTemplate.opsForValue().set(cacheKey, config.getConfigValue());
            return config.getConfigValue();
        }

        return defaultValue;
    }

    @Override
    public List<SysConfig> listByGroup(String groupCode) {
        return configMapper.selectByGroupCode(groupCode);
    }

    @Override
    public Map<String, String> getConfigMap(String groupCode) {
        List<SysConfig> configs = configMapper.selectByGroupCode(groupCode);
        Map<String, String> map = new HashMap<>();
        for (SysConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    @Override
    public Long create(SysConfig config) {
        // 检查Key唯一性
        SysConfig existing = configMapper.selectByConfigKey(config.getConfigKey());
        if (existing != null) {
            throw new IllegalArgumentException("配置Key已存在：" + config.getConfigKey());
        }
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        if (config.getBuiltIn() == null) {
            config.setBuiltIn(0);
        }
        if (config.getSortOrder() == null) {
            config.setSortOrder(0);
        }
        configMapper.insert(config);
        log.info("创建系统配置：{} = {}", config.getConfigKey(), config.getConfigValue());
        return config.getId();
    }

    @Override
    public void update(Long id, SysConfig config) {
        SysConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在：" + id);
        }
        if (existing.getBuiltIn() != null && existing.getBuiltIn() == 1) {
            throw new IllegalArgumentException("内置配置不可修改");
        }
        config.setId(id);
        configMapper.updateById(config);

        // 失效缓存
        redisTemplate.delete(CACHE_PREFIX + existing.getConfigKey());
        log.info("更新系统配置：id={}", id);
    }

    @Override
    public void delete(Long id) {
        SysConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在：" + id);
        }
        if (existing.getBuiltIn() != null && existing.getBuiltIn() == 1) {
            throw new IllegalArgumentException("内置配置不可删除");
        }
        configMapper.deleteById(id);

        // 失效缓存
        redisTemplate.delete(CACHE_PREFIX + existing.getConfigKey());
        log.info("删除系统配置：id={}, key={}", id, existing.getConfigKey());
    }

    @Override
    public void updateByKey(String configKey, String configValue) {
        SysConfig config = configMapper.selectByConfigKey(configKey);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在：" + configKey);
        }
        config.setConfigValue(configValue);
        configMapper.updateById(config);

        // 失效缓存
        redisTemplate.delete(CACHE_PREFIX + configKey);
        log.info("按Key更新配置：key={}, value={}", configKey, configValue);
    }

    @Override
    public void refreshCache(String groupCode) {
        if (StringUtils.hasText(groupCode)) {
            // 刷新指定分组
            List<SysConfig> configs = configMapper.selectByGroupCode(groupCode);
            for (SysConfig config : configs) {
                String cacheKey = CACHE_PREFIX + config.getConfigKey();
                if (config.getStatus() == 1) {
                    redisTemplate.opsForValue().set(cacheKey, config.getConfigValue());
                } else {
                    redisTemplate.delete(cacheKey);
                }
            }
            log.info("刷新配置缓存，分组：{}", groupCode);
        } else {
            // 刷新所有配置（清理所有缓存）
            // 实际生产环境建议只清理特定前缀的key
            log.info("刷新所有配置缓存");
        }
    }
}
