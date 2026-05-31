package com.beijixing.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.social.entity.AccountGroup;
import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.mapper.AccountGroupMapper;
import com.beijixing.social.mapper.AccountMapper;
import com.beijixing.social.vo.AccountRequestVO;
import com.beijixing.social.vo.AccountVO;
import com.beijixing.social.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 账号服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService extends ServiceImpl<AccountMapper, SocialAccount> {

    private final AccountGroupMapper groupMapper;

    /** 分页查询账号列表 */
    public PageVO<AccountVO> pageAccounts(Long pageNum, Long pageSize, String platformCode, Long groupId, Integer status) {
        Page<SocialAccount> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SocialAccount> wrapper = new LambdaQueryWrapper<>();
        if (platformCode != null) wrapper.eq(SocialAccount::getPlatformCode, platformCode);
        if (groupId != null) wrapper.eq(SocialAccount::getGroupId, groupId);
        if (status != null) wrapper.eq(SocialAccount::getStatus, status);
        wrapper.orderByDesc(SocialAccount::getCreateTime);
        
        Page<SocialAccount> result = page(page, wrapper);
        return PageVO.of(result.getTotal(), pageNum, pageSize, result.getRecords().stream().map(this::toVO).toList());
    }

    /** 根据ID获取账号详情 */
    public AccountVO getAccountById(Long id) {
        SocialAccount account = getById(id);
        return account != null ? toVO(account) : null;
    }

    /** 根据平台账号ID获取(返回实体) */
    public SocialAccount getSocialAccountByAccountId(String platformCode, String accountId) {
        LambdaQueryWrapper<SocialAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialAccount::getPlatformCode, platformCode)
               .eq(SocialAccount::getAccountId, accountId);
        return getOne(wrapper);
    }

    /** 根据平台账号ID获取 */
    public AccountVO getByAccountId(String platformCode, String accountId) {
        SocialAccount account = getSocialAccountByAccountId(platformCode, accountId);
        return account != null ? toVO(account) : null;
    }

    /** 创建/更新账号 */
    @Transactional
    public SocialAccount saveAccount(AccountRequestVO request) {
        SocialAccount account = new SocialAccount();
        account.setPlatformCode(request.getPlatformCode());
        account.setNickname(request.getNickname());
        account.setAvatarUrl(request.getAvatarUrl());
        account.setGroupId(request.getGroupId());
        if (request.getId() != null) {
            account.setId(request.getId());
            updateById(account);
        } else {
            account.setStatus(1);
            account.setNurturingStatus(0);
            account.setCreateTime(LocalDateTime.now());
            save(account);
        }
        return account;
    }

    /** 更新账号状态 */
    public boolean updateStatus(Long id, Integer status, String errorMsg) {
        SocialAccount account = getById(id);
        if (account == null) return false;
        account.setStatus(status);
        account.setErrorMsg(errorMsg);
        if (status == 1) {
            account.setErrorMsg(null);
        }
        return updateById(account);
    }

    /** 解绑账号 */
    @Transactional
    public boolean unbindAccount(Long id) {
        SocialAccount account = getById(id);
        if (account == null) return false;
        // 清理Token
        account.setAccessToken(null);
        account.setRefreshToken(null);
        account.setTokenExpireTime(null);
        account.setRefreshExpireTime(null);
        account.setStatus(0); // 未激活
        return updateById(account);
    }

    /** 更新Token */
    public boolean updateToken(Long id, String accessToken, String refreshToken, LocalDateTime expireTime, LocalDateTime refreshExpireTime) {
        SocialAccount account = getById(id);
        if (account == null) return false;
        account.setAccessToken(accessToken);
        account.setRefreshToken(refreshToken);
        account.setTokenExpireTime(expireTime);
        account.setRefreshExpireTime(refreshExpireTime);
        return updateById(account);
    }

    /** 查询即将过期的账号 */
    public List<SocialAccount> selectExpiringAccounts(int days) {
        return baseMapper.selectExpiringAccounts(days);
    }

    /** 查询异常账号 */
    public List<SocialAccount> selectAbnormalAccounts() {
        return baseMapper.selectAbnormalAccounts();
    }

    /**
     * 根据用户ID获取社交账号列表
     */
    public List<SocialAccount> getAccountsByUserId(Long userId) {
        return baseMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SocialAccount>()
                .eq(SocialAccount::getUserId, userId)
        );
    }

    /** 更新最后活跃时间 */
    public void updateLastActiveTime(Long accountId) {
        baseMapper.updateLastActiveTime(accountId);
    }

    private AccountVO toVO(SocialAccount a) {
        AccountVO vo = new AccountVO();
        vo.setId(a.getId());
        vo.setAccountId(a.getAccountId());
        vo.setPlatformCode(a.getPlatformCode());
        vo.setNickname(a.getNickname());
        vo.setAvatarUrl(a.getAvatarUrl());
        vo.setProfileUrl(a.getProfileUrl());
        vo.setFansCount(a.getFansCount());
        vo.setFollowCount(a.getFollowCount());
        vo.setLikeCount(a.getLikeCount());
        vo.setVerified(a.getVerified());
        vo.setStatus(a.getStatus());
        vo.setStatusName(getStatusName(a.getStatus()));
        vo.setGroupId(a.getGroupId());
        vo.setLastActiveTime(a.getLastActiveTime());
        vo.setNurturingStatus(a.getNurturingStatus());
        vo.setTokenExpireTime(a.getTokenExpireTime());
        if (a.getTokenExpireTime() != null) {
            vo.setDaysUntilExpire((int) ChronoUnit.DAYS.between(LocalDateTime.now(), a.getTokenExpireTime()));
        }
        // 查询分组名称
        if (a.getGroupId() != null) {
            AccountGroup group = groupMapper.selectById(a.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getGroupName());
            }
        }
        return vo;
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case 0 -> "未激活";
            case 1 -> "正常";
            case 2 -> "异常";
            case 3 -> "封禁";
            default -> "未知";
        };
    }
}
