package com.beijixing.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beijixing.tenant.dto.PackageChangeRequest;
import com.beijixing.tenant.entity.Tenant;
import com.beijixing.tenant.entity.TenantPackage;
import com.beijixing.tenant.enums.PackageType;
import com.beijixing.tenant.exception.TenantException;
import com.beijixing.tenant.repository.mapper.TenantMapper;
import com.beijixing.tenant.repository.mapper.TenantPackageMapper;
import com.beijixing.tenant.service.PackageService;
import com.beijixing.tenant.vo.PackageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐服务实现类
 * 功能：TM-005 套餐管理
 *
 * @author bx-tenant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final TenantMapper tenantMapper;
    private final TenantPackageMapper tenantPackageMapper;

    // ==================== TM-005 套餐管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantPackage purchasePackage(PackageChangeRequest request) {
        log.info("购买/变更套餐，请求：{}", request);

        // 1. 校验租户是否存在且状态正常
        Tenant tenant = tenantMapper.selectById(request.getTenantId());
        if (tenant == null) {
            throw new TenantException("租户不存在");
        }
        if (!tenant.isActive()) {
            throw new TenantException("租户状态异常，无法购买套餐");
        }

        // 2. 如果有当前生效套餐，先将其标记为已过期
        markCurrentPackageExpired(request.getTenantId());

        // 3. 构建新的租户套餐记录
        String packageType = getPackageTypeById(request.getPackageId());
        TenantPackage newPackage = new TenantPackage();
        newPackage.setTenantId(request.getTenantId());
        newPackage.setPackageId(request.getPackageId());
        newPackage.setPackageName(getPackageNameById(request.getPackageId()));
        newPackage.setPackageType(packageType);
        newPackage.setPrice(BigDecimal.ZERO); // 价格由计费服务填充
        newPackage.setPointAmount(BigDecimal.ZERO); // 积分由计费服务填充
        newPackage.setPurchaseType(request.getPurchaseType() != null ? request.getPurchaseType() : 1);
        newPackage.setOrderId(request.getOrderId());
        newPackage.setAuditStatus(1); // 直接生效（在线购买已支付）
        newPackage.setEffectiveTime(LocalDateTime.now());
        newPackage.setExpireTime(calculateExpireTime(packageType));
        newPackage.setCreateBy(request.getOperatorId());
        newPackage.setUpdateBy(request.getOperatorId());

        // 4. 插入套餐记录
        tenantPackageMapper.insert(newPackage);

        // 5. 更新租户的套餐信息
        updateTenantPackageInfo(tenant, newPackage);

        log.info("套餐购买成功，租户ID：{}，套餐类型：{}", request.getTenantId(), packageType);
        return newPackage;
    }

    @Override
    public List<PackageVO> getTenantPackages(Long tenantId) {
        List<TenantPackage> packages = tenantPackageMapper.selectByTenantId(tenantId);
        TenantPackage currentPackage = packages.stream()
                .filter(TenantPackage::isEffective)
                .findFirst()
                .orElse(null);
        return packages.stream()
                .map(pkg -> convertToVO(pkg, currentPackage))
                .collect(Collectors.toList());
    }

    @Override
    public PackageVO getCurrentPackage(Long tenantId) {
        TenantPackage current = tenantPackageMapper.selectCurrentByTenantId(tenantId);
        if (current == null) {
            return null;
        }
        return convertToVO(current, current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPackage(Long tenantPackageId, Long operatorId) {
        TenantPackage pkg = tenantPackageMapper.selectById(tenantPackageId);
        if (pkg == null) {
            throw new TenantException("套餐记录不存在");
        }

        boolean wasEffective = pkg.isEffective();
        pkg.setAuditStatus(3); // 已退订
        pkg.setUpdateBy(operatorId);
        pkg.setUpdateTime(LocalDateTime.now());
        tenantPackageMapper.updateById(pkg);

        // 如果是当前生效套餐，清除租户的套餐信息
        if (wasEffective) {
            Tenant tenant = tenantMapper.selectById(pkg.getTenantId());
            if (tenant != null) {
                tenant.setPackageType(null);
                tenant.setPackageExpireTime(null);
                tenant.setUpdateBy(operatorId);
                tenant.setUpdateTime(LocalDateTime.now());
                tenantMapper.updateById(tenant);
            }
        }

        log.info("套餐退订成功，套餐记录ID：{}", tenantPackageId);
    }

    @Override
    public TenantPackage getPackageById(Long id) {
        return tenantPackageMapper.selectById(id);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将当前生效套餐标记为已过期
     */
    private void markCurrentPackageExpired(Long tenantId) {
        LambdaUpdateWrapper<TenantPackage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TenantPackage::getTenantId, tenantId)
               .eq(TenantPackage::getAuditStatus, 1)
               .set(TenantPackage::getAuditStatus, 2) // 标记为已过期
               .set(TenantPackage::getUpdateTime, LocalDateTime.now());
        tenantPackageMapper.update(null, wrapper);
        log.info("已将租户 {} 的原生效套餐标记为过期", tenantId);
    }

    /**
     * 根据套餐ID获取套餐名称（实际应调用 bx-billing 服务）
     */
    private String getPackageNameById(Long packageId) {
        log.debug("查询套餐名称(待接入bx-billing服务，当前使用本地缓存降级): packageId={}", packageId);
        if (packageId == null) {
            return "未知套餐";
        }
        return queryPackageNameFromCache(packageId);
    }

    /**
     * 根据套餐ID获取套餐类型（实际应调用 bx-billing 服务）
     */
    private String getPackageTypeById(Long packageId) {
        log.debug("查询套餐类型(待接入bx-billing服务，当前使用本地缓存降级): packageId={}", packageId);
        return queryPackageTypeFromCache(packageId);
    }

    /**
     * 从本地缓存查询套餐名称（降级方案）
     */
    private String queryPackageNameFromCache(Long packageId) {
        java.util.Map<Long, String> packageNames = new java.util.HashMap<>();
        packageNames.put(1L, "基础版");
        packageNames.put(2L, "专业版");
        packageNames.put(3L, "企业版");
        packageNames.put(4L, "旗舰版");
        packageNames.put(5L, "定制版");

        String name = packageNames.get(packageId);
        return name != null ? name : "套餐-" + packageId;
    }

    /**
     * 从本地缓存查询套餐类型（降级方案）
     */
    private String queryPackageTypeFromCache(Long packageId) {
        if (packageId == null) {
            return "basic";
        }

        switch (packageId.intValue()) {
            case 1: return "basic";
            case 2: return "advanced";
            case 3: return "annual";
            case 4: return "lifetime";
            case 5: return "enterprise";
            default: return "basic";
        }
    }

    /**
     * 计算套餐过期时间
     */
    private LocalDateTime calculateExpireTime(String packageType) {
        PackageType type = PackageType.fromCode(packageType);
        if (type == null) {
            return LocalDateTime.now().plusDays(30);
        }
        return switch (type) {
            case BASIC, ADVANCED -> LocalDateTime.now().plusDays(30);
            case ANNUAL -> LocalDateTime.now().plusDays(365);
            case LIFETIME -> null; // 终身不过期
        };
    }

    /**
     * 更新租户的套餐信息
     */
    private void updateTenantPackageInfo(Tenant tenant, TenantPackage pkg) {
        tenant.setPackageType(pkg.getPackageType());
        tenant.setPackageExpireTime(pkg.getExpireTime());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.updateById(tenant);
    }

    /**
     * 将实体转换为VO
     */
    private PackageVO convertToVO(TenantPackage pkg, TenantPackage currentPackage) {
        if (pkg == null) {
            return null;
        }
        PackageVO vo = new PackageVO();
        vo.setId(pkg.getId());
        vo.setTenantId(pkg.getTenantId());
        vo.setPackageId(pkg.getPackageId());
        vo.setPackageName(pkg.getPackageName());
        vo.setPackageType(pkg.getPackageType());
        vo.setPackageTypeDesc(getPackageTypeDescription(pkg.getPackageType()));
        vo.setPrice(pkg.getPrice());
        vo.setPointAmount(pkg.getPointAmount());
        vo.setEffectiveTime(pkg.getEffectiveTime());
        vo.setExpireTime(pkg.getExpireTime());
        vo.setPurchaseType(pkg.getPurchaseType());
        vo.setPurchaseTypeDesc(getPurchaseTypeDescription(pkg.getPurchaseType()));
        vo.setAuditStatus(pkg.getAuditStatus());
        vo.setAuditStatusDesc(getAuditStatusDescription(pkg.getAuditStatus()));
        vo.setAuditRemark(pkg.getAuditRemark());
        vo.setCreateTime(pkg.getCreateTime());
        vo.setIsCurrent(currentPackage != null && currentPackage.getId().equals(pkg.getId()));
        return vo;
    }

    private String getPackageTypeDescription(String packageType) {
        PackageType type = PackageType.fromCode(packageType);
        return type != null ? type.getDescription() : "未知";
    }

    private String getPurchaseTypeDescription(Integer purchaseType) {
        if (purchaseType == null) return "未知";
        return switch (purchaseType) {
            case 1 -> "在线购买";
            case 2 -> "线下购买";
            case 3 -> "活动赠送";
            default -> "未知";
        };
    }

    private String getAuditStatusDescription(Integer auditStatus) {
        if (auditStatus == null) return "未知";
        return switch (auditStatus) {
            case 0 -> "待审核";
            case 1 -> "已生效";
            case 2 -> "已过期";
            case 3 -> "已退订";
            default -> "未知";
        };
    }
}
