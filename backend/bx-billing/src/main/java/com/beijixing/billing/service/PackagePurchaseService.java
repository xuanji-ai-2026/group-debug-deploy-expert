package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.BillingOrderDTO;
import com.beijixing.billing.dto.PackagePurchaseDTO;
import com.beijixing.billing.entity.BillingOrder;
import com.beijixing.billing.entity.PackagePurchase;
import com.beijixing.billing.mapper.BillingOrderMapper;
import com.beijixing.billing.mapper.PackagePurchaseMapper;
import com.beijixing.billing.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class PackagePurchaseService {
    
    private final PackagePurchaseMapper packagePurchaseMapper;
    private final BillingOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    
    private static final Map<String, PackageConfig> PACKAGE_CONFIGS = new ConcurrentHashMap<>();
    
    static {
        PACKAGE_CONFIGS.put(BillingConstants.PACKAGE_BASIC, 
            new PackageConfig("基础套餐", 100000L, 30, 9900L));
        PACKAGE_CONFIGS.put(BillingConstants.PACKAGE_ADVANCED, 
            new PackageConfig("高级套餐", 500000L, 30, 39900L));
        PACKAGE_CONFIGS.put(BillingConstants.PACKAGE_ANNUAL, 
            new PackageConfig("年度套餐", 5000000L, 365, 299900L));
        PACKAGE_CONFIGS.put(BillingConstants.PACKAGE_LIFETIME, 
            new PackageConfig("终身套餐", 50000000L, -1, 999900L));
    }
    
    @Transactional
    public BillingOrderDTO createPackageOrder(Long tenantId, Long userId, String packageType) {
        PackageConfig config = PACKAGE_CONFIGS.get(packageType);
        if (config == null) {
            throw new IllegalArgumentException("无效的套餐类型: " + packageType);
        }
        
        BillingOrder order = new BillingOrder();
        order.setOrderNo(generateOrderNo());
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setOrderType(BillingConstants.ORDER_TYPE_PACKAGE);
        order.setStatus(BillingConstants.ORDER_STATUS_PENDING);
        order.setAmount(config.getPrice());
        order.setActualAmount(config.getPrice());
        order.setPackageId(packageType);
        order.setDescription("购买" + config.getName());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        
        orderMapper.insert(order);
        
        BillingOrderDTO dto = new BillingOrderDTO();
        BeanUtils.copyProperties(order, dto);
        return dto;
    }
    
    @Transactional
    public boolean handlePackagePurchase(Long orderId) {
        BillingOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != BillingConstants.ORDER_STATUS_PAID) {
            return false;
        }
        
        String lockKey = "package:" + order.getUserId();
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 30);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                return false;
            }
            
            PackageConfig config = PACKAGE_CONFIGS.get(order.getPackageId());
            if (config == null) {
                return false;
            }
            
            PackagePurchase purchase = new PackagePurchase();
            purchase.setTenantId(order.getTenantId());
            purchase.setUserId(order.getUserId());
            purchase.setOrderId(orderId);
            purchase.setPackageType(order.getPackageId());
            purchase.setPackageName(config.getName());
            purchase.setTokenQuota(config.getTokenQuota());
            purchase.setUsedTokens(0L);
            purchase.setEffectiveDate(LocalDate.now());
            
            if (config.getDurationDays() > 0) {
                purchase.setExpireDate(LocalDate.now().plusDays(config.getDurationDays()));
            } else {
                purchase.setExpireDate(null);
            }
            
            purchase.setStatus(1);
            packagePurchaseMapper.insert(purchase);
            
            return true;
            
        } finally {
            lock.unlock();
        }
    }
    
    public List<PackagePurchaseDTO> getUserPackages(Long userId) {
        List<PackagePurchase> packages = packagePurchaseMapper.selectByUserId(userId);
        return packages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public List<PackagePurchaseDTO> getActivePackages(Long userId) {
        List<PackagePurchase> packages = packagePurchaseMapper.selectActivePackages(userId);
        return packages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Transactional
    public boolean consumePackageToken(Long packageId, Long tokenCount) {
        PackagePurchase purchase = packagePurchaseMapper.selectById(packageId);
        if (purchase == null || purchase.getStatus() != 1) {
            return false;
        }
        
        if (purchase.getExpireDate() != null && purchase.getExpireDate().isBefore(LocalDate.now())) {
            purchase.setStatus(2);
            packagePurchaseMapper.updateById(purchase);
            return false;
        }
        
        long remaining = purchase.getTokenQuota() - purchase.getUsedTokens();
        if (remaining < tokenCount) {
            return false;
        }
        
        purchase.setUsedTokens(purchase.getUsedTokens() + tokenCount);
        
        if (purchase.getUsedTokens() >= purchase.getTokenQuota()) {
            purchase.setStatus(3);
        }
        
        packagePurchaseMapper.updateById(purchase);
        return true;
    }
    
    public Map<String, PackageConfig> getPackageConfigs() {
        return new ConcurrentHashMap<>(PACKAGE_CONFIGS);
    }
    
    private String generateOrderNo() {
        return "BX" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
               java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    @SuppressWarnings("nullness")
    private PackagePurchaseDTO convertToDTO(PackagePurchase purchase) {
        PackagePurchaseDTO dto = new PackagePurchaseDTO();
        BeanUtils.copyProperties(purchase, dto);
        return dto;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PackageConfig {
        private String name;
        private Long tokenQuota;
        private int durationDays;
        private Long price;
    }
}