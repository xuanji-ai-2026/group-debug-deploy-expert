package com.beijixing.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.system.entity.SysOperLog;
import com.beijixing.system.mapper.SysOperLogMapper;
import com.beijixing.system.service.OperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现
 *
 * @author bx-system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper operLogMapper;

    @Override
    public void saveLog(SysOperLog operLog) {
        if (operLog.getOperTime() == null) {
            operLog.setOperTime(LocalDateTime.now());
        }
        operLogMapper.insert(operLog);
        log.debug("记录操作日志：module={}, type={}, url={}", operLog.getModuleName(), operLog.getOperType(), operLog.getRequestUrl());
    }

    @Async
    @Override
    public void saveLogAsync(String moduleName, String operType, String operDesc,
                              String requestMethod, String requestUrl, String requestParams,
                              Long userId, String userName, Long tenantId,
                              String operIp, String operLocation, String userAgent,
                              Long duration, Integer status, String errorMsg) {
        SysOperLog operLog = new SysOperLog();
        operLog.setModuleName(moduleName);
        operLog.setOperType(operType);
        operLog.setOperDesc(operDesc);
        operLog.setRequestMethod(requestMethod);
        operLog.setRequestUrl(requestUrl);
        operLog.setRequestParams(requestParams);
        operLog.setUserId(userId);
        operLog.setUserName(userName);
        operLog.setTenantId(tenantId);
        operLog.setOperIp(operIp);
        operLog.setOperLocation(operLocation);
        operLog.setUserAgent(userAgent);
        operLog.setDuration(duration);
        operLog.setStatus(status);
        operLog.setErrorMsg(errorMsg);
        operLog.setOperTime(LocalDateTime.now());

        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.error("异步保存操作日志失败", e);
        }
    }

    @Override
    public List<SysOperLog> getRecentLogs(Integer limit) {
        return operLogMapper.selectRecent(limit);
    }

    @Override
    public List<SysOperLog> getLogsByUserId(Long userId, Integer limit) {
        return operLogMapper.selectByUserId(userId, limit);
    }

    @Override
    public List<SysOperLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return operLogMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        List<SysOperLog> logs = operLogMapper.selectByTimeRange(startTime, endTime);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", logs.size());
        stats.put("successCount", logs.stream().filter(l -> l.getStatus() != null && l.getStatus() == 1).count());
        stats.put("failCount", logs.stream().filter(l -> l.getStatus() != null && l.getStatus() == 0).count());
        stats.put("avgDuration", logs.stream()
                .filter(l -> l.getDuration() != null)
                .mapToLong(SysOperLog::getDuration)
                .average().orElse(0));

        return stats;
    }

    @Override
    public int cleanOldLogs(Integer days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SysOperLog::getOperTime, beforeTime);
        int count = operLogMapper.delete(wrapper);
        log.info("清理 {} 天前的操作日志，删除 {} 条", days, count);
        return count;
    }

    @Override
    public SysOperLog getById(Long id) {
        return operLogMapper.selectById(id);
    }
}
