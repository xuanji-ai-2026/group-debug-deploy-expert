package com.beijixing.schedule.executor;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 数据同步任务执行器
 * 负责同步各平台数据到数据仓库
 */
@Slf4j
@Component
public class DataSyncExecutor extends BaseExecutor {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String SYNC_TIME_PREFIX = "data_sync:last_time:";
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int BATCH_SIZE = 500;
    private static final int MAX_RETRY = 3;

    @XxlJob("dataSyncJob")
    public void xxlExecute() {
        String params = XxlJobHelper.getJobParam();
        execute(params);
    }

    @Override
    protected String doExecute(String params) {
        log.info("开始执行数据同步任务");
        XxlJobHelper.log("开始执行数据同步任务");

        SyncStatistics totalStats = new SyncStatistics();

        // 1. 同步商机数据
        SyncStatistics leadStats = syncLeads();
        totalStats.merge(leadStats);

        // 2. 同步内容数据
        SyncStatistics contentStats = syncContents();
        totalStats.merge(contentStats);

        // 3. 同步用户行为数据
        SyncStatistics behaviorStats = syncBehaviors();
        totalStats.merge(behaviorStats);

        // 4. 同步统计数据
        SyncStatistics statsStats = syncStats();
        totalStats.merge(statsStats);

        // 5. 清理过期数据
        int cleaned = cleanExpiredData();

        log.info("数据同步完成: 成功={}, 失败={}, 跳过={}, 清理过期={}",
                totalStats.successCount, totalStats.failCount, totalStats.skipCount, cleaned);
        XxlJobHelper.log("数据同步完成: 成功={}, 失败={}, 跳过={}, 清理过期={}",
                totalStats.successCount, totalStats.failCount, totalStats.skipCount, cleaned);

        Map<String, Object> result = new HashMap<>();
        result.put("success", totalStats.successCount);
        result.put("failed", totalStats.failCount);
        result.put("skipped", totalStats.skipCount);
        result.put("cleaned", cleaned);
        result.put("details", totalStats.getDetailMap());
        return toJson(result);
    }

    @Override
    protected String getJobName() {
        return "数据同步任务";
    }

    @Override
    protected String getJobType() {
        return "data_sync";
    }

    @Override
    protected String getLockKey(String params) {
        return "hourly";
    }

    @Override
    protected int getTimeoutSeconds() {
        return 180;
    }

    /**
     * 同步商机数据 - 从bx_lead表同步到数据仓库(dw_leads)
     * 支持基于update_time的增量同步，包含数据清洗（电话/邮箱脱敏）
     */
    private SyncStatistics syncLeads() {
        log.info("同步商机数据...");
        XxlJobHelper.log("开始同步商机数据");
        SyncStatistics stats = new SyncStatistics("leads");

        try {
            LocalDateTime lastSyncTime = getLastSyncTime("leads");
            String timeCondition = lastSyncTime != null
                    ? " WHERE update_time > '" + lastSyncTime.format(DT_FORMAT) + "'"
                    : " WHERE 1=1";

            String countSql = "SELECT COUNT(*) FROM bx_lead" + timeCondition + " AND (deleted IS NULL OR deleted = 0)";
            Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            totalCount = totalCount != null ? totalCount : 0;

            if (totalCount == 0) {
                stats.skipCount = totalCount;
                log.info("商机数据无需同步，无新增或更新记录");
                return stats;
            }

            String selectSql = "SELECT id, lead_no, title, source, channel, customer_name, " +
                    "customer_phone, customer_email, customer_company, industry, region, " +
                    "requirement_desc, budget_amount, expected_deal_time, status, intent_score, " +
                    "level, owner_id, owner_name, assign_type, assign_time, competitor_keywords, " +
                    "intercept_source_id, ai_analysis_result, follow_count, last_follow_time, " +
                    "remark, create_time, update_time, create_by, update_by " +
                    "FROM bx_lead" + timeCondition + " AND (deleted IS NULL OR deleted = 0) " +
                    "ORDER BY update_time ASC LIMIT " + BATCH_SIZE;

            int offset = 0;
            while (true) {
                String pagedSql = selectSql + " OFFSET " + offset;
                List<Map<String, Object>> rows = executeWithRetry(pagedSql);
                if (rows == null || rows.isEmpty()) break;

                for (Map<String, Object> row : rows) {
                    try {
                        Map<String, Object> cleanedRow = cleanLeadData(row);
                        upsertLeadToWarehouse(cleanedRow);
                        stats.successCount++;
                    } catch (Exception e) {
                        stats.failCount++;
                        stats.addFailDetail(row.get("id"), e.getMessage());
                        log.warn("商机数据同步失败 id={}: {}", row.get("id"), e.getMessage());
                    }
                }
                offset += BATCH_SIZE;
                if (rows.size() < BATCH_SIZE) break;
            }

            updateLastSyncTime("leads");
            XxlJobHelper.log("商机数据同步完成: 成功={}, 失败={}", stats.successCount, stats.failCount);
        } catch (Exception e) {
            log.error("商机数据同步异常", e);
            stats.failCount++;
            stats.error = e.getMessage();
        }
        return stats;
    }

    /**
     * 同步内容数据 - 从bx_content表同步到dw_contents
     * 支持增量同步，包含内容状态过滤和数据标准化
     */
    private SyncStatistics syncContents() {
        log.info("同步内容数据...");
        XxlJobHelper.log("开始同步内容数据");
        SyncStatistics stats = new SyncStatistics("contents");

        try {
            LocalDateTime lastSyncTime = getLastSyncTime("contents");
            String timeCondition = lastSyncTime != null
                    ? " WHERE update_time > '" + lastSyncTime.format(DT_FORMAT) + "'"
                    : " WHERE 1=1";

            String countSql = "SELECT COUNT(*) FROM bx_content" + timeCondition + " AND (deleted IS NULL OR deleted = 0)";
            Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            totalCount = totalCount != null ? totalCount : 0;

            if (totalCount == 0) {
                stats.skipCount = totalCount;
                log.info("内容数据无需同步");
                return stats;
            }

            String selectSql = "SELECT id, title, content_type, category_id, status, " +
                    "publish_status, source_platform, author_id, view_count, like_count, " +
                    "comment_count, share_count, collect_count, create_time, update_time " +
                    "FROM bx_content" + timeCondition + " AND (deleted IS NULL OR deleted = 0) " +
                    "ORDER BY update_time ASC LIMIT " + BATCH_SIZE;

            int offset = 0;
            while (true) {
                List<Map<String, Object>> rows = executeWithRetry(selectSql + " OFFSET " + offset);
                if (rows == null || rows.isEmpty()) break;

                for (Map<String, Object> row : rows) {
                    try {
                        upsertContentToWarehouse(row);
                        stats.successCount++;
                    } catch (Exception e) {
                        stats.failCount++;
                        stats.addFailDetail(row.get("id"), e.getMessage());
                        log.warn("内容数据同步失败 id={}: {}", row.get("id"), e.getMessage());
                    }
                }
                offset += BATCH_SIZE;
                if (rows.size() < BATCH_SIZE) break;
            }

            updateLastSyncTime("contents");
            XxlJobHelper.log("内容数据同步完成: 成功={}, 失败={}", stats.successCount, stats.failCount);
        } catch (Exception e) {
            log.error("内容数据同步异常", e);
            stats.failCount++;
            stats.error = e.getMessage();
        }
        return stats;
    }

    /**
     * 同步用户行为数据 - 从bx_user_behavior表同步到dw_behaviors
     * 基于event_time进行增量同步，支持大批量日志数据的高效批处理
     */
    private SyncStatistics syncBehaviors() {
        log.info("同步用户行为数据...");
        XxlJobHelper.log("开始同步用户行为数据");
        SyncStatistics stats = new SyncStatistics("behaviors");

        try {
            LocalDateTime lastSyncTime = getLastSyncTime("behaviors");
            String timeCondition = lastSyncTime != null
                    ? " WHERE event_time > '" + lastSyncTime.format(DT_FORMAT) + "'"
                    : " WHERE event_time > DATE_SUB(NOW(), INTERVAL 7 DAY)";

            String countSql = "SELECT COUNT(*) FROM bx_user_behavior" + timeCondition;
            Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            totalCount = totalCount != null ? totalCount : 0;

            if (totalCount == 0) {
                stats.skipCount = totalCount;
                log.info("行为数据无需同步");
                return stats;
            }

            String selectSql = "SELECT id, user_id, event_type, target_type, target_id, " +
                    "event_time, ip_address, user_agent, device_type, referrer, " +
                    "extra_params, tenant_id, create_time " +
                    "FROM bx_user_behavior" + timeCondition + " " +
                    "ORDER BY event_time ASC LIMIT " + BATCH_SIZE;

            int offset = 0;
            while (true) {
                List<Map<String, Object>> rows = executeWithRetry(selectSql + " OFFSET " + offset);
                if (rows == null || rows.isEmpty()) break;

                List<Map<String, Object>> batch = new ArrayList<>();
                for (Map<String, Object> row : rows) {
                    try {
                        batch.add(cleanBehaviorData(row));
                        stats.successCount++;
                    } catch (Exception e) {
                        stats.failCount++;
                        stats.addFailDetail(row.get("id"), e.getMessage());
                    }
                }
                if (!batch.isEmpty()) {
                    batchInsertBehaviors(batch);
                }
                offset += BATCH_SIZE;
                if (rows.size() < BATCH_SIZE) break;
            }

            updateLastSyncTime("behaviors");
            XxlJobHelper.log("行为数据同步完成: 成功={}, 失败={}", stats.successCount, stats.failCount);
        } catch (Exception e) {
            log.error("行为数据同步异常", e);
            stats.failCount++;
            stats.error = e.getMessage();
        }
        return stats;
    }

    /**
     * 同步聚合统计数据 - 聚合计算并写入统计表(bx_lead_stats/bx_content_stats/bx_behavior_stats)
     * 按天聚合，支持全量和增量两种模式，默认聚合今天和昨天
     */
    private SyncStatistics syncStats() {
        log.info("同步统计数据...");
        XxlJobHelper.log("开始同步统计数据");
        SyncStatistics stats = new SyncStatistics("stats");

        try {
            String today = LocalDateTime.now().format(DATE_FORMAT);
            String yesterday = LocalDateTime.now().minusDays(1).format(DATE_FORMAT);

            stats.successCount += aggregateLeadStats(today);
            stats.successCount += aggregateLeadStats(yesterday);
            stats.successCount += aggregateContentStats(today);
            stats.successCount += aggregateContentStats(yesterday);
            stats.successCount += aggregateBehaviorStats(today);
            stats.successCount += aggregateBehaviorStats(yesterday);

            updateLastSyncTime("stats");
            XxlJobHelper.log("统计数据同步完成: 成功={}, 失败={}", stats.successCount, stats.failCount);
        } catch (Exception e) {
            log.error("统计数据同步异常", e);
            stats.failCount++;
            stats.error = e.getMessage();
        }
        return stats;
    }

    /**
     * 通用表同步方法 - 基于时间戳的增量同步框架
     * 提供通用的读取-转换-写入管道，适用于任意业务表到数仓表的映射
     */
    private int syncTable(String tableName, String timeField) {
        log.info("同步表: {}", tableName);
        SyncStatistics stats = new SyncStatistics(tableName);

        try {
            LocalDateTime lastSyncTime = getLastSyncTime(tableName);
            String timeCondition = lastSyncTime != null
                    ? " WHERE " + timeField + " > '" + lastSyncTime.format(DT_FORMAT) + "'"
                    : "";

            String selectSql = "SELECT * FROM " + tableName + timeCondition +
                    " ORDER BY " + timeField + " ASC LIMIT " + BATCH_SIZE;

            int offset = 0;
            int totalSynced = 0;
            while (true) {
                List<Map<String, Object>> rows = executeWithRetry(selectSql + " OFFSET " + offset);
                if (rows == null || rows.isEmpty()) break;

                for (Map<String, Object> row : rows) {
                    try {
                        upsertToWarehouse(tableName, row);
                        totalSynced++;
                        stats.successCount++;
                    } catch (Exception e) {
                        stats.failCount++;
                    }
                }
                offset += BATCH_SIZE;
                if (rows.size() < BATCH_SIZE) break;
            }

            updateLastSyncTime(tableName);
            return totalSynced;
        } catch (Exception e) {
            log.error("同步表 {} 异常: {}", tableName, e.getMessage());
            return 0;
        }
    }

    /**
     * 清理过期数据 - 根据保留策略清理各表的过期数据
     * 行为日志保留90天、操作日志保留180天、临时数据7天、同步日志30天
     */
    private int cleanExpiredData() {
        log.info("清理过期数据...");
        XxlJobHelper.log("开始清理过期数据");
        int totalCleaned = 0;

        try {
            totalCleaned += cleanTable("bx_user_behavior", "event_time", 90);
            totalCleaned += cleanTable("bx_operation_log", "create_time", 180);
            totalCleaned += cleanTable("bx_temp_data", "create_time", 7);
            totalCleaned += cleanTable("bx_data_sync_log", "create_time", 30);

            log.info("过期数据清理完成，共清理 {} 条", totalCleaned);
            XxlJobHelper.log("过期数据清理完成，共清理 {} 条", totalCleaned);
        } catch (Exception e) {
            log.error("清理过期数据异常", e);
        }
        return totalCleaned;
    }

    /**
     * 清理指定表中超过保留天数的数据
     * 使用分批删除策略（每次最多10000条），避免长事务锁表
     *
     * @param tableName      表名
     * @param timeField      时间字段名
     * @param retentionDays  保留天数
     * @return 删除的记录总数
     */
    private int cleanTable(String tableName, String timeField, int retentionDays) {
        try {
            String deleteSql = "DELETE FROM " + tableName +
                    " WHERE " + timeField + " < DATE_SUB(NOW(), INTERVAL " + retentionDays + " DAY)" +
                    " LIMIT 10000";
            int affected = 0;
            int totalDeleted = 0;
            do {
                affected = jdbcTemplate.update(deleteSql);
                totalDeleted += affected;
            } while (affected >= 10000);

            log.info("清理表 {} 中 {} 天前的数据，共删除 {} 条", tableName, retentionDays, totalDeleted);
            return totalDeleted;
        } catch (Exception e) {
            log.warn("清理表 {} 失败: {}", tableName, e.getMessage());
            return 0;
        }
    }

    // ==================== 统计聚合方法 ====================

    /**
     * 聚合商机统计数据 - 按状态分组统计商机数量
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 实现幂等写入
     */
    private int aggregateLeadStats(String date) {
        try {
            String sql = "INSERT INTO bx_lead_stats (tenant_id, stat_date, new_leads, following_leads, " +
                    "converted_leads, valid_leads, invalid_leads, create_time, update_time) " +
                    "SELECT COALESCE(tenant_id, 0), ?, " +
                    "SUM(CASE WHEN status = 'NEW' THEN 1 ELSE 0 END), " +
                    "SUM(CASE WHEN status = 'FOLLOWING' THEN 1 ELSE 0 END), " +
                    "SUM(CASE WHEN status = 'CONVERTED' THEN 1 ELSE 0 END), " +
                    "SUM(CASE WHEN intent_score >= 60 THEN 1 ELSE 0 END), " +
                    "SUM(CASE WHEN intent_score < 60 OR intent_score IS NULL THEN 1 ELSE 0 END), " +
                    "NOW(), NOW() " +
                    "FROM bx_lead " +
                    "WHERE DATE(create_time) = ? AND (deleted IS NULL OR deleted = 0) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "new_leads = VALUES(new_leads), following_leads = VALUES(following_leads), " +
                    "converted_leads = VALUES(converted_leads), valid_leads = VALUES(valid_leads), " +
                    "invalid_leads = VALUES(invalid_leads), update_time = NOW()";
            return jdbcTemplate.update(sql, date, date);
        } catch (Exception e) {
            log.warn("聚合商机统计 {} 失败: {}", date, e.getMessage());
            return 0;
        }
    }

    /**
     * 聚合内容统计数据 - 统计发布数和互动量
     */
    private int aggregateContentStats(String date) {
        try {
            String sql = "INSERT INTO bx_content_stats (stat_date, publish_count, view_total, " +
                    "like_total, comment_total, create_time, update_time) " +
                    "SELECT ?, COUNT(*), COALESCE(SUM(view_count), 0), " +
                    "COALESCE(SUM(like_count), 0), COALESCE(SUM(comment_count), 0), NOW(), NOW() " +
                    "FROM bx_content " +
                    "WHERE DATE(create_time) = ? AND (deleted IS NULL OR deleted = 0) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "publish_count = VALUES(publish_count), view_total = VALUES(view_total), " +
                    "like_total = VALUES(like_total), comment_total = VALUES(comment_total), " +
                    "update_time = NOW()";
            return jdbcTemplate.update(sql, date, date);
        } catch (Exception e) {
            log.warn("聚合内容统计 {} 失败: {}", date, e.getMessage());
            return 0;
        }
    }

    /**
     * 聚合行为统计数据 - 按事件类型分组统计PV/UV
     */
    private int aggregateBehaviorStats(String date) {
        try {
            String sql = "INSERT INTO bx_behavior_stats (stat_date, event_type, event_count, " +
                    "unique_users, create_time, update_time) " +
                    "SELECT ?, event_type, COUNT(*), COUNT(DISTINCT user_id), NOW(), NOW() " +
                    "FROM bx_user_behavior " +
                    "WHERE DATE(event_time) = ? " +
                    "GROUP BY event_type " +
                    "ON DUPLICATE KEY UPDATE " +
                    "event_count = VALUES(event_count), unique_users = VALUES(unique_users), " +
                    "update_time = NOW()";
            return jdbcTemplate.update(sql, date, date);
        } catch (Exception e) {
            log.warn("聚合行为统计 {} 失败: {}", date, e.getMessage());
            return 0;
        }
    }

    // ==================== 数据清洗方法 ====================

    /**
     * 商机数据清洗 - 对敏感信息进行脱敏处理
     * 电话号码: 138****1234
     * 邮箱: abc***@example.com
     */
    private Map<String, Object> cleanLeadData(Map<String, Object> row) {
        Map<String, Object> cleaned = new HashMap<>(row);
        Object phone = cleaned.get("customer_phone");
        if (phone instanceof String && phone.toString().length() > 6) {
            String p = phone.toString();
            cleaned.put("customer_phone", p.substring(0, 3) + "****" + p.substring(p.length() - 4));
        }
        Object email = cleaned.get("customer_email");
        if (email instanceof String && email.toString().contains("@")) {
            String e = email.toString();
            int atIdx = e.indexOf("@");
            cleaned.put("customer_email", e.substring(0, Math.min(3, atIdx)) + "***" + e.substring(atIdx));
        }
        return cleaned;
    }

    /**
     * 行为数据清洗 - 移除PII信息，IP地址做哈希处理
     */
    private Map<String, Object> cleanBehaviorData(Map<String, Object> row) {
        Map<String, Object> cleaned = new HashMap<>(row);
        Object ip = cleaned.remove("ip_address");
        if (ip != null) {
            String ipStr = ip.toString();
            cleaned.put("ip_hash", Integer.toHexString(ipStr.hashCode()));
        }
        cleaned.remove("user_agent");
        return cleaned;
    }

    // ==================== 数仓写入方法 ====================

    /**
     * 写入/更新商机数据到数仓表 dw_leads
     * 使用主键 upsert 策略保证幂等性
     *
     * [需确认] dw_leads 表结构需与 SELECT 字段匹配，如不存在需先建表:
     * CREATE TABLE dw_leads LIKE bx_lead; ALTER TABLE dw_leads ADD COLUMN sync_time DATETIME;
     */
    private void upsertLeadToWarehouse(Map<String, Object> data) {
        String sql = "INSERT INTO dw_leads (id, lead_no, title, source, channel, customer_name, " +
                "customer_phone, customer_email, customer_company, industry, region, status, " +
                "intent_score, level, owner_id, owner_name, create_time, update_time, sync_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE " +
                "title=VALUES(title), source=VALUES(source), channel=VALUES(channel), " +
                "customer_name=VALUES(customer_name), customer_phone=VALUES(customer_phone), " +
                "customer_email=VALUES(customer_email), industry=VALUES(industry), " +
                "status=VALUES(status), intent_score=VALUES(intent_score), level=VALUES(level), " +
                "owner_id=VALUES(owner_id), owner_name=VALUES(owner_name), " +
                "update_time=VALUES(update_time), sync_time=NOW()";

        jdbcTemplate.update(sql,
                data.get("id"), data.get("lead_no"), data.get("title"), data.get("source"),
                data.get("channel"), data.get("customer_name"), data.get("customer_phone"),
                data.get("customer_email"), data.get("customer_company"), data.get("industry"),
                data.get("region"), data.get("status"), data.get("intent_score"),
                data.get("level"), data.get("owner_id"), data.get("owner_name"),
                data.get("create_time"), data.get("update_time"));
    }

    /**
     * 写入/更新内容数据到数仓表 dw_contents
     *
     * [需确认] dw_contents 表结构需与字段匹配
     */
    private void upsertContentToWarehouse(Map<String, Object> data) {
        String sql = "INSERT INTO dw_contents (id, title, content_type, category_id, status, " +
                "publish_status, source_platform, view_count, like_count, comment_count, " +
                "share_count, collect_count, create_time, update_time, sync_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE " +
                "title=VALUES(title), status=VALUES(status), publish_status=VALUES(publish_status), " +
                "view_count=VALUES(view_count), like_count=VALUES(like_count), " +
                "comment_count=VALUES(comment_count), share_count=VALUES(share_count), " +
                "collect_count=VALUES(collect_count), update_time=VALUES(update_time), sync_time=NOW()";

        jdbcTemplate.update(sql,
                data.get("id"), data.get("title"), data.get("content_type"),
                data.get("category_id"), data.get("status"), data.get("publish_status"),
                data.get("source_platform"), data.get("view_count"), data.get("like_count"),
                data.get("comment_count"), data.get("share_count"), data.get("collect_count"),
                data.get("create_time"), data.get("update_time"));
    }

    /**
     * 批量写入行为数据到数仓表 dw_behaviors
     * 使用 JDBC batchUpdate 提升大批量写入性能
     *
     * [需确认] dw_behaviors 表需含 ip_hash 字段（替代原始 ip_address）
     */
    private void batchInsertBehaviors(List<Map<String, Object>> batch) {
        String sql = "INSERT IGNORE INTO dw_behaviors (id, user_id, event_type, target_type, " +
                "target_id, event_time, device_type, referrer, ip_hash, extra_params, " +
                "tenant_id, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, batch, BATCH_SIZE, (ps, row) -> {
            ps.setObject(1, row.get("id"));
            ps.setObject(2, row.get("user_id"));
            ps.setObject(3, row.get("event_type"));
            ps.setObject(4, row.get("target_type"));
            ps.setObject(5, row.get("target_id"));
            ps.setObject(6, row.get("event_time"));
            ps.setObject(7, row.get("device_type"));
            ps.setObject(8, row.get("referrer"));
            ps.setObject(9, row.get("ip_hash"));
            ps.setObject(10, row.get("extra_params"));
            ps.setObject(11, row.get("tenant_id"));
            ps.setObject(12, row.get("create_time"));
        });
    }

    /**
     * 通用数仓 upsert 方法 - 动态构建 INSERT ... ON DUPLICATE KEY UPDATE
     * 适用于 syncTable() 通用同步场景
     */
    private void upsertToWarehouse(String tableName, Map<String, Object> data) {
        String dwTableName = "dw_" + tableName;
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        StringBuilder updates = new StringBuilder();
        List<Object> values = new ArrayList<>();

        int idx = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (idx > 0) {
                columns.append(", ");
                placeholders.append(", ");
                updates.append(", ");
            }
            String col = entry.getKey();
            columns.append(col);
            placeholders.append("?");
            updates.append(col).append("=VALUES(").append(col).append(")");
            values.add(entry.getValue());
            idx++;
        }
        columns.append(", sync_time");
        placeholders.append(", NOW()");
        values.add(LocalDateTime.now());

        String sql = "INSERT INTO " + dwTableName + " (" + columns + ") VALUES (" + placeholders + ") " +
                "ON DUPLICATE KEY UPDATE " + updates + ", sync_time=NOW()";
        jdbcTemplate.update(sql, values.toArray());
    }

    // ==================== 增量同步辅助方法 ====================

    /**
     * 从 Redis 获取上次同步时间
     */
    private LocalDateTime getLastSyncTime(String tableKey) {
        if (redisTemplate == null) return null;
        try {
            String cached = redisTemplate.opsForValue().get(SYNC_TIME_PREFIX + tableKey);
            if (cached != null && !cached.isEmpty()) {
                return LocalDateTime.parse(cached, DT_FORMAT);
            }
        } catch (Exception e) {
            log.warn("获取{}最后同步时间失败: {}", tableKey, e.getMessage());
        }
        return null;
    }

    /**
     * 更新 Redis 中的最后同步时间（保留30天）
     */
    private void updateLastSyncTime(String tableKey) {
        if (redisTemplate == null) return;
        try {
            String now = LocalDateTime.now().format(DT_FORMAT);
            redisTemplate.opsForValue().set(SYNC_TIME_PREFIX + tableKey, now, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("更新{}最后同步时间失败: {}", tableKey, e.getMessage());
        }
    }

    /**
     * 带重试机制的 SQL 查询执行
     * 最多重试 MAX_RETRY 次，采用指数退避策略
     */
    private List<Map<String, Object>> executeWithRetry(String sql) {
        Exception lastException = null;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return jdbcTemplate.queryForList(sql);
            } catch (DataAccessException e) {
                lastException = e;
                if (i < MAX_RETRY - 1) {
                    try {
                        Thread.sleep(1000L * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Collections.emptyList();
                    }
                }
            }
        }
        throw new RuntimeException("SQL执行重试" + MAX_RETRY + "次后仍失败: " + lastException.getMessage(), lastException);
    }

    // ==================== 工具方法 ====================

    private String toJson(Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 同步统计信息内部类 - 记录各维度同步结果
     */
    private static class SyncStatistics {
        String name;
        int successCount;
        int failCount;
        int skipCount;
        String error;
        private final List<Map<String, Object>> failDetails = new ArrayList<>();

        SyncStatistics() {
        }

        SyncStatistics(String name) {
            this.name = name;
        }

        void merge(SyncStatistics other) {
            this.successCount += other.successCount;
            this.failCount += other.failCount;
            this.skipCount += other.skipCount;
        }

        void addFailDetail(Object id, String reason) {
            if (failDetails.size() < 10) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", id);
                detail.put("reason", reason);
                failDetails.add(detail);
            }
        }

        Map<String, Object> getDetailMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("success", successCount);
            map.put("failed", failCount);
            map.put("skipped", skipCount);
            if (!failDetails.isEmpty()) {
                map.put("failDetails", failDetails);
            }
            if (error != null) {
                map.put("error", error);
            }
            return map;
        }
    }
}