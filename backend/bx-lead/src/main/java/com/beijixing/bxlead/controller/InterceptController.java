package com.beijixing.bxlead.controller;

import com.beijixing.bxlead.vo.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 商机拦截控制器
 */
@RestController
@RequestMapping("/lead/intercept")
public class InterceptController {
    
    /**
     * 获取拦截列表
     */
    @GetMapping("/list")
    public Result<?> list() {
        return Result.success("拦截功能开发中");
    }
    
    /**
     * 添加拦截来源
     */
    @PostMapping
    public Result<?> add(@RequestParam String keyword, @RequestParam(required = false) String reason) {
        return Result.success("添加成功");
    }
    
    /**
     * 移除拦截来源
     */
    @DeleteMapping("/{id}")
    public Result<?> remove(@PathVariable Long id) {
        return Result.success("移除成功");
    }
}
