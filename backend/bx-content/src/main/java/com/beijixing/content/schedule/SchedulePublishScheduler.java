package com.beijixing.content.schedule;

import com.beijixing.content.enums.PublishStatus;
import com.beijixing.content.mapper.ContentMapper;
import com.beijixing.content.service.ContentPublishService;
import com.beijixing.content.entity.Content;
import com.beijixing.content.task.SchedulePublishTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.RedissonShutdownException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulePublishScheduler {

    private final RedissonClient redissonClient;
    private final ContentMapper contentMapper;
    private final ContentPublishService publishService;
    private final DataSource dataSource;

    private RQueue<SchedulePublishTask> queue;
    
    private volatile boolean running = true;
    private static final int MAX_CONSECUTIVE_ERRORS = 100;
    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private final AtomicBoolean tableExists = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        if (!checkContentTableExists()) {
            log.warn("content表不存在，定时发布调度器跳过初始化");
            return;
        }
        tableExists.set(true);
        queue = redissonClient.getQueue("content:schedule:queue");
        
        startConsumer();
        
        log.info("定时发布调度器初始化完成");
    }

    @PreDestroy
    public void destroy() {
        running = false;
        log.info("定时发布调度器已停止");
    }

    private boolean checkContentTableExists() {
        try (Connection conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            try (var rs = meta.getTables(conn.getCatalog(), null, "content", new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.warn("检查content表是否存在时异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 启动延迟队列消费
     */
    private void startConsumer() {
        new Thread(() -> {
            log.info("定时发布消费线程启动: schedule-consumer");
            
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    SchedulePublishTask task = queue.poll();
                    if (task != null) {
                        consecutiveErrors.set(0);
                        log.info("执行定时发布任务: contentId={}", task.getContentId());
                        executePublish(task);
                    }
                    
                    Thread.sleep(1000);
                    
                } catch (RedissonShutdownException e) {
                    log.error("Redisson已关闭，停止消费线程");
                    running = false;
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("消费线程被中断，停止运行");
                    break;
                } catch (Exception e) {
                    int errors = consecutiveErrors.incrementAndGet();
                    
                    if (errors <= 5 || errors % 10 == 0) {
                        log.error("定时发布任务处理异常 (连续{}次)", errors, e);
                    }
                    
                    if (errors >= MAX_CONSECUTIVE_ERRORS) {
                        log.error("连续错误超过{}次，停止消费线程防止日志爆炸", MAX_CONSECUTIVE_ERRORS);
                        running = false;
                        break;
                    }
                    
                    try {
                        Thread.sleep(Math.min(errors * 100, 10000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            log.info("定时发布消费线程已停止");
        }, "schedule-consumer").start();
    }

    /**
     * 执行发布
     */
    private void executePublish(SchedulePublishTask task) {
        try {
            Content content = contentMapper.selectById(task.getContentId());
            if (content == null) {
                log.warn("定时发布失败，内容不存在: contentId={}", task.getContentId());
                return;
            }

            // 更新内容状态
            content.setStatus(2); // 已发布
            content.setPublishStatus(PublishStatus.PUBLISHING.getCode());
            content.setPublishTime(LocalDateTime.now());
            content.setScheduledTime(null);
            contentMapper.updateById(content);

            // 发布到各平台
            if (task.getPlatforms() != null && !task.getPlatforms().isEmpty()) {
                publishService.publishToPlatforms(task.getContentId(), task.getPlatforms());
            }

            log.info("定时发布成功: contentId={}", task.getContentId());

        } catch (Exception e) {
            log.error("定时发布失败: contentId={}", task.getContentId(), e);
        }
    }

    /**
     * 每分钟检查一次待发布内容
     * 兜底机制，防止延迟队列消息丢失
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkScheduledContents() {
        if (!tableExists.get()) {
            return;
        }
        
        log.debug("检查待发布内容...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Content> contents = contentMapper.selectContentsToPublish(now);
            
            for (Content content : contents) {
                log.info("发现待发布内容: contentId={}, scheduledTime={}", 
                        content.getId(), content.getScheduledTime());
                
                // 发布到官网
                publishService.publishToPlatforms(content.getId(), List.of(6));
            }
            
        } catch (Exception e) {
            log.error("检查待发布内容异常", e);
        }
    }
}
