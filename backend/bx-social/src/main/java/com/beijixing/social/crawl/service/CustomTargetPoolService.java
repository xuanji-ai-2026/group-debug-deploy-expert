package com.beijixing.social.crawl.service;

import com.beijixing.social.crawl.model.CommentCrawlResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义目标池管理服务
 *
 * 核心功能:
 * 1. **目标管理**: 添加/删除/批量导入抓取目标（URL/ID）
 * 2. **URL解析**: 自动识别并提取各平台的视频/笔记ID
 * 3. **状态追踪**: 实时跟踪每个目标的抓取进度和状态
 * 4. **去重管理**: 防止重复抓取相同目标
 * 5. **批量操作**: 支持Excel/CSV批量导入目标列表
 *
 * 支持的目标格式:
 * - 抖音: https://v.douyin.com/i2eABC123/ 或 https://www.douyin.com/video/7234567890123456789
 * - 小红书: http://xhslink.com/a/xxxxx 或 https://www.xiaohongshu.com/explore/65a1b2c3d4e5f6g7h8i9
 * - 快手: https://v.kuaishou.com/xxx 或 https://www.kuaishou.com/short-video/3xXXXXX
 * - 微博: https://weibo.com/1234567890/AbcDefGhi 或 https://m.weibo.cn/status/4800000000000000
 * - B站: BV1xx411c7mD 或 av12345678
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Service
@Slf4j
public class CustomTargetPoolService {

    // 内存存储 (生产环境应替换为Redis或数据库)
    private final Map<String, CrawlTarget> targetPool = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userTargets = new ConcurrentHashMap<>();

    /**
     * 抓取目标数据模型
     */
    @lombok.Data
    public static class CrawlTarget {
        private String targetId;                // 唯一标识 (MD5 hash)
        private String platform;                // 平台代码
        private String originalInput;           // 原始输入 (URL或ID)
        private String resolvedId;              // 解析后的真实ID (视频ID/笔记ID)
        private String targetUrl;               // 标准化URL
        private TargetStatus status;            // 当前状态
        private LocalDateTime createdAt;        // 创建时间
        private LocalDateTime lastCrawledAt;    // 上次抓取时间
        private CommentCrawlResult lastResult;  // 最近一次抓取结果
        private int crawlCount;                 // 累计抓取次数
        private String addedByUser;             // 添加者用户ID
        private Map<String, Object> metadata;   // 额外元信息

        public CrawlTarget() {}

        public static CrawlTargetBuilder builder() {
            return new CrawlTargetBuilder();
        }

        public CrawlTarget(String targetId, String platform, String originalInput,
                          String resolvedId, String targetUrl) {
            this.targetId = targetId;
            this.platform = platform;
            this.originalInput = originalInput;
            this.resolvedId = resolvedId;
            this.targetUrl = targetUrl;
            this.status = TargetStatus.PENDING;
            this.createdAt = LocalDateTime.now();
            this.crawlCount = 0;
        }

        public static class CrawlTargetBuilder {
            private CrawlTarget target = new CrawlTarget();

            public CrawlTargetBuilder targetId(String targetId) { target.targetId = targetId; return this; }
            public CrawlTargetBuilder platform(String platform) { target.platform = platform; return this; }
            public CrawlTargetBuilder originalInput(String originalInput) { target.originalInput = originalInput; return this; }
            public CrawlTargetBuilder resolvedId(String resolvedId) { target.resolvedId = resolvedId; return this; }
            public CrawlTargetBuilder targetUrl(String targetUrl) { target.targetUrl = targetUrl; return this; }
            public CrawlTargetBuilder addedByUser(String addedByUser) { target.addedByUser = addedByUser; return this; }

            public CrawlTarget build() {
                if (target.status == null) target.status = TargetStatus.PENDING;
                if (target.createdAt == null) target.createdAt = LocalDateTime.now();
                if (target.crawlCount == 0) target.crawlCount = 0;
                return target;
            }
        }
    }

    /**
     * 目标状态枚举
     */
    public enum TargetStatus {
        PENDING("待抓取"),
        CRAWLING("抓取中"),
        COMPLETED("已完成"),
        PARTIAL("部分完成(有错误)"),
        FAILED("失败"),
        SKIPPED("跳过(重复)");

        private final String description;
        TargetStatus(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    /**
     * 添加单个抓取目标 (支持URL自动解析)
     *
     * @param input 用户输入 (URL或平台ID)
     * @param userId 添加者用户ID
     * @return 创建的目标对象
     */
    public CrawlTarget addTarget(String input, String userId) {
        log.info("📥 添加抓取目标: {} | 用户: {}", input, userId);

        // 1. 自动识别平台 + 解析真实ID
        ParsedTarget parsed = parseAndResolve(input);
        if (parsed == null) {
            throw new IllegalArgumentException("无法识别的目标格式: " + input +
                    "\n支持的格式:\n" +
                    "- 抖音: https://v.douyin.com/xxx 或 video ID\n" +
                    "- 小红书: http://xhslink.com/xxx 或 note ID\n" +
                    "- 快手: https://v.kuaishou.com/xxx\n" +
                    "- 微博: weibo.com 链接\n" +
                    "- B站: BV号 或 av号");
        }

        // 2. 生成唯一ID
        String targetId = generateTargetId(parsed.getPlatform(), parsed.getResolvedId());

        // 3. 去重检查
        if (targetPool.containsKey(targetId)) {
            log.warn("⚠️ 目标已存在，跳过添加: {}", targetId);
            CrawlTarget existing = targetPool.get(targetId);
            existing.setStatus(TargetStatus.SKIPPED);
            return existing;
        }

        // 4. 创建目标对象
        CrawlTarget target = CrawlTarget.builder()
                .targetId(targetId)
                .platform(parsed.getPlatform())
                .originalInput(input)
                .resolvedId(parsed.getResolvedId())
                .targetUrl(parsed.getNormalizedUrl())
                .addedByUser(userId)
                .build();

        // 5. 存储到内存池
        targetPool.put(targetId, target);

        // 6. 关联到用户
        userTargets.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                  .add(targetId);

        log.info("✅ 目标添加成功: {} | 平台: {} | 解析ID: {}",
                targetId, parsed.getPlatform(), parsed.getResolvedId());

        return target;
    }

    /**
     * 批量添加目标 (支持多种输入格式)
     *
     * @param inputs 输入列表 (URL/ID混合)
     * @param userId 用户ID
     * @return 添加结果统计
     */
    public BatchAddResult batchAddTargets(List<String> inputs, String userId) {
        BatchAddResult result = new BatchAddResult();

        for (String input : inputs) {
            try {
                if (input == null || input.trim().isEmpty()) continue;

                CrawlTarget target = addTarget(input.trim(), userId);
                result.addSuccess(target);
            } catch (Exception e) {
                log.error("❌ 添加目标失败: {} | 错误: {}", input, e.getMessage());
                result.addFailure(input, e.getMessage());
            }
        }

        log.info("📊 批量添加完成 | 总数:{} | 成功:{} | 失败:{} | 跳过:{}",
                inputs.size(), result.successCount, result.failureCount, result.skippedCount);

        return result;
    }

    /**
     * 从Excel/CSV内容批量导入
     *
     * @param csvContent CSV格式内容 (每行一个URL)
     * @param delimiter 分隔符 (默认逗号)
     * @param userId 用户ID
     * @return 批量添加结果
     */
    public BatchAddResult importFromCsv(String csvContent, String delimiter, String userId) {
        List<String> lines = Arrays.asList(csvContent.split("\\r?\\n"));
        return batchAddTargets(lines, userId);
    }

    /**
     * 解析并标准化目标输入
     *
     * 支持的URL模式:
     * - 抖音短链: v.douyin.com
     * - 抖音长链: www.douyin.com/video/{item_id}
     * - 小红书短链: xhslink.com
     * - 小红书长链: www.xiaohongshu.com/explore/{note_id}
     * - 快手: v.kuaishou.com / www.kuaishou.com
     * - 微博: weibo.com / m.weibo.cn
     * - B站: bilibili.com/video/BV{bvid} / av{avid}
     */
    public ParsedTarget parseAndResolve(String input) {
        if (input == null || input.trim().isEmpty()) return null;

        input = input.trim();

        // ====== 抖音 ======
        if (input.contains("douyin.com") || input.contains("douyin")) {
            return parseDouyinInput(input);
        }

        // ====== 小红书 ======
        if (input.contains("xiaohongshu.com") || input.contains("xhslink.com") ||
            input.contains("小红书") || input.length() == 24 && input.matches("[a-f0-9]+")) {
            return parseXiaohongshuInput(input);
        }

        // ====== 快手 ======
        if (input.contains("kuaishou.com") || input.contains("kuaishou")) {
            return parseKuaishouInput(input);
        }

        // ====== 微博 ======
        if (input.contains("weibo.com") || input.contains("weibo.cn") || input.contains("微博")) {
            return parseWeiboInput(input);
        }

        // ====== B站 ======
        if (input.contains("bilibili.com") || input.startsWith("BV") || input.startsWith("av")) {
            return parseBilibiliInput(input);
        }

        return null;
    }

    // ====== 各平台解析器实现 ======

    private ParsedTarget parseDouyinInput(String input) {
        String itemId = null;
        String normalizedUrl = null;

        // 短链格式: https://v.douyin.com/i2eABC123/
        if (input.contains("v.douyin.com")) {
            itemId = extractFromShortLink(input);  // 注意: 短链需要实际请求才能获取真实ID
            normalizedUrl = input;
        }
        // 长链格式: https://www.douyin.com/video/7234567890123456789
        else if (input.contains("/video/")) {
            itemId = extractVideoId(input, "/video/");
            normalizedUrl = "https://www.douyin.com/video/" + itemId;
        }
        // 直接输入视频ID (19位数字)
        else if (input.matches("\\d{19}")) {
            itemId = input;
            normalizedUrl = "https://www.douyin.com/video/" + input;
        }

        if (itemId != null) {
            return new ParsedTarget("DOUYIN", itemId, normalizedUrl != null ? normalizedUrl : input);
        }
        return null;
    }

    private ParsedTarget parseXiaohongshuInput(String input) {
        String noteId = null;
        String normalizedUrl = null;

        // 短链格式: http://xhslink.com/a/xxxxx
        if (input.contains("xhslink.com")) {
            noteId = extractFromShortLink(input);
            normalizedUrl = input;
        }
        // 长链格式: https://www.xiaohongshu.com/explore/65a1b2c3d4e5f6g7h8i9
        else if (input.contains("/explore/") || input.contains("/discovery/item/")) {
            noteId = extractNoteId(input);
            normalizedUrl = "https://www.xiaohongshu.com/explore/" + noteId;
        }
        // 直接输入笔记ID (24位hex字符)
        else if (input.matches("[a-fA-F0-9]{24}")) {
            noteId = input;
            normalizedUrl = "https://www.xiaohongshu.com/explore/" + input;
        }

        if (noteId != null) {
            return new ParsedTarget("XIAOHONGSHU", noteId, normalizedUrl != null ? normalizedUrl : input);
        }
        return null;
    }

    private ParsedTarget parseKuaishouInput(String input) {
        String photoId = null;

        if (input.contains("v.kuaishou.com")) {
            photoId = extractFromShortLink(input);
        } else if (input.contains("/short-video/")) {
            photoId = extractVideoId(input, "/short-video/");
        }

        if (photoId != null) {
            return new ParsedTarget("KUAISHOU", photoId, input);
        }
        return null;
    }

    private ParsedTarget parseWeiboInput(String input) {
        String statusId = null;

        if (input.contains("weibo.com") && input.contains("/status/")) {
            statusId = extractVideoId(input, "/status/");
        } else if (input.contains("m.weibo.cn") && input.contains("/detail/")) {
            statusId = extractVideoId(input, "/detail/");
        }

        if (statusId != null) {
            return new ParsedTarget("WEIBO", statusId, input);
        }
        return null;
    }

    private ParsedTarget parseBilibiliInput(String input) {
        String bvidOrAvid = null;

        if (input.contains("/video/BV")) {
            int start = input.indexOf("/video/BV") + 7;
            int end = input.indexOf("/", start);
            if (end == -1) end = input.indexOf("?", start);
            if (end == -1) end = input.length();
            bvidOrAvid = input.substring(start, Math.min(end, start + 12));  // BV号长度约12
        } else if (input.startsWith("BV")) {
            bvidOrAvid = input.substring(0, Math.min(input.length(), 12));
        } else if (input.startsWith("av") || input.matches("\\d+")) {
            bvidOrAvid = "av" + input.replaceAll("\\D", "");
        }

        if (bvidOrAvid != null) {
            return new ParsedTarget("BILIBILI", bvidOrAvid,
                    "https://www.bilibili.com/video/" + bvidOrAvid);
        }
        return null;
    }

    // ====== 工具方法 ======

    private String extractVideoId(String url, String pattern) {
        try {
            int start = url.indexOf(pattern) + pattern.length();
            int end = url.indexOf("/", start);
            if (end == -1) end = url.indexOf("?", start);
            if (end == -1) end = url.length();
            return url.substring(start, end).split("\\?")[0];
        } catch (Exception e) {
            return null;
        }
    }

    private String extractNoteId(String url) {
        if (url.contains("/explore/")) {
            return extractVideoId(url, "/explore/");
        } else if (url.contains("/discovery/item/")) {
            return extractVideoId(url, "/discovery/item/");
        }
        return null;
    }

    private String extractFromShortLink(String shortUrl) {
        return shortUrl;  // 短链需后续HTTP请求获取真实ID
    }

    private String generateTargetId(String platform, String resolvedId) {
        return platform + "_" + resolvedId;
    }

    // ====== 查询接口 ======

    public CrawlTarget getTarget(String targetId) {
        return targetPool.get(targetId);
    }

    public List<CrawlTarget> getUserTargets(String userId) {
        Set<String> targetIds = userTargets.get(userId);
        if (targetIds == null) return Collections.emptyList();

        List<CrawlTarget> targets = new ArrayList<>();
        for (String id : targetIds) {
            CrawlTarget target = targetPool.get(id);
            if (target != null) targets.add(target);
        }
        return targets;
    }

    public List<CrawlTarget> getAllTargets() {
        return new ArrayList<>(targetPool.values());
    }

    public void updateTargetStatus(String targetId, TargetStatus status) {
        CrawlTarget target = targetPool.get(targetId);
        if (target != null) {
            target.setStatus(status);
            if (status == TargetStatus.COMPLETED || status == TargetStatus.FAILED) {
                target.setLastCrawledAt(LocalDateTime.now());
                target.setCrawlCount(target.getCrawlCount() + 1);
            }
        }
    }

    public void updateTargetResult(String targetId, CommentCrawlResult result) {
        CrawlTarget target = targetPool.get(targetId);
        if (target != null) {
            target.setLastResult(result);
        }
    }

    public boolean removeTarget(String targetId, String userId) {
        CrawlTarget target = targetPool.get(targetId);
        if (target != null && target.getAddedByUser().equals(userId)) {
            targetPool.remove(targetId);
            Set<String> userTargetSet = userTargets.get(userId);
            if (userTargetSet != null) userTargetSet.remove(targetId);
            return true;
        }
        return false;
    }

    public int getPoolSize() {
        return targetPool.size();
    }

    // ====== 数据模型 ======

    @Data
    public static class ParsedTarget {
        private String platform;
        private String resolvedId;
        private String normalizedUrl;

        public ParsedTarget() {}

        public ParsedTarget(String platform, String resolvedId, String normalizedUrl) {
            this.platform = platform;
            this.resolvedId = resolvedId;
            this.normalizedUrl = normalizedUrl;
        }
    }

    public static class BatchAddResult {
        private int successCount = 0;
        private int failureCount = 0;
        private int skippedCount = 0;
        private List<CrawlTarget> successTargets = new ArrayList<>();
        private Map<String, String> failures = new LinkedHashMap<>();

        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<CrawlTarget> getSuccessTargets() { return successTargets; }
        public Map<String, String> getFailures() { return failures; }

        public void addSuccess(CrawlTarget target) {
            if ("SKIPPED".equals(target.getStatus().name())) {
                skippedCount++;
            } else {
                successCount++;
            }
            successTargets.add(target);
        }

        public void addFailure(String input, String error) {
            failureCount++;
            failures.put(input, error);
        }
    }
}
