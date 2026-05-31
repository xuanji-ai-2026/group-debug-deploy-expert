package com.beijixing.billing.controller;

import com.beijixing.billing.dto.*;
import com.beijixing.billing.entity.BillingOrder;
import com.beijixing.billing.service.AlipayService;
import com.beijixing.billing.service.BillingOrderService;
import com.beijixing.billing.service.WechatPayService;
import com.beijixing.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 订单与支付控制器
 * BL-002: 在线充值
 * BL-006: 充值优惠规则
 * BL-007: 订单管理
 */
@RestController
@RequestMapping("/billing/order")
@RequiredArgsConstructor
public class BillingOrderController {
    
    private final BillingOrderService orderService;
    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;
    
    /**
     * 创建充值订单
     */
    @PostMapping("/recharge")
    public Result<BillingOrderDTO> createRechargeOrder(
            @RequestParam Long tenantId,
            @Valid @RequestBody RechargeRequestDTO request) {
        BillingOrderDTO order = orderService.createRechargeOrder(tenantId, request);
        return Result.success(order);
    }
    
    /**
     * 获取充值优惠
     */
    @GetMapping("/bonus")
    public Result<Long> calculateBonus(@RequestParam Long amount) {
        Long bonus = orderService.calculateBonus(amount);
        return Result.success(bonus);
    }
    
    /**
     * 获取支付宝支付二维码
     */
    @GetMapping("/{orderNo}/alipay-qr")
    public Result<String> getAlipayQrCode(@PathVariable String orderNo) {
        BillingOrderDTO order = orderService.getOrderByNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        BillingOrder entity = new BillingOrder();
        entity.setOrderNo(order.getOrderNo());
        entity.setAmount(order.getAmount());
        entity.setDescription(order.getDescription());
        String qrCode = alipayService.createQrCodePayment(entity);
        return qrCode != null ? Result.success(qrCode) : Result.error("创建支付失败");
    }
    
    /**
     * 获取微信支付二维码
     */
    @GetMapping("/{orderNo}/wechatpay-qr")
    public Result<String> getWechatPayQrCode(@PathVariable String orderNo) {
        BillingOrderDTO order = orderService.getOrderByNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        BillingOrder entity = new BillingOrder();
        entity.setOrderNo(order.getOrderNo());
        entity.setAmount(order.getAmount());
        entity.setDescription(order.getDescription());
        String qrCode = wechatPayService.createNativePayment(entity);
        return qrCode != null ? Result.success(qrCode) : Result.error("创建支付失败");
    }
    
    /**
     * 查询订单状态
     */
    @GetMapping("/{orderNo}")
    public Result<BillingOrderDTO> getOrder(@PathVariable String orderNo) {
        BillingOrderDTO order = orderService.getOrderByNo(orderNo);
        return order != null ? Result.success(order) : Result.error("订单不存在");
    }
    
    /**
     * 获取用户订单列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<BillingOrderDTO>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer orderType) {
        List<BillingOrderDTO> orders = orderService.getUserOrders(userId, orderType);
        return Result.success(orders);
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        boolean success = orderService.cancelOrder(orderNo);
        return success ? Result.success() : Result.error("取消订单失败");
    }
}
