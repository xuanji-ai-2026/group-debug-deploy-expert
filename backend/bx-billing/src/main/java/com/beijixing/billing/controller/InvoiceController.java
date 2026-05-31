package com.beijixing.billing.controller;

import com.beijixing.billing.dto.InvoiceRequestDTO;
import com.beijixing.billing.entity.InvoiceRequest;
import com.beijixing.billing.service.InvoiceService;
import com.beijixing.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 发票控制器
 * BL-008: 发票申请
 */
@RestController
@RequestMapping("/billing/invoice")
@RequiredArgsConstructor
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    /**
     * 申请发票
     */
    @PostMapping("/apply")
    public Result<InvoiceRequest> applyInvoice(
            @RequestParam Long tenantId,
            @Valid @RequestBody InvoiceRequestDTO dto) {
        try {
            InvoiceRequest request = invoiceService.submitInvoiceRequest(tenantId, dto);
            return Result.success(request);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户发票列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<InvoiceRequest>> getUserInvoices(@PathVariable Long userId) {
        List<InvoiceRequest> invoices = invoiceService.getUserInvoices(userId);
        return Result.success(invoices);
    }
    
    /**
     * 获取发票详情
     */
    @GetMapping("/{requestId}")
    public Result<InvoiceRequest> getInvoiceDetail(@PathVariable Long requestId) {
        InvoiceRequest invoice = invoiceService.getInvoiceDetail(requestId);
        return invoice != null ? Result.success(invoice) : Result.error("发票不存在");
    }
}
