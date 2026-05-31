package com.beijixing.bxlead.job;

import com.beijixing.bxlead.entity.InterceptSource;
import com.beijixing.bxlead.mapper.InterceptSourceMapper;
import com.beijixing.bxlead.service.LeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 同业截客定时任务
 * @author 朱怡
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterceptJob {
    
    private final InterceptSourceMapper interceptSourceMapper;
    private final LeadService leadService;
    
    /**
     * 每分钟扫描未处理的截客来源并生成商机
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processInterceptSources() {
        log.info("开始处理截客来源...");
        
        List<InterceptSource> unprocessedList = interceptSourceMapper.selectUnprocessed(10);
        
        if (unprocessedList.isEmpty()) {
            log.info("没有未处理的截客来源");
            return;
        }
        
        int successCount = 0;
        for (InterceptSource source : unprocessedList) {
            try {
                Long leadId = leadService.generateLeadFromIntercept(source.getId());
                log.info("截客来源[{}]生成商机[{}]成功", source.getId(), leadId);
                successCount++;
            } catch (Exception e) {
                log.error("处理截客来源[{}]失败: {}", source.getId(), e.getMessage());
            }
        }
        
        log.info("截客来源处理完成，成功: {}/总: {}", successCount, unprocessedList.size());
    }
}