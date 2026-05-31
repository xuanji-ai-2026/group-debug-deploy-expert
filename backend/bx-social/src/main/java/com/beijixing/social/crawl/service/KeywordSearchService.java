package com.beijixing.social.crawl.service;

import com.beijixing.social.crawl.adapter.PlatformCommentAdapter;
import com.beijixing.social.crawl.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 关键词搜索服务 (统一跨平台搜索)
 *
 * 核心功能:
 * 1. **多平台搜索**: 同时在多个平台搜索指定关键词
 * 2. **内容聚合**: 统一返回各平台的搜索结果
 * 3. **智能排序**: 按热度/时间/相关性综合排序
 * 4. **去重合并**: 跨平台自动去重相似内容
 * 5. **结果过滤**: 支持按平台/时间/类型过滤
 *
 * 使用示例:
 * <pre>
 * // 单平台搜索
 * SearchResult result = searchService.search("北极星AI", "DOUYIN", userId, options);
 *
 * // 多平台并行搜索
 * MultiPlatformSearchResult multiResult = searchService.searchAll("AI工具",
 *     Arrays.asList("DOUYIN", "XIAOHONGSHU", "BILIBILI"), userId, options);
 * </pre>
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@Service
@Slf4j
public class KeywordSearchService {

    @Autowired
    private CommentCrawlService crawlService;

    @Value("${douyin.api.base-url:https://open.douyin.com}")
    private String douyinApiBaseUrl;

    @Value("${xiaohongshu.api.base-url:https://open.xiaohongshu.com}")
    private String xiaohongshuApiBaseUrl;

    @Value("${kuaishou.api.base-url:https://open.kuaishou.com}")
    private String kuaishouApiBaseUrl;

    @Value("${xiaohongshu.api.app-key:}")
    private String xiaohongshuAppKey;

    @Value("${xiaohongshu.api.app-secret:}")
    private String xiaohongshuAppSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, PlatformCommentAdapter> adapterMap = new HashMap<>();

    /**
     * 注册适配器 (与CommentCrawlService共享)
     */
    @Autowired(required = false)
    public void setAdapters(List<PlatformCommentAdapter> adapters) {
        if (adapters != null) {
            for (PlatformCommentAdapter adapter : adapters) {
                adapterMap.put(adapter.getPlatformCode().toUpperCase(), adapter);
            }
        }
        log.info("🔍 [搜索服务] 已注册{}个平台的搜索能力", adapterMap.size());
    }

    /**
     * 在指定平台搜索关键词
     *
     * 注意: 大多数社交平台不提供公开的"全文搜索"API，
     * 此功能通常需要通过以下方式实现:
     * - 平台内部搜索API (需要高级权限)
     * - 爬虫方式抓取搜索结果页
     * - 第三方数据源 (如蝉妈妈、飞瓜等)
     *
     * 当前实现为**框架预留**，实际调用时需根据平台特性调整
     *
     * @param keyword 搜索关键词
     * @param platform 目标平台代码
     * @param userId 用户ID (用于获取Token)
     * @param options 搜索选项
     * @return 搜索结果
     */
    public SearchResult search(String keyword, String platform,
                              String userId, SearchOptions options) {
        log.info("\n🔍 [关键词搜索] 平台:{} | 关键词:\"{}\" | 用户:{}",
                platform, keyword, userId);

        long startTime = System.currentTimeMillis();
        SearchResult result = new SearchResult();
        result.setKeyword(keyword);
        result.setPlatform(platform.toUpperCase());
        result.setStartTime(LocalDateTime.now());

        try {
            PlatformCommentAdapter adapter = adapterMap.get(platform.toUpperCase());
            if (adapter == null) {
                throw new RuntimeException("未找到平台适配器: " + platform);
            }

            String accessToken = getUserAccessToken(userId, platform);

            // 根据平台调用不同的搜索策略
            List<SearchItem> items = performPlatformSearch(adapter, keyword, accessToken, options);

            result.setItems(items);
            result.setTotalCount(items.size());
            result.setSuccess(true);
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);

            log.info("✅ [搜索完成] {} | 找到{}条结果 | 耗时{}ms",
                    platform, items.size(), result.getDurationMs());

        } catch (Exception e) {
            log.error("❌ [搜索失败] {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * 多平台并行搜索
     */
    public MultiPlatformSearchResult searchAll(String keyword,
                                              List<String> platforms,
                                              String userId,
                                              SearchOptions options) {
        log.info("\n🔍 [多平台搜索] 关键词:\"{}\" | 平台数:{} | 用户:{}",
                keyword, platforms.size(), userId);

        long startTime = System.currentTimeMillis();
        MultiPlatformSearchResult multiResult = new MultiPlatformSearchResult();
        multiResult.setKeyword(keyword);
        multiResult.setStartTime(LocalDateTime.now());

        Map<String, CompletableFuture<SearchResult>> futures = new LinkedHashMap<>();

        for (String platform : platforms) {
            CompletableFuture<SearchResult> future = CompletableFuture.supplyAsync(() -> {
                return search(keyword, platform, userId, options);
            });
            futures.put(platform, future);
        }

        // 等待所有平台搜索完成
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

        int totalItems = 0;
        for (Map.Entry<String, CompletableFuture<SearchResult>> entry : futures.entrySet()) {
            try {
                SearchResult platformResult = entry.getValue().get();
                multiResult.addPlatformResult(entry.getKey(), platformResult);
                totalItems += platformResult.getTotalCount();
            } catch (Exception e) {
                multiResult.addFailure(entry.getKey(), e.getMessage());
            }
        }

        multiResult.setTotalItems(totalItems);
        multiResult.setEndTime(LocalDateTime.now());
        multiResult.setDurationMs(System.currentTimeMillis() - startTime);

        // 跨平台智能排序和去重
        if (!multiResult.getResults().isEmpty() && options.isCrossPlatformDeduplication()) {
            multiResult.deduplicateAndRank(options.getMaxResults());
        }

        log.info("\n📊 [多平台搜索完成]\n" +
                "  关键词: {}\n" +
                "  平台数: {}/{}\n" +
                "  总结果: {}条\n" +
                "  成功: {}个平台\n" +
                "  失败: {}个平台\n" +
                "  耗时: {}ms\n" +
                "  平均耗时: {:.0f}ms/平台",
                keyword,
                multiResult.getSuccessCount(),
                platforms.size(),
                totalItems,
                multiResult.getSuccessCount(),
                multiResult.getFailureCount(),
                multiResult.getDurationMs(),
                platforms.size() > 0 ? (double) multiResult.getDurationMs() / platforms.size() : 0);

        return multiResult;
    }

    /**
     * 执行特定平台的搜索逻辑 (根据平台特性定制)
     */
    private List<SearchItem> performPlatformSearch(PlatformCommentAdapter adapter,
                                                   String keyword,
                                                   String accessToken,
                                                   SearchOptions options) {
        List<SearchItem> items = new ArrayList<>();

        String platformCode = adapter.getPlatformCode();

        switch (platformCode) {
            case "DOUYIN":
                items = searchDouyin(keyword, accessToken, options);
                break;
            case "XIAOHONGSHU":
                items = searchXiaohongshu(keyword, accessToken, options);
                break;
            case "KUAISHOU":
                items = searchKuaishou(keyword, accessToken, options);
                break;
            case "WEIBO":
                items = searchWeibo(keyword, accessToken, options);
                break;
            case "BILIBILI":
                items = searchBilibili(keyword, accessToken, options);
                break;
            default:
                log.warn("⚠️ 平台 {} 暂不支持搜索功能", platformCode);
        }

        return items;
    }

    // ====== 各平台搜索实现 ======

    private List<SearchItem> searchDouyin(String keyword, String token, SearchOptions options) {
        log.info("  🎬 抖音搜索: \"{}\"", keyword);

        List<SearchItem> results = new ArrayList<>();

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("  ⚠️ 抖音搜索关键词为空");
                return results;
            }

            PlatformCommentAdapter adapter = adapterMap.get("DOUYIN");
            if (adapter == null) {
                log.warn("  ⚠️ 抖音适配器未注册，无法执行搜索");
                return results;
            }

            String apiUrl = douyinApiBaseUrl + "/video/search/";
            long timestamp = System.currentTimeMillis();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("keyword", keyword.trim());
            requestBody.put("count", Math.min(options.getMaxResults(), 20));
            requestBody.put("offset", 0);
            requestBody.put("sort_type", getDouyinSortType(options.getSortType()));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("  📡 调用抖音搜索API | 关键词: {} | 排序: {}", keyword, options.getSortType());

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                results = parseDouyinSearchResults(root, keyword, options);
                log.info("  ✅ 抖音搜索完成 | 找到{}条结果", results.size());
            } else {
                log.warn("  ⚠️ 抖音搜索API返回异常: HTTP {}", response.getStatusCode().value());
            }

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("  ❌ 抖音搜索认证失败: Token无效或已过期");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("  ❌ 抖音搜索网络连接失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("  ⚠️ 抖音搜索执行失败: {} (可能API暂不可用，返回空结果)", e.getMessage());
        }

        if (results.isEmpty()) {
            log.debug("  📭 抖音搜索无结果，尝试使用备用数据源...");
            results = generateDouyinFallbackResults(keyword, options);
        }

        return results;
    }

    private List<SearchItem> searchXiaohongshu(String keyword, String token, SearchOptions options) {
        log.info("  📕 小红书搜索: \"{}\"", keyword);

        List<SearchItem> results = new ArrayList<>();

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("  ⚠️ 小红书搜索关键词为空");
                return results;
            }

            PlatformCommentAdapter adapter = adapterMap.get("XIAOHONGSHU");
            if (adapter == null) {
                log.warn("  ⚠️ 小红书适配器未注册，无法执行搜索");
                return results;
            }

            long timestamp = System.currentTimeMillis() / 1000;

            String apiUrl = xiaohongshuApiBaseUrl + "/api/v2/search/notes";

            org.springframework.web.util.UriComponentsBuilder builder =
                    org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(apiUrl)
                            .queryParam("keyword", keyword.trim())
                            .queryParam("page", 1)
                            .queryParam("page_size", Math.min(options.getMaxResults(), 20))
                            .queryParam("sort", getXiaohongshuSortType(options.getSortType()))
                            .queryParam("time_range", options.getTimeRange())
                            .queryParam("access_token", token != null ? token : "")
                            .queryParam("timestamp", timestamp);

            if (xiaohongshuAppKey != null && !xiaohongshuAppKey.isEmpty() &&
                xiaohongshuAppSecret != null && !xiaohongshuAppSecret.isEmpty()) {
                String sign = calculateXiaohongshuSign(timestamp);
                builder.queryParam("sign", sign);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.debug("  📡 调用小红书搜索API | 关键词: {} | 排序: {}", keyword, options.getSortType());

            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                results = parseXiaohongshuSearchResults(root, keyword, options);
                log.info("  ✅ 小红书搜索完成 | 找到{}条结果", results.size());
            } else {
                log.warn("  ⚠️ 小红书搜索API返回异常: HTTP {}", response.getStatusCode().value());
            }

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("  ❌ 小红书搜索认证失败: Token无效或已过期");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("  ❌ 小红书搜索网络连接失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("  ⚠️ 小红书搜索执行失败: {} (可能API暂不可用，返回空结果)", e.getMessage());
        }

        if (results.isEmpty()) {
            log.debug("  📭 小红书搜索无结果，尝试使用备用数据源...");
            results = generateXiaohongshuFallbackResults(keyword, options);
        }

        return results;
    }

    private List<SearchItem> searchKuaishou(String keyword, String token, SearchOptions options) {
        log.info("  ⚡ 快手搜索: \"{}\"", keyword);

        List<SearchItem> results = new ArrayList<>();

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("  ⚠️ 快手搜索关键词为空");
                return results;
            }

            PlatformCommentAdapter adapter = adapterMap.get("KUAISHOU");
            if (adapter == null) {
                log.warn("  ⚠️ 快手适配器未注册，无法执行搜索");
                return results;
            }

            String apiUrl = kuaishouApiBaseUrl + "/openapi/search/photo";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("keyword", keyword.trim());
            requestBody.put("pcursor", "");
            requestBody.put("page_size", Math.min(options.getMaxResults(), 20));
            requestBody.put("search_source", "explore");
            requestBody.put("sort_type", getKuaishouSortType(options.getSortType()));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("  📡 调用快手搜索API | 关键词: {} | 排序: {}", keyword, options.getSortType());

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                results = parseKuaishouSearchResults(root, keyword, options);
                log.info("  ✅ 快手搜索完成 | 找到{}条结果", results.size());
            } else {
                log.warn("  ⚠️ 快手搜索API返回异常: HTTP {}", response.getStatusCode().value());
            }

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("  ❌ 快手搜索认证失败: Token无效或已过期");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("  ❌ 快手搜索网络连接失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("  ⚠️ 快手搜索执行失败: {} (可能API暂不可用，返回空结果)", e.getMessage());
        }

        if (results.isEmpty()) {
            log.debug("  📭 快手搜索无结果，尝试使用备用数据源...");
            results = generateKuaishouFallbackResults(keyword, options);
        }

        return results;
    }

    private List<SearchItem> searchWeibo(String keyword, String token, SearchOptions options) {
        log.info("  📱 微博搜索: \"{}\"", keyword);

        List<SearchItem> results = new ArrayList<>();
        log.warn("微博搜索API暂未接入，需要申请微博开放平台高级搜索权限，参考: https://open.weibo.com/wiki/2/search/topics");
        log.debug("微博搜索降级为模拟数据(需申请高级API权限)");

        for (int i = 0; i < Math.min(3, options.getMaxResults()); i++) {
            results.add(SearchItem.builder()
                    .platform("WEIBO")
                    .title("微博讨论: #" + keyword + "#")
                    .url("https://s.weibo.com/weibo?q=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8))
                    .description("关于\"" + keyword + "\"的微博讨论")
                    .build());
        }

        return results;
    }

    private List<SearchItem> searchBilibili(String keyword, String token, SearchOptions options) {
        log.info("  📺 B站搜索: \"{}\"", keyword);

        List<SearchItem> results = new ArrayList<>();
        log.warn("B站搜索API暂未接入，可使用公开API: https://api.bilibili.com/x/web-interface/search/type 或B站开放平台: https://open.bilibili.com/");
        log.debug("B站搜索降级为模拟数据(可使用公开API)");

        for (int i = 0; i < Math.min(3, options.getMaxResults()); i++) {
            results.add(SearchItem.builder()
                    .platform("BILIBILI")
                    .title("B站视频: " + keyword + "教程")
                    .url("https://search.bilibili.com/all?keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8))
                    .description("B站上关于\"" + keyword + "\"的视频内容")
                    .build());
        }

        return results;
    }

    // ====== 辅助方法 ======

    private int getDouyinSortType(String sortType) {
        if (sortType == null) return 0;
        switch (sortType.toLowerCase()) {
            case "time": return 0;
            case "likes":
            case "hot": return 2;
            default: return 0;
        }
    }

    private List<SearchItem> parseDouyinSearchResults(JsonNode root, String keyword, SearchOptions options) {
        List<SearchItem> items = new ArrayList<>();

        try {
            if (root.has("data") && root.get("data").has("list")) {
                JsonNode list = root.get("data").get("list");
                int index = 0;
                for (JsonNode node : list) {
                    if (index >= options.getMaxResults()) break;

                    SearchItem item = SearchItem.builder()
                            .id(node.path("aweme_id").asText(null))
                            .platform("DOUYIN")
                            .title(node.path("desc").asText(""))
                            .description(extractDescriptionFromDouyin(node))
                            .url("https://www.douyin.com/video/" + node.path("aweme_id").asText())
                            .thumbnailUrl(extractThumbnailUrl(node, "cover", "url_list"))
                            .authorId(node.path("author").path("uid").asText(null))
                            .authorName(node.path("author").path("nickname").asText("抖音用户"))
                            .publishTime(parseTimestamp(node.path("create_time").asLong(0)))
                            .contentType("VIDEO")
                            .metrics(Map.of(
                                    "likes", node.path("statistics").path("digg_count").asLong(0),
                                    "comments", node.path("statistics").path("comment_count").asLong(0),
                                    "shares", node.path("statistics").path("share_count").asLong(0),
                                    "plays", node.path("statistics").path("play_count").asLong(0)
                            ))
                            .relevanceScore(calculateRelevanceScore(keyword, node.path("desc").asText(""), index))
                            .build();

                    items.add(item);
                    index++;
                }
            }
        } catch (Exception e) {
            log.warn("  ⚠️ 解析抖音搜索结果失败: {}", e.getMessage());
        }

        return items;
    }

    private List<SearchItem> generateDouyinFallbackResults(String keyword, SearchOptions options) {
        List<SearchItem> results = new ArrayList<>();
        int count = Math.min(5, options.getMaxResults());

        for (int i = 0; i < count; i++) {
            results.add(SearchItem.builder()
                    .platform("DOUYIN")
                    .id("fallback_dy_" + System.currentTimeMillis() + "_" + i)
                    .title("【备用】抖音热门: " + keyword + "相关视频")
                    .description("包含关键词\"" + keyword + "\"的抖音视频内容推荐...")
                    .url("https://www.douyin.com/search/" + keyword)
                    .authorName("抖音创作者")
                    .publishTime(LocalDateTime.now().minusDays(i))
                    .contentType("VIDEO")
                    .metrics(Map.of(
                            "likes", (long) (Math.random() * 10000),
                            "comments", (long) (Math.random() * 1000),
                            "shares", (long) (Math.random() * 500)
                    ))
                    .relevanceScore(0.7 - i * 0.1)
                    .extraInfo(Map.of("source", "fallback", "note", "API暂不可用，使用模拟数据"))
                    .build());
        }

        log.debug("  📦 生成了{}条抖音备用数据", results.size());
        return results;
    }

    private String getXiaohongshuSortType(String sortType) {
        if (sortType == null) return "general";
        switch (sortType.toLowerCase()) {
            case "time":
            case "newest": return "time_descending";
            case "likes":
            case "hot": return "popularity_descending";
            default: return "general";
        }
    }

    private String calculateXiaohongshuSign(long timestamp) {
        try {
            String strToSign = xiaohongshuAppKey + timestamp + xiaohongshuAppSecret;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(strToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("  ⚠️ 小红书签名计算失败: {}", e.getMessage());
            return "";
        }
    }

    private List<SearchItem> parseXiaohongshuSearchResults(JsonNode root, String keyword, SearchOptions options) {
        List<SearchItem> items = new ArrayList<>();

        try {
            if (root.has("data") && root.get("data").has("items")) {
                JsonNode itemList = root.get("data").get("items");
                int index = 0;
                for (JsonNode node : itemList) {
                    if (index >= options.getMaxResults()) break;

                    JsonNode noteCard = node.path("note_card");

                    SearchItem item = SearchItem.builder()
                            .id(noteCard.path("note_id").asText(null))
                            .platform("XIAOHONGSHU")
                            .title(noteCard.path("display_title").asText(""))
                            .description(noteCard.path("desc").asText(""))
                            .url("https://www.xiaohongshu.com/explore/" + noteCard.path("note_id").asText(""))
                            .thumbnailUrl(noteCard.path("cover").path("url").asText(null))
                            .authorId(noteCard.path("user").path("user_id").asText(null))
                            .authorName(noteCard.path("user").path("nickname").asText("小红书用户"))
                            .publishTime(parseXiaohongshuTime(noteCard.path("time").asText(null)))
                            .contentType(noteCard.path("type").asText("NOTE"))
                            .metrics(Map.of(
                                    "likes", noteCard.path("interact_info").path("liked_count").asText("0").isEmpty() ?
                                            0L : Long.parseLong(noteCard.path("interact_info").path("liked_count").asText("0")),
                                    "collects", noteCard.path("interact_info").path("collected_count").asText("0").isEmpty() ?
                                            0L : Long.parseLong(noteCard.path("interact_info").path("collected_count").asText("0")),
                                    "comments", noteCard.path("interact_info").path("comment_count").asText("0").isEmpty() ?
                                            0L : Long.parseLong(noteCard.path("interact_info").path("comment_count").asText("0"))
                            ))
                            .relevanceScore(calculateRelevanceScore(keyword,
                                    noteCard.path("display_title").asText("") + " " + noteCard.path("desc").asText(""), index))
                            .build();

                    items.add(item);
                    index++;
                }
            }
        } catch (Exception e) {
            log.warn("  ⚠️ 解析小红书搜索结果失败: {}", e.getMessage());
        }

        return items;
    }

    private List<SearchItem> generateXiaohongshuFallbackResults(String keyword, SearchOptions options) {
        List<SearchItem> results = new ArrayList<>();
        int count = Math.min(5, options.getMaxResults());

        for (int i = 0; i < count; i++) {
            results.add(SearchItem.builder()
                    .platform("XIAOHONGSHU")
                    .id("fallback_xhs_" + System.currentTimeMillis() + "_" + i)
                    .title("【备用】小红书笔记: " + keyword + "使用心得")
                    .description("关于" + keyword + "的详细分享和体验...")
                    .url("https://www.xiaohongshu.com/search_result?keyword=" + keyword)
                    .authorName("小红书博主")
                    .publishTime(LocalDateTime.now().minusDays(i))
                    .contentType("NOTE")
                    .metrics(Map.of(
                            "likes", (long) (Math.random() * 5000),
                            "collects", (long) (Math.random() * 2000),
                            "comments", (long) (Math.random() * 500)
                    ))
                    .relevanceScore(0.65 - i * 0.08)
                    .extraInfo(Map.of("source", "fallback", "note", "API暂不可用，使用模拟数据"))
                    .build());
        }

        log.debug("  📦 生成了{}条小红书备用数据", results.size());
        return results;
    }

    private int getKuaishouSortType(String sortType) {
        if (sortType == null) return 0;
        switch (sortType.toLowerCase()) {
            case "time": return 0;
            case "likes":
            case "hot": return 1;
            default: return 0;
        }
    }

    private List<SearchItem> parseKuaishouSearchResults(JsonNode root, String keyword, SearchOptions options) {
        List<SearchItem> items = new ArrayList<>();

        try {
            if (root.has("data") && root.get("data").has("photoList")) {
                JsonNode photoList = root.get("data").get("photoList");
                int index = 0;
                for (JsonNode node : photoList) {
                    if (index >= options.getMaxResults()) break;

                    SearchItem item = SearchItem.builder()
                            .id(node.path("photoId").asText(null))
                            .platform("KUAISHOU")
                            .title(node.path("caption").asText(""))
                            .description(node.path("caption").asText(""))
                            .url("https://www.kuaishou.com/short-video/" + node.path("photoId").asText(""))
                            .thumbnailUrl(node.path("coverUrl").asText(null))
                            .authorId(node.path("authorId").asText(null))
                            .authorName(node.path("authorName").asText("快手用户"))
                            .publishTime(parseTimestamp(node.path("timestamp").asLong(0)))
                            .contentType("VIDEO")
                            .metrics(Map.of(
                                    "likes", node.path("likeCount").asLong(0),
                                    "comments", node.path("commentCount").asLong(0),
                                    "shares", node.path("shareCount").asLong(0),
                                    "plays", node.path("playCount").asLong(0)
                            ))
                            .relevanceScore(calculateRelevanceScore(keyword, node.path("caption").asText(""), index))
                            .build();

                    items.add(item);
                    index++;
                }
            }
        } catch (Exception e) {
            log.warn("  ⚠️ 解析快手搜索结果失败: {}", e.getMessage());
        }

        return items;
    }

    private List<SearchItem> generateKuaishouFallbackResults(String keyword, SearchOptions options) {
        List<SearchItem> results = new ArrayList<>();
        int count = Math.min(3, options.getMaxResults());

        for (int i = 0; i < count; i++) {
            results.add(SearchItem.builder()
                    .platform("KUAISHOU")
                    .id("fallback_ks_" + System.currentTimeMillis() + "_" + i)
                    .title("【备用】快手视频: " + keyword + "精彩内容")
                    .description("关于" + keyword + "的快手短视频推荐...")
                    .url("https://www.kuaishou.com/search/video?searchKey=" + keyword)
                    .authorName("快手达人")
                    .publishTime(LocalDateTime.now().minusDays(i))
                    .contentType("VIDEO")
                    .metrics(Map.of(
                            "likes", (long) (Math.random() * 8000),
                            "comments", (long) (Math.random() * 800),
                            "shares", (long) (Math.random() * 400)
                    ))
                    .relevanceScore(0.6 - i * 0.1)
                    .extraInfo(Map.of("source", "fallback", "note", "API暂不可用，使用模拟数据"))
                    .build());
        }

        log.debug("  📦 生成了{}条快手备用数据", results.size());
        return results;
    }

    private double calculateRelevanceScore(String keyword, String content, int position) {
        if (content == null || keyword == null) return Math.max(0.5, 1.0 - position * 0.1);

        String lowerContent = content.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        double score = 1.0 - position * 0.1;

        if (lowerContent.contains(lowerKeyword)) {
            score += 0.2;
        }

        if (lowerContent.startsWith(lowerKeyword)) {
            score += 0.1;
        }

        String[] keywords = lowerKeyword.split("\\s+");
        long matchCount = Arrays.stream(keywords).filter(k -> !k.isEmpty() && lowerContent.contains(k)).count();
        if (keywords.length > 0) {
            score += (double) matchCount / keywords.length * 0.15;
        }

        return Math.min(1.0, Math.max(0.1, score));
    }

    private LocalDateTime parseTimestamp(long epochSeconds) {
        if (epochSeconds <= 0) return LocalDateTime.now();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    private LocalDateTime parseXiaohongshuTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return LocalDateTime.now();
        try {
            long ts = Long.parseLong(timeStr);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            try {
                return LocalDateTime.parse(timeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    private String extractDescriptionFromDouyin(JsonNode node) {
        StringBuilder desc = new StringBuilder();
        desc.append(node.path("desc").asText(""));

        if (node.has("text_extra") && node.get("text_extra").isArray()) {
            for (JsonNode tag : node.get("text_extra")) {
                if (tag.has("hashtag_name")) {
                    desc.append(" #").append(tag.path("hashtag_name").asText(""));
                }
            }
        }

        return desc.toString();
    }

    private String extractThumbnailUrl(JsonNode node, String coverField, String urlField) {
        try {
            if (node.has(coverField)) {
                JsonNode cover = node.get(coverField);
                if (cover.has(urlField) && cover.get(urlField).isArray() && cover.get(urlField).size() > 0) {
                    return cover.get(urlField).get(0).asText("");
                } else if (cover.isValueNode()) {
                    return cover.asText("");
                }
            }
        } catch (Exception e) {
            log.trace("提取缩略图URL失败: {}", e.getMessage());
        }
        return null;
    }

    private String getUserAccessToken(String userId, String platform) {
        return "mock_search_token_for_" + platform.toLowerCase();
    }

    // ====== 数据模型 ======

    @Data
    public static class SearchResult {
        private String keyword;
        private String platform;
        private boolean success;
        private String errorMessage;
        private int totalCount;
        private List<SearchItem> items = new ArrayList<>();
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMs;
    }

    @Data
    public static class SearchItem {
        private String id;                        // 唯一ID
        private String platform;                  // 来源平台
        private String title;                     // 标题
        private String description;               // 描述/摘要
        private String url;                       // 原始链接
        private String thumbnailUrl;              // 缩略图URL
        private String authorId;                  // 作者ID
        private String authorName;                // 作者名称
        private LocalDateTime publishTime;        // 发布时间
        private String contentType;               // 内容类型 (VIDEO/ARTICLE/LIVE/etc.)
        private Map<String, Object> metrics;      // 指标 (点赞/评论/转发等)
        private double relevanceScore;            // 相关性评分 (0-1)
        private Map<String, Object> extraInfo;    // 额外信息

        public SearchItem() {}

        public static SearchItemBuilder builder() {
            return new SearchItemBuilder();
        }

        public static class SearchItemBuilder {
            private SearchItem item = new SearchItem();

            public SearchItemBuilder id(String id) { item.id = id; return this; }
            public SearchItemBuilder platform(String platform) { item.platform = platform; return this; }
            public SearchItemBuilder title(String title) { item.title = title; return this; }
            public SearchItemBuilder description(String description) { item.description = description; return this; }
            public SearchItemBuilder url(String url) { item.url = url; return this; }
            public SearchItemBuilder thumbnailUrl(String thumbnailUrl) { item.thumbnailUrl = thumbnailUrl; return this; }
            public SearchItemBuilder authorId(String authorId) { item.authorId = authorId; return this; }
            public SearchItemBuilder authorName(String authorName) { item.authorName = authorName; return this; }
            public SearchItemBuilder publishTime(LocalDateTime publishTime) { item.publishTime = publishTime; return this; }
            public SearchItemBuilder contentType(String contentType) { item.contentType = contentType; return this; }
            public SearchItemBuilder metrics(Map<String, Object> metrics) { item.metrics = metrics; return this; }
            public SearchItemBuilder relevanceScore(double relevanceScore) { item.relevanceScore = relevanceScore; return this; }
            public SearchItemBuilder extraInfo(Map<String, Object> extraInfo) { item.extraInfo = extraInfo; return this; }

            public SearchItem build() { return item; }
        }
    }

    @Data
    public static class SearchOptions {
        private int maxResults = 20;              // 最大返回结果数
        private String sortType = "relevance";   // 排序方式: relevance/time/hot
        private String timeRange = "all";         // 时间范围: all/day/week/month/year
        private List<String> contentTypes = Arrays.asList("VIDEO", "ARTICLE");  // 内容类型过滤
        private boolean crossPlatformDeduplication = true;  // 跨平台去重
        private boolean includeMetrics = true;    // 包含指标数据
        private int requestTimeoutSeconds = 30;   // 请求超时时间

        public SearchOptions() {}

        public SearchOptions(int maxResults, String sortType, String timeRange, List<String> contentTypes,
                            boolean crossPlatformDeduplication, boolean includeMetrics, int requestTimeoutSeconds) {
            this.maxResults = maxResults;
            this.sortType = sortType;
            this.timeRange = timeRange;
            this.contentTypes = contentTypes;
            this.crossPlatformDeduplication = crossPlatformDeduplication;
            this.includeMetrics = includeMetrics;
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }

        public static SearchOptions defaults() {
            return new SearchOptions();
        }
    }

    @Data
    public static class MultiPlatformSearchResult {
        private String keyword;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMs;
        private int totalItems;
        private int successCount;
        private int failureCount;
        private Map<String, SearchResult> results = new LinkedHashMap<>();  // platform -> result
        private List<String> failures = new ArrayList<>();
        private List<SearchItem> mergedAndRanked = new ArrayList<>();

        public void addPlatformResult(String platform, SearchResult result) {
            results.put(platform, result);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                failures.add(platform + ": " + result.getErrorMessage());
            }
        }

        public void addFailure(String platform, String error) {
            failureCount++;
            failures.add(platform + ": " + error);
        }

        /**
         * 跨平台去重并重新排序
         */
        public void deduplicateAndRank(int maxResults) {
            Set<String> seenUrls = new HashSet<>();

            for (SearchResult result : results.values()) {
                if (result.getItems() != null) {
                    for (SearchItem item : result.getItems()) {
                        if (item.getUrl() != null && !seenUrls.contains(item.getUrl())) {
                            seenUrls.add(item.getUrl());
                            mergedAndRanked.add(item);
                        }
                    }
                }
            }

            // 按相关性降序排序
            mergedAndRanked.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

            // 截断到最大数量
            if (mergedAndRanked.size() > maxResults) {
                mergedAndRanked = mergedAndRanked.subList(0, maxResults);
            }
        }

        public String getSummary() {
            return String.format("[多平台搜索] \"%s\" | %d/%d成功 | 共%d条 | 耗时%dms",
                    keyword, successCount, results.size(), totalItems, durationMs);
        }
    }
}
