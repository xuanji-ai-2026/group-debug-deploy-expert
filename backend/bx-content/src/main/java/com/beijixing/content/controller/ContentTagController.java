package com.beijixing.content.controller;

import com.beijixing.content.dto.ContentTagDTO;
import com.beijixing.content.service.ContentTagService;
import com.beijixing.content.vo.ContentTagVO;
import com.beijixing.content.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容标签控制器 - CO-006: 内容标签管理
 * @author 胡云 (EMP-CONTENT-001)
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "内容标签", description = "内容标签的增删改查")
public class ContentTagController {

    private final ContentTagService tagService;

    @Operation(summary = "创建标签")
    @PostMapping
    public ResultVO<ContentTagVO> createTag(@Valid @RequestBody ContentTagDTO dto) {
        return ResultVO.success(tagService.createTag(dto));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public ResultVO<ContentTagVO> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @Valid @RequestBody ContentTagDTO dto) {
        dto.setId(id);
        return ResultVO.success(tagService.updateTag(dto));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public ResultVO<Void> deleteTag(
            @Parameter(description = "标签ID") @PathVariable Long id) {
        tagService.deleteTag(id);
        return ResultVO.success();
    }

    @Operation(summary = "获取标签详情")
    @GetMapping("/{id}")
    public ResultVO<ContentTagVO> getTagById(
            @Parameter(description = "标签ID") @PathVariable Long id) {
        return ResultVO.success(tagService.getTagById(id));
    }

    @Operation(summary = "获取所有标签")
    @GetMapping
    public ResultVO<List<ContentTagVO>> listAllTags() {
        return ResultVO.success(tagService.listAllTags());
    }

    @Operation(summary = "搜索标签")
    @GetMapping("/search")
    public ResultVO<List<ContentTagVO>> searchTags(
            @Parameter(description = "关键词") @RequestParam String keyword) {
        return ResultVO.success(tagService.searchTags(keyword));
    }

    @Operation(summary = "获取热门标签")
    @GetMapping("/hot")
    public ResultVO<List<ContentTagVO>> getHotTags(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return ResultVO.success(tagService.getHotTags(limit));
    }

    @Operation(summary = "获取内容的标签列表")
    @GetMapping("/content/{contentId}")
    public ResultVO<List<ContentTagVO>> getTagsByContentId(
            @Parameter(description = "内容ID") @PathVariable Long contentId) {
        return ResultVO.success(tagService.getTagsByContentId(contentId));
    }
}
