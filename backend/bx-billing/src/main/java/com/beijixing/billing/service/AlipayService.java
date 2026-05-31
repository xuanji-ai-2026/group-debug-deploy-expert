package com.beijixing.billing.service;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.entity.BillingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * 支付宝支付服务
 * BL-002: 在线充值（微信/支付宝支付集成）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {
    
    @Value("${alipay.app-id:}")
    private String appId;
    
    @Value("${alipay.private-key:}")
    private String privateKey;
    
    @Value("${alipay.public-key:}")
    private String alipayPublicKey;
    
    @Value("${alipay.server-url:https://openapi.alipay.com/gateway.do}")
    private String serverUrl;
    
    @Value("${alipay.notify-url:}")
    private String notifyUrl;
    
    private AlipayClient alipayClient;
    private final BillingOrderService orderService;
    
    @PostConstruct
    public void init() {
        if (appId != null && !appId.isEmpty()) {
            alipayClient = new DefaultAlipayClient(
                serverUrl, appId, privateKey, "json", "UTF-8", 
                alipayPublicKey, "RSA2"
            );
        }
    }
    
    /**
     * 创建支付宝扫码支付
     */
    public String createQrCodePayment(BillingOrder order) {
        if (alipayClient == null) {
            log.error("Alipay client not initialized");
            return null;
        }
        
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(notifyUrl);
        
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", String.format("%.2f", order.getAmount() / 100.0));
        bizContent.put("subject", order.getDescription());
        bizContent.put("timeout_express", "30m");
        
        request.setBizContent(bizContent.toString());
        
        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("Alipay QR code created for order: {}", order.getOrderNo());
                return response.getQrCode();
            } else {
                log.error("Alipay create QR code failed: {} - {}", 
                         response.getCode(), response.getMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            log.error("Alipay API exception", e);
            return null;
        }
    }
    
    /**
     * 查询订单支付状态
     */
    public boolean queryOrderStatus(String orderNo) {
        if (alipayClient == null) {
            return false;
        }
        
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());
        
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                // 更新订单状态
                orderService.handlePaymentCallback(orderNo, response.getTradeNo(), 
                    BillingConstants.PAY_TYPE_ALIPAY);
                return true;
            }
        } catch (AlipayApiException e) {
            log.error("Alipay query exception", e);
        }
        return false;
    }
}
