package com.beijixing.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.tenant.dto.*;
import com.beijixing.tenant.entity.Tenant;
import com.beijixing.tenant.enums.PackageType;
import com.beijixing.tenant.enums.TenantStatus;
import com.beijixing.tenant.exception.TenantException;
import com.beijixing.tenant.repository.mapper.TenantMapper;
import com.beijixing.tenant.service.TenantService;
import com.beijixing.tenant.vo.TenantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 租户服务实现类
 * 功能：TM-001 租户注册、TM-002 租户审核、TM-003 批量审核、TM-004 租户状态管理
 *
 * @author bx-tenant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TENANT_INFO_KEY = "tenant:info:";

    // ==================== TM-001 租户注册流程 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant createTenant(CreateTenantRequest request) {
        log.info("创建租户，请求参数：{}", request);

        // 1. 校验手机号唯一性（此处简化，实际应查询数据库）
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getContactPhone, request.getContactPhone())
               .eq(Tenant::getDeleted, 0);
        Long existsCount = tenantMapper.selectCount(wrapper);
        if (existsCount > 0) {
            throw new TenantException("该手机号已注册租户");
        }

        // 2. 构建租户实体
        Tenant tenant = new Tenant();
        tenant.setTenantCode(generateTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setIndustry(request.getIndustry());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setBusinessLicense(request.getBusinessLicense());
        tenant.setLicenseImage(request.getLicenseImage());
        // 注册时默认状态为待审核
        tenant.setStatus(TenantStatus.PENDING.getCode());
        // 默认风控等级为低
        tenant.setRiskLevel(1);
        // 设置套餐类型（如果选择了套餐）
        if (request.getPackageType() != null) {
            tenant.setPackageType(request.getPackageType());
        }
        // 邀请人
        tenant.setInviterId(request.getInviterId());
        // 初始积分余额为0
        tenant.setPointBalance(java.math.BigDecimal.ZERO);
        tenant.setTotalConsumption(java.math.BigDecimal.ZERO);
        tenant.setCreateBy(request.getUserId());
        tenant.setUpdateBy(request.getUserId());

        // 3. 插入数据库
        tenantMapper.insert(tenant);
        log.info("租户创建成功，租户ID：{}，编码：{}", tenant.getId(), tenant.getTenantCode());

        // 4. 缓存租户基本信息
        cacheTenantInfo(tenant);

        return tenant;
    }

    @Override
    public String generateTenantCode() {
        // 格式：TJ + 时间戳后8位 + 4位随机数
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "TJ" + timestamp.substring(timestamp.length() - 8) + random;
    }

    // ==================== TM-002 租户审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditTenant(Long tenantId, TenantAuditRequest request) {
        log.info("审核租户，租户ID：{}，审核结果：{}", tenantId, request.getApproved());

        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new TenantException("租户不存在");
        }

        // 状态校验：只有待审核状态才能审核
        if (!TenantStatus.PENDING.getCode().equals(tenant.getStatus())) {
            throw new TenantException("该租户不在待审核状态，无法审核");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            // 审核通过：状态变为正常
            tenant.setStatus(TenantStatus.NORMAL.getCode());
            // 如果指定了套餐，更新套餐信息
            if (request.getPackageType() != null) {
                tenant.setPackageType(request.getPackageType());
                // 设置套餐过期时间（根据套餐类型）
                tenant.setPackageExpireTime(calculateExpireTime(request.getPackageType()));
            }
        } else {
            // 审核不通过：状态变为禁用
            tenant.setStatus(TenantStatus.DISABLED.getCode());
        }

        tenant.setUpdateBy(request.getAuditorId());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantMapper.updateById(tenant);

        // 更新缓存
        cacheTenantInfo(tenant);

        log.info("租户审核完成，租户ID：{}，最终状态：{}", tenantId, tenant.getStatus());
    }

    // ==================== TM-003 批量审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAuditTenants(BatchAuditRequest request) {
        log.info("批量审核租户，数量：{}", request.getTenantIds().size());

        int successCount = 0;
        for (Long tenantId : request.getTenantIds()) {
            try {
                TenantAuditRequest auditRequest = new TenantAuditRequest();
                auditRequest.setApproved(request.getApproved());
                auditRequest.setReason(request.getReason());
                auditRequest.setAuditorId(request.getAuditorId());
                auditRequest.setPackageType(request.getPackageType());
                auditTenant(tenantId, auditRequest);
                successCount++;
            } catch (TenantException e) {
                log.warn("批量审核中跳过租户ID：{}，原因：{}", tenantId, e.getMessage());
            }
        }

        log.info("批量审核完成，成功数量：{}", successCount);
        return successCount;
    }

    // ==================== TM-004 租户状态管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTenantStatus(Long tenantId, TenantStatusChangeRequest request) {
        log.info("变更租户状态，租户ID：{}，目标状态：{}", tenantId, request.getTargetStatus());

        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new TenantException("租户不存在");
        }

        TenantStatus targetStatus = TenantStatus.fromCode(request.getTargetStatus());
        if (targetStatus == null) {
            throw new TenantException("无效的目标状态");
        }

        // 校验状态转换合法性
        validateStatusTransition(tenant.getStatus(), request.getTargetStatus());

        tenant.setStatus(request.getTargetStatus());
        tenant.setUpdateBy(request.getOperatorId());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantMapper.updateById(tenant);

        // 如果注销，清空敏感数据（可选）
        if (TenantStatus.CANCELLED.getCode().equals(request.getTargetStatus())) {
            log.info("租户已注销，租户ID：{}，将进入30天数据保留期", tenantId);
        }

        // 更新缓存
        cacheTenantInfo(tenant);

        log.info("租户状态变更完成，租户ID：{}，新状态：{}", tenantId, tenant.getStatus());
    }

    // ==================== 基础查询 ====================

    @Override
    public TenantVO getTenantById(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            return null;
        }
        return convertToVO(tenant);
    }

    @Override
    public TenantVO getTenantByUserId(Long userId) {
        // 此处通过用户ID查找租户，实际应调用bx-user服务或查询关联表
        // 简化处理：直接返回null，由调用方通过其他方式获取
        log.warn("getTenantByUserId 需要依赖 bx-user 服务，此处简化处理");
        return null;
    }

    @Override
    public List<TenantVO> listTenants(Integer page, Integer size, String keyword, Integer status) {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;

        Page<Tenant> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Tenant::getTenantName, keyword)
                    .or().like(Tenant::getContactName, keyword)
                    .or().like(Tenant::getContactPhone, keyword));
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(Tenant::getStatus, status);
        }

        wrapper.orderByDesc(Tenant::getCreateTime);
        wrapper.eq(Tenant::getDeleted, 0);

        IPage<Tenant> pageResult = tenantMapper.selectPage(pageParam, wrapper);

        return pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TenantVO> listPendingTenants() {
        List<Tenant> tenants = tenantMapper.selectPendingTenants();
        return tenants.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(Long tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new TenantException("租户不存在");
        }

        // 更新非空字段
        if (request.getTenantName() != null) {
            tenant.setTenantName(request.getTenantName());
        }
        if (request.getIndustry() != null) {
            tenant.setIndustry(request.getIndustry());
        }
        if (request.getContactName() != null) {
            tenant.setContactName(request.getContactName());
        }
        if (request.getContactPhone() != null) {
            tenant.setContactPhone(request.getContactPhone());
        }
        if (request.getContactEmail() != null) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.getBusinessLicense() != null) {
            tenant.setBusinessLicense(request.getBusinessLicense());
        }
        if (request.getLicenseImage() != null) {
            tenant.setLicenseImage(request.getLicenseImage());
        }
        if (request.getRiskLevel() != null) {
            tenant.setRiskLevel(request.getRiskLevel());
        }

        tenant.setUpdateBy(request.getUpdateBy());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantMapper.updateById(tenant);
        cacheTenantInfo(tenant);

        log.info("租户信息更新成功，租户ID：{}", tenantId);
    }

    @Override
    public Tenant getTenantByCode(String tenantCode) {
        return tenantMapper.selectByTenantCode(tenantCode);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将实体转换为VO
     */
    private TenantVO convertToVO(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        TenantVO vo = new TenantVO();
        vo.setId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setTenantName(tenant.getTenantName());
        vo.setIndustry(tenant.getIndustry());
        vo.setContactName(tenant.getContactName());
        // 手机号脱敏
        vo.setContactPhone(maskPhone(tenant.getContactPhone()));
        // 邮箱脱敏
        vo.setContactEmail(maskEmail(tenant.getContactEmail()));
        vo.setBusinessLicense(tenant.getBusinessLicense());
        vo.setLicenseImage(tenant.getLicenseImage());
        vo.setStatus(tenant.getStatus());
        vo.setStatusDesc(getStatusDescription(tenant.getStatus()));
        vo.setRiskLevel(tenant.getRiskLevel());
        vo.setRiskLevelDesc(getRiskLevelDescription(tenant.getRiskLevel()));
        vo.setPackageType(tenant.getPackageType());
        vo.setPackageTypeDesc(getPackageTypeDescription(tenant.getPackageType()));
        vo.setPackageExpireTime(tenant.getPackageExpireTime());
        vo.setPointBalance(tenant.getPointBalance());
        vo.setTotalConsumption(tenant.getTotalConsumption());
        vo.setCreateTime(tenant.getCreateTime());
        vo.setUpdateTime(tenant.getUpdateTime());
        return vo;
    }

    /**
     * 缓存租户基本信息
     */
    private void cacheTenantInfo(Tenant tenant) {
        try {
            String cacheKey = TENANT_INFO_KEY + tenant.getId();
            redisTemplate.opsForValue().set(cacheKey, tenant);
        } catch (Exception e) {
            log.warn("缓存租户信息失败，租户ID：{}，原因：{}", tenant.getId(), e.getMessage());
        }
    }

    /**
     * 计算套餐过期时间
     */
    private LocalDateTime calculateExpireTime(String packageType) {
        PackageType type = PackageType.fromCode(packageType);
        if (type == null) {
            return null;
        }
        return switch (type) {
            case BASIC, ADVANCED -> LocalDateTime.now().plusDays(30);
            case ANNUAL -> LocalDateTime.now().plusDays(365);
            case LIFETIME -> null; // 终身无过期时间
        };
    }

    /**
     * 校验状态转换合法性
     */
    private void validateStatusTransition(Integer currentStatus, Integer targetStatus) {
        // 注销状态不可逆
        if (TenantStatus.CANCELLED.getCode().equals(currentStatus)) {
            throw new TenantException("已注销的租户不可变更状态");
        }
        // 其他状态转换可自行扩展
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email;
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(Integer status) {
        TenantStatus ts = TenantStatus.fromCode(status);
        return ts != null ? ts.getDescription() : "未知";
    }

    /**
     * 获取风控等级描述
     */
    private String getRiskLevelDescription(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 1 -> "低风险";
            case 2 -> "中风险";
            case 3 -> "高风险";
            default -> "未知";
        };
    }

    /**
     * 获取套餐类型描述
     */
    private String getPackageTypeDescription(String packageType) {
        PackageType type = PackageType.fromCode(packageType);
        return type != null ? type.getDescription() : "未购买";
    }
}
