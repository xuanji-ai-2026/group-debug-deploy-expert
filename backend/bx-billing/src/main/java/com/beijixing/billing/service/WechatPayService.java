package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.entity.BillingOrder;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * 微信支付服务
 * BL-002: 在线充值（微信/支付宝支付集成）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayService {
    
    @Value("${wechatpay.merchant-id:}")
    private String merchantId;
    
    @Value("${wechatpay.private-key-path:}")
    private String privateKeyPath;
    
    @Value("${wechatpay.merchant-serial-number:}")
    private String merchantSerialNumber;
    
    @Value("${wechatpay.api-v3-key:}")
    private String apiV3Key;
    
    @Value("${wechatpay.notify-url:}")
    private String notifyUrl;
    
    private NativePayService nativePayService;
    private final BillingOrderService orderService;
    
    @PostConstruct
    public void init() {
        if (merchantId != null && !merchantId.isEmpty()) {
            Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
            
            nativePayService = new NativePayService.Builder().config(config).build();
        }
    }
    
    /**
     * 创建微信Native支付
     */
    public String createNativePayment(BillingOrder order) {
        if (nativePayService == null) {
            log.error("WechatPay service not initialized");
            return null;
        }
        
        PrepayRequest request = new PrepayRequest();
        request.setMchid(merchantId);
        request.setOutTradeNo(order.getOrderNo());
        request.setAppid("your-app-id"); // 需要从配置获取
        request.setDescription(order.getDescription());
        request.setNotifyUrl(notifyUrl);
        request.setAmount(buildAmount(order.getAmount()));
        
        try {
            PrepayResponse response = nativePayService.prepay(request);
            log.info("WechatPay QR code created for order: {}", order.getOrderNo());
            return response.getCodeUrl();
        } catch (Exception e) {
            log.error("WechatPay create payment failed", e);
            return null;
        }
    }
    
    /**
     * 查询订单状态
     */
    public boolean queryOrderStatus(String orderNo) {
        if (nativePayService == null) {
            return false;
        }
        
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(merchantId);
        request.setOutTradeNo(orderNo);
        
        try {
            Transaction transaction = nativePayService.queryOrderByOutTradeNo(request);
            if (transaction != null && "SUCCESS".equals(transaction.getTradeState().name())) {
                orderService.handlePaymentCallback(orderNo, transaction.getTransactionId(),
                    BillingConstants.PAY_TYPE_WECHAT);
                return true;
            }
        } catch (Exception e) {
            log.error("WechatPay query exception", e);
        }
        return false;
    }
    
    private Amount buildAmount(long amount) {
        Amount a = new Amount();
        a.setTotal((int) amount);
        a.setCurrency("CNY");
        return a;
    }
}
