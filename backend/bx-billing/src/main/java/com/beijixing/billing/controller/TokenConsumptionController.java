package com.beijixing.billing.controller;

import com.beijixing.billing.dto.ConsumptionRecordDTO;
import com.beijixing.billing.dto.TokenConsumptionDTO;
import com.beijixing.billing.service.TokenConsumptionService;
import com.beijixing.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Token消耗控制器
 * BL-003: Token消耗计算
 * BL-004: 消费明细记录
 */
@RestController
@RequestMapping("/billing/consumption")
@RequiredArgsConstructor
public class TokenConsumptionController {
    
    private final TokenConsumptionService consumptionService;
    
    /**
     * 计算消费金额
     */
    @PostMapping("/calculate")
    public Result<Long> calculateCost(@RequestBody TokenConsumptionDTO dto) {
        long cost = consumptionService.calculateConsumptionCost(
            dto.getTokenCount(), 
            dto.getResourceUsageMinutes() != null ? dto.getResourceUsageMinutes() : 0
        );
        return Result.success(cost);
    }
    
    /**
     * 执行Token消费扣费
     */
    @PostMapping("/consume")
    public Result<ConsumptionRecordDTO> consumeToken(@Valid @RequestBody TokenConsumptionDTO dto) {
        try {
            ConsumptionRecordDTO record = consumptionService.consumeToken(dto);
            return Result.success(record);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户消费记录
     */
    @GetMapping("/user/{userId}")
    public Result<List<ConsumptionRecordDTO>> getUserConsumptionRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ConsumptionRecordDTO> records = consumptionService.getUserConsumptionRecords(userId, page, size);
        return Result.success(records);
    }
    
    /**
     * 根据调用ID查询消费记录
     */
    @GetMapping("/call/{callId}")
    public Result<ConsumptionRecordDTO> getConsumptionByCallId(@PathVariable String callId) {
        ConsumptionRecordDTO record = consumptionService.getConsumptionByCallId(callId);
        return record != null ? Result.success(record) : Result.error("记录不存在");
    }
}
