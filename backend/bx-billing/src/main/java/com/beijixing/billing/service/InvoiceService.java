package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.InvoiceRequestDTO;
import com.beijixing.billing.entity.BillingOrder;
import com.beijixing.billing.entity.InvoiceRequest;
import com.beijixing.billing.mapper.BillingOrderMapper;
import com.beijixing.billing.mapper.InvoiceRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 发票服务
 * BL-008: 发票申请
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    
    private final InvoiceRequestMapper invoiceRequestMapper;
    private final BillingOrderMapper orderMapper;
    
    private static final DateTimeFormatter REQUEST_NO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * 申请发票
     */
    @Transactional
    public InvoiceRequest submitInvoiceRequest(Long tenantId, InvoiceRequestDTO dto) {
        // 验证订单是否属于该用户且已支付
        String[] orderIdArray = dto.getOrderIds().split(",");
        long totalAmount = 0;
        
        for (String orderIdStr : orderIdArray) {
            Long orderId = Long.valueOf(orderIdStr.trim());
            BillingOrder order = orderMapper.selectById(orderId);
            
            if (order == null || !order.getUserId().equals(dto.getUserId())) {
                throw new IllegalArgumentException("订单不存在或不属于当前用户: " + orderId);
            }
            
            if (order.getStatus() != BillingConstants.ORDER_STATUS_PAID) {
                throw new IllegalArgumentException("订单未支付，无法开具发票: " + orderId);
            }
            
            totalAmount += order.getActualAmount();
        }
        
        InvoiceRequest request = new InvoiceRequest();
        BeanUtils.copyProperties(dto, request);
        request.setTenantId(tenantId);
        request.setRequestNo(generateRequestNo());
        request.setAmount(totalAmount);
        request.setStatus(BillingConstants.INVOICE_STATUS_PENDING);
        
        invoiceRequestMapper.insert(request);
        return request;
    }
    
    /**
     * 获取用户发票申请列表
     */
    public List<InvoiceRequest> getUserInvoices(Long userId) {
        return invoiceRequestMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InvoiceRequest>()
                .eq(InvoiceRequest::getUserId, userId)
                .orderByDesc(InvoiceRequest::getCreateTime)
        );
    }
    
    /**
     * 获取发票详情
     */
    public InvoiceRequest getInvoiceDetail(Long requestId) {
        return invoiceRequestMapper.selectById(requestId);
    }
    
    /**
     * 处理发票（管理员操作）
     */
    @Transactional
    public boolean processInvoice(Long requestId, String invoiceCode, String invoiceNumber, String invoiceUrl) {
        InvoiceRequest request = invoiceRequestMapper.selectById(requestId);
        if (request == null) {
            return false;
        }
        
        request.setStatus(BillingConstants.INVOICE_STATUS_COMPLETED);
        request.setInvoiceCode(invoiceCode);
        request.setInvoiceNumber(invoiceNumber);
        request.setInvoiceUrl(invoiceUrl);
        
        invoiceRequestMapper.updateById(request);
        return true;
    }
    
    /**
     * 驳回发票申请
     */
    @Transactional
    public boolean rejectInvoice(Long requestId, String reason) {
        InvoiceRequest request = invoiceRequestMapper.selectById(requestId);
        if (request == null) {
            return false;
        }
        
        request.setStatus(BillingConstants.INVOICE_STATUS_REJECTED);
        request.setRejectReason(reason);
        
        invoiceRequestMapper.updateById(request);
        return true;
    }
    
    private String generateRequestNo() {
        return "INV" + LocalDateTime.now().format(REQUEST_NO_FORMATTER) + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
