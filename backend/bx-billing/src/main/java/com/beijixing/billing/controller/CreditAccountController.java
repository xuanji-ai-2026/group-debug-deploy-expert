package com.beijixing.billing.controller;

import com.beijixing.billing.dto.*;
import com.beijixing.billing.service.CreditAccountService;
import com.beijixing.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 积分账户控制器
 * BL-001: 积分账户管理（账户余额、冻结、解冻）
 */
@RestController
@RequestMapping("/billing/account")
@RequiredArgsConstructor
public class CreditAccountController {
    
    private final CreditAccountService creditAccountService;
    
    /**
     * 获取账户信息
     */
    @GetMapping("/{tenantId}/{userId}")
    public Result<CreditAccountDTO> getAccount(
            @PathVariable Long tenantId,
            @PathVariable Long userId) {
        CreditAccountDTO account = creditAccountService.getOrCreateAccount(tenantId, userId);
        return Result.success(account);
    }
    
    /**
     * 冻结账户
     */
    @PostMapping("/{accountId}/freeze")
    public Result<Void> freezeAccount(@PathVariable Long accountId) {
        boolean success = creditAccountService.freezeAccount(accountId);
        return success ? Result.success() : Result.error("冻结账户失败");
    }
    
    /**
     * 解冻账户
     */
    @PostMapping("/{accountId}/unfreeze")
    public Result<Void> unfreezeAccount(@PathVariable Long accountId) {
        boolean success = creditAccountService.unfreezeAccount(accountId);
        return success ? Result.success() : Result.error("解冻账户失败");
    }
}
