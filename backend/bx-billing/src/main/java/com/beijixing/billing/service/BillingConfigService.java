package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.entity.BillingConfig;
import com.beijixing.billing.mapper.BillingConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 计费配置服务
 * BL-010: 扣点标准配置API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingConfigService {
    
    private final BillingConfigMapper billingConfigMapper;
    
    /**
     * 获取配置值
     */
    @Cacheable(value = "billingConfig", key = "#configCode")
    public String getConfigValue(String configCode) {
        BillingConfig config = billingConfigMapper.selectByCode(configCode);
        return config != null ? config.getConfigValue() : null;
    }
    
    /**
     * 获取Token单价
     */
    public int getTokenUnitPrice() {
        String value = getConfigValue("token_unit_price");
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid token_unit_price config: {}", value);
            }
        }
        return BillingConstants.TOKEN_UNIT_PRICE;
    }
    
    /**
     * 获取资源占用单价
     */
    public int getResourceUnitPrice() {
        String value = getConfigValue("resource_unit_price");
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid resource_unit_price config: {}", value);
            }
        }
        return BillingConstants.RESOURCE_USAGE_FEE_PER_MIN;
    }
    
    /**
     * 获取所有配置
     */
    public List<BillingConfig> getAllConfigs() {
        return billingConfigMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillingConfig>()
                .eq(BillingConfig::getEnabled, 1)
        );
    }
    
    /**
     * 根据类型获取配置
     */
    public List<BillingConfig> getConfigsByType(String configType) {
        return billingConfigMapper.selectByType(configType);
    }
    
    /**
     * 更新配置
     */
    @Transactional
       public boolean updateConfig(Long configId, String configValue) {
        BillingConfig config = billingConfigMapper.selectById(configId);
        if (config == null) {
            return false;
        }
        config.setConfigValue(configValue);
        billingConfigMapper.updateById(config);
        return true;
    }
    
    /**
     * 新增配置
     */
    @Transactional
    public boolean addConfig(BillingConfig config) {
        // 检查编码是否已存在
        BillingConfig exist = billingConfigMapper.selectByCode(config.getConfigCode());
        if (exist != null) {
            return false;
        }
        config.setEnabled(1);
        billingConfigMapper.insert(config);
        return true;
    }
    
    /**
     * 删除配置
     */
    @Transactional
    public boolean deleteConfig(Long configId) {
        billingConfigMapper.deleteById(configId);
        return true;
    }
}
