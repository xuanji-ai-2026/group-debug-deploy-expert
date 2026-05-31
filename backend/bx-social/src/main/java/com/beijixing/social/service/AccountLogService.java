package com.beijixing.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.social.entity.AccountLog;
import com.beijixing.social.mapper.AccountLogMapper;
import com.beijixing.social.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号日志服务
 */
@Service
@RequiredArgsConstructor
public class AccountLogService extends ServiceImpl<AccountLogMapper, AccountLog> {

    /** 分页查询日志 */
    public PageVO<AccountLog> pageLogs(Long pageNum, Long pageSize, Long accountId, String actionType, 
                                       String platformCode, LocalDateTime startTime, LocalDateTime endTime) {
        Page<AccountLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AccountLog> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) wrapper.eq(AccountLog::getAccountId, accountId);
        if (actionType != null) wrapper.eq(AccountLog::getActionType, actionType);
        if (platformCode != null) wrapper.eq(AccountLog::getPlatformCode, platformCode);
        if (startTime != null) wrapper.ge(AccountLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(AccountLog::getCreateTime, endTime);
        wrapper.orderByDesc(AccountLog::getCreateTime);
        
        Page<AccountLog> result = page(page, wrapper);
        return PageVO.of(result.getTotal(), pageNum, pageSize, result.getRecords());
    }

    /** 记录操作日志 */
    @Transactional
    public void saveLog(Long accountId, String platformCode, String actionType, String actionDesc,
                        String targetId, String targetDesc, String contentId, String contentType,
                        String result, String failReason, String requestParams, String responseData,
                        Long duration, String ipAddress, String deviceId, Long userId) {
        AccountLog log = new AccountLog();
        log.setAccountId(accountId);
        log.setPlatformCode(platformCode);
        log.setActionType(actionType);
        log.setActionDesc(actionDesc);
        log.setTargetId(targetId);
        log.setTargetDesc(targetDesc);
        log.setContentId(contentId);
        log.setContentType(contentType);
        log.setResult(result);
        log.setFailReason(failReason);
        log.setRequestParams(requestParams);
        log.setResponseData(responseData);
        log.setDuration(duration);
        log.setIpAddress(ipAddress);
        log.setDeviceId(deviceId);
        log.setUserId(userId);
        log.setCreateTime(LocalDateTime.now());
        save(log);
    }

    /** 记录成功操作 */
    public void logSuccess(Long accountId, String platformCode, String actionType, String actionDesc) {
        saveLog(accountId, platformCode, actionType, actionDesc, null, null, null, null,
                "SUCCESS", null, null, null, null, null, null, null);
    }

    /** 记录失败操作 */
    public void logFail(Long accountId, String platformCode, String actionType, String actionDesc, String failReason) {
        saveLog(accountId, platformCode, actionType, actionDesc, null, null, null, null,
                "FAIL", failReason, null, null, null, null, null, null);
    }

    /** 获取账号操作统计 */
    public List<AccountLog> getAccountStats(Long accountId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AccountLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountLog::getAccountId, accountId);
        if (startTime != null) wrapper.ge(AccountLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(AccountLog::getCreateTime, endTime);
        wrapper.orderByDesc(AccountLog::getCreateTime);
        wrapper.last("LIMIT 100");
        return list(wrapper);
    }
}
