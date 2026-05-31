package com.beijixing.social.controller;

import com.beijixing.social.service.PlatformService;
import com.beijixing.social.vo.ApiResponse;
import com.beijixing.social.vo.PlatformVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 平台管理控制器
 */
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    /** 初始化平台数据 */
    @PostMapping("/init")
    public ApiResponse<String> initPlatforms() {
        platformService.initPlatforms();
        return ApiResponse.success("平台初始化完成");
    }

    /** 获取启用的平台列表 */
    @GetMapping("/list")
    public ApiResponse<List<PlatformVO>> listEnabledPlatforms() {
        return ApiResponse.success(platformService.listEnabledPlatforms());
    }

    /** 获取所有平台列表 */
    @GetMapping("/list/all")
    public ApiResponse<List<PlatformVO>> listAllPlatforms() {
        return ApiResponse.success(platformService.listAllPlatforms());
    }

    /** 启用/禁用平台 */
    @PostMapping("/toggle/{id}")
    public ApiResponse<String> togglePlatform(@PathVariable Long id, @RequestParam boolean enabled) {
        boolean result = platformService.togglePlatform(id, enabled);
        return result ? ApiResponse.success(enabled ? "平台已启用" : "平台已禁用")
                      : ApiResponse.fail("操作失败");
    }
}
