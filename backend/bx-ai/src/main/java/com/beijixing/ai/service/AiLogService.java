package com.beijixing.ai.service;

import com.beijixing.ai.entity.AiModelLogEntity;
import com.beijixing.ai.mapper.AiModelLogMapper;
import com.beijixing.ai.model.AiModelLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLogService {
    
    private final AiModelLogMapper aiModelLogMapper;
    
    @Async
    public void saveLog(AiModelLog logEntry) {
        try {
            AiModelLogEntity entity = new AiModelLogEntity();
            BeanUtils.copyProperties(logEntry, entity);
            
            if (entity.getCreateTime() == null) {
                entity.setCreateTime(LocalDateTime.now());
            }
            
            aiModelLogMapper.insert(entity);
            
            log.info("[AI调用日志] requestId={}, userId={}, provider={}, type={}, status={}, time={}ms, fallback={}",
                    logEntry.getRequestId(),
                    logEntry.getUserId(),
                    logEntry.getProvider(),
                    logEntry.getRequestType(),
                    logEntry.getStatus(),
                    logEntry.getResponseTime(),
                    logEntry.getIsFallback());
        } catch (Exception e) {
            log.error("保存AI调用日志失败", e);
        }
    }
}
