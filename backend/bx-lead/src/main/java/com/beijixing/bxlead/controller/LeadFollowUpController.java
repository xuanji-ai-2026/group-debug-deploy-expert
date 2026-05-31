package com.beijixing.bxlead.controller;

import com.beijixing.bxlead.service.LeadFollowUpService;
import com.beijixing.bxlead.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商机跟进记录控制器
 */
@RestController
@RequestMapping("/lead/followup")
@RequiredArgsConstructor
public class LeadFollowUpController {
    
    private final LeadFollowUpService leadFollowUpService;
    
    /**
     * 查询商机跟进记录列表
     */
    @GetMapping("/list/{leadId}")
    public Result<?> list(@PathVariable Long leadId) {
        return Result.success(leadFollowUpService.getFollowUpList(leadId));
    }
    
    /**
     * 获取跟进详情
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(leadFollowUpService.getDetail(id));
    }
}
