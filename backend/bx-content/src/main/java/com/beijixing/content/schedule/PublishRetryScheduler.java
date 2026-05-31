package com.beijixing.content.schedule;

import com.beijixing.content.service.ContentPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishRetryScheduler {

    private final ContentPublishService publishService;
    private final DataSource dataSource;
    private final AtomicBoolean tableExists = new AtomicBoolean(false);

    private boolean checkContentTableExists() {
        try (Connection conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            try (var rs = meta.getTables(conn.getCatalog(), null, "content_publish_record", new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.warn("检查content_publish_record表是否存在时异常: {}", e.getMessage());
            return false;
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void retryFailedPublishes() {
        if (!tableExists.get()) {
            if (!checkContentTableExists()) {
                return;
            }
            tableExists.set(true);
        }

        log.info("开始执行发布重试任务...");
        
        try {
            publishService.retryFailedPublishes();
        } catch (Exception e) {
            log.error("发布重试任务异常", e);
        }
        
        log.info("发布重试任务执行完成");
    }
}
