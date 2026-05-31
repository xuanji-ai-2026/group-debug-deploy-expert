package com.beijixing.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.social.entity.AccountGroup;
import com.beijixing.social.mapper.AccountGroupMapper;
import com.beijixing.social.vo.GroupRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 账号分组服务
 */
@Service
@RequiredArgsConstructor
public class AccountGroupService extends ServiceImpl<AccountGroupMapper, AccountGroup> {

    private final AccountService accountService;

    /** 获取用户的分组列表 */
    public List<AccountGroup> listUserGroups(Long userId, String platformCode) {
        LambdaQueryWrapper<AccountGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountGroup::getUserId, userId);
        if (platformCode != null) {
            wrapper.and(w -> w.eq(AccountGroup::getPlatformCode, platformCode)
                             .or().isNull(AccountGroup::getPlatformCode));
        }
        wrapper.orderByAsc(AccountGroup::getSortOrder);
        return list(wrapper);
    }

    /** 创建分组 */
    public AccountGroup createGroup(GroupRequestVO request, Long userId) {
        AccountGroup group = new AccountGroup();
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        group.setPlatformCode(request.getPlatformCode());
        group.setColor(request.getColor());
        group.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        group.setUserId(userId);
        group.setAccountCount(0);
        save(group);
        return group;
    }

    /** 更新分组 */
    public boolean updateGroup(GroupRequestVO request) {
        AccountGroup group = getById(request.getId());
        if (group == null) return false;
        if (request.getGroupName() != null) group.setGroupName(request.getGroupName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());
        if (request.getColor() != null) group.setColor(request.getColor());
        if (request.getSortOrder() != null) group.setSortOrder(request.getSortOrder());
        return updateById(group);
    }

    /** 删除分组 */
    public boolean deleteGroup(Long id) {
        return removeById(id);
    }

    /** 更新分组账号数量 */
    public void updateAccountCount(Long groupId) {
        if (groupId == null) return;
        AccountGroup group = getById(groupId);
        if (group != null) {
            LambdaQueryWrapper<com.beijixing.social.entity.SocialAccount> wrapper = 
                new LambdaQueryWrapper<>();
            wrapper.eq(com.beijixing.social.entity.SocialAccount::getGroupId, groupId);
            long count = accountService.count(wrapper);
            group.setAccountCount((int) count);
            updateById(group);
        }
    }
}
