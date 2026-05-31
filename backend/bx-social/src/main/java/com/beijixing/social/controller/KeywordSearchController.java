package com.beijixing.social.controller;

import com.beijixing.social.crawl.service.KeywordSearchService;
import com.beijixing.social.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关键词搜索API控制器
 *
 * 提供跨平台关键词搜索功能
 *
 * 核心API:
 * - POST /api/search/keyword - 单平台搜索
 * - POST /api/search/all - 多平台并行搜索
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */
@RestController
@RequestMapping("/search")
@Tag(name = "关键词搜索API", description = "支持抖音/小红书/快手/微博/B站等平台的跨平台搜索")
@Slf4j
public class KeywordSearchController {

    @Autowired
    private KeywordSearchService searchService;

    /**
     * 单平台关键词搜索
     */
    @PostMapping("/keyword")
    @Operation(summary = "单平台搜索", description = "在指定平台搜索关键词")
    public ResponseEntity<ApiResponse<KeywordSearchService.SearchResult>> searchKeyword(
            @RequestBody SearchRequest request) {

        log.info("🔍 [API] 收到搜索请求 | 平台:{} | 关键词:\"{}\"",
                request.getPlatform(), request.getKeyword());

        KeywordSearchService.SearchResult result = searchService.search(
                request.getKeyword(),
                request.getPlatform(),
                request.getUserId(),
                request.getOptions() != null ? request.getOptions() :
                        new KeywordSearchService.SearchOptions()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(400, "SEARCH_ERROR: " + result.getErrorMessage()));
        }
    }

    /**
     * 多平台并行搜索
     */
    @PostMapping("/all")
    @Operation(summary = "多平台并行搜索", description = "同时在多个平台搜索关键词并聚合结果")
    public ResponseEntity<ApiResponse<KeywordSearchService.MultiPlatformSearchResult>> searchAll(
            @RequestBody MultiPlatformSearchRequest request) {

        log.info("🔍 [API] 多平台搜索 | 平台数:{} | 关键词:\"{}\"",
                request.getPlatforms().size(), request.getKeyword());

        KeywordSearchService.MultiPlatformSearchResult multiResult =
                searchService.searchAll(
                        request.getKeyword(),
                        request.getPlatforms(),
                        request.getUserId(),
                        request.getOptions()
                );

        return ResponseEntity.ok(ApiResponse.success(multiResult));
    }

    // ====== 请求数据模型 ======

    @lombok.Data
    public static class SearchRequest {
        private String keyword;                   // 搜索关键词 (必填)
        private String platform;                  // 目标平台代码 (必填)
        private String userId;                    // 用户ID (必填)
        private KeywordSearchService.SearchOptions options;  // 搜索选项 (可选)
    }

    @lombok.Data
    public static class MultiPlatformSearchRequest {
        private String keyword;                   // 搜索关键词 (必填)
        private List<String> platforms;           // 平台代码列表 (必填, 如 ["DOUYIN","XIAOHONGSHU"])
        private String userId;                    // 用户ID (必填)
        private KeywordSearchService.SearchOptions options;  // 搜索选项 (可选)
    }
}
