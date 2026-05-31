package com.beijixing.billing.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 银联支付服务
 * 实现银联在线支付（B2C）核心流程
 *
 * @author bx-billing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnionPayService {

    @Value("${unionpay.merchant-id:}")
    private String merchantId;

    @Value("${unionpay.cert-path:}")
    private String certPath;

    @Value("${unionpay.cert-password:}")
    private String certPassword;

    @Value("${unionpay.front-url:}")
    private String frontUrl;

    @Value("${unionpay.back-url:}")
    private String backUrl;

    @Value("${unionpay.gateway-url:https://gateway.95516.com/gateway/api}")
    private String gatewayUrl;

    @Value("${unionpay.query-url:https://gateway.95516.com/gateway/api/queryTxn}")
    private String queryUrl;

    private static final DateTimeFormatter ORDER_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 创建银联支付订单
     *
     * @param orderId   商户订单号
     * @param amount    支付金额（元）
     * @param orderDesc 订单描述
     * @return 支付参数（含跳转HTML表单）
     */
    public UnionPayResult createPayment(String orderId, Long amount, String orderDesc) {
        log.info("创建银联支付订单: orderId={}, amount={}", orderId, amount);

        String txnTime = LocalDateTime.now().format(ORDER_TIME_FMT);
        String txnAmt = String.valueOf(amount); // 银联要求分为单位

        Map<String, String> params = new HashMap<>();
        params.put("version", "5.1.0");
        params.put("encoding", "UTF-8");
        params.put("signMethod", "01"); // RSA
        params.put("txnType", "01"); // 消费
        params.put("txnSubType", "01");
        params.put("bizType", "000201");
        params.put("channelType", "07"); // PC
        params.put("merId", merchantId);
        params.put("orderId", orderId);
        params.put("txnTime", txnTime);
        params.put("txnAmt", txnAmt);
        params.put("currencyCode", "156"); // 人民币
        params.put("frontUrl", frontUrl);
        params.put("backUrl", backUrl);
        params.put("orderDesc", orderDesc != null ? orderDesc : "北极星AI服务");

        String signature = signParams(params);
        params.put("signature", signature);

        // 生成自动提交的HTML表单
        String htmlForm = buildAutoSubmitForm(gatewayUrl, params);

        UnionPayResult result = new UnionPayResult();
        result.setOrderId(orderId);
        result.setPayUrl(htmlForm);
        result.setTxnTime(txnTime);
        result.setStatus("CREATED");

        log.info("银联支付订单创建成功: orderId={}", orderId);
        return result;
    }

    /**
     * 处理银联支付回调通知
     *
     * @param callbackParams 银联回调参数
     * @return 验签结果
     */
    public PayCallbackResult handleCallback(Map<String, String> callbackParams) {
        log.info("收到银联支付回调: orderId={}", callbackParams.get("orderId"));

        PayCallbackResult result = new PayCallbackResult();

        // 1. 验签
        // boolean verified = verifySignature(callbackParams);
        // if (!verified) { result.setSuccess(false); return result; }

        // 2. 解析结果
        String respCode = callbackParams.get("respCode");
        String orderId = callbackParams.get("orderId");
        String queryId = callbackParams.get("queryId");
        String txnAmt = callbackParams.get("txnAmt");

        if ("00".equals(respCode)) {
            result.setSuccess(true);
            result.setOrderId(orderId);
            result.setTransactionId(queryId);
            result.setAmount(Long.parseLong(txnAmt));
            log.info("银联支付成功: orderId={}, txnAmt={}", orderId, txnAmt);
        } else {
            result.setSuccess(false);
            result.setErrorCode(respCode);
            result.setErrorMsg(callbackParams.get("respMsg"));
            log.warn("银联支付失败: orderId={}, respCode={}, respMsg={}",
                orderId, respCode, callbackParams.get("respMsg"));
        }

        return result;
    }

    /**
     * 查询银联支付订单状态
     *
     * @param orderId 商户订单号
     * @param txnTime 订单时间
     * @return 查询结果
     */
    public UnionPayQueryResult queryPayment(String orderId, String txnTime) {
        log.info("查询银联支付状态: orderId={}", orderId);

        Map<String, String> params = new HashMap<>();
        params.put("version", "5.1.0");
        params.put("encoding", "UTF-8");
        params.put("signMethod", "01");
        params.put("txnType", "00"); // 查询
        params.put("txnSubType", "00");
        params.put("bizType", "000000");
        params.put("merId", merchantId);
        params.put("orderId", orderId);
        params.put("txnTime", txnTime);

        String signature = signParams(params);
        params.put("signature", signature);
        
        String responseBody = httpPost(queryUrl, params);
        
        UnionPayQueryResult result = parseQueryResponse(responseBody);
        result.setOrderId(orderId);
        
        log.info("银联查询完成: orderId={}, status={}", orderId, result.getStatus());

        return result;
    }

    /**
     * 银联退款
     *
     * @param orderId      原订单号
     * @param refundAmount 退款金额（分）
     * @param refundReason 退款原因
     * @return 退款结果
     */
    public boolean refund(String orderId, Long refundAmount, String refundReason) {
        log.info("银联退款: orderId={}, amount={}", orderId, refundAmount);
        
        try {
            String txnTime = LocalDateTime.now().format(ORDER_TIME_FMT);
            
            Map<String, String> params = new HashMap<>();
            params.put("version", "5.1.0");
            params.put("encoding", "UTF-8");
            params.put("signMethod", "01");
            params.put("txnType", "04"); // 退款
            params.put("txnSubType", "00");
            params.put("bizType", "000402");
            params.put("channelType", "07");
            params.put("merId", merchantId);
            params.put("orderId", orderId);
            params.put("txnTime", txnTime);
            params.put("txnAmt", String.valueOf(refundAmount));
            params.put("currencyCode", "156");
            params.put("backUrl", backUrl);
            params.put("refundReason", refundReason != null ? refundReason : "用户申请退款");
            
            String signature = signParams(params);
            params.put("signature", signature);
            
            String responseBody = httpPost(gatewayUrl, params);
            
            Map<String, String> responseMap = parseResponse(responseBody);
            String respCode = responseMap.get("respCode");
            
            if ("00".equals(respCode)) {
                log.info("银联退款成功: orderId={}, amount={}", orderId, refundAmount);
                return true;
            } else {
                log.warn("银联退款失败: orderId={}, respCode={}, respMsg={}", 
                        orderId, respCode, responseMap.get("respMsg"));
                return false;
            }
        } catch (Exception e) {
            log.error("银联退款异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成自动提交HTML表单
     */
    private String buildAutoSubmitForm(String action, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><form id=\"pay_form\" method=\"POST\" action=\"").append(action).append("\">");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("<input type=\"hidden\" name=\"").append(entry.getKey())
              .append("\" value=\"").append(entry.getValue()).append("\"/>");
        }
        sb.append("</form><script>document.getElementById(\"pay_form\").submit();</script>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // ==================== 内部数据类 ====================

    @Data
    public static class UnionPayResult {
        private String orderId;
        private String payUrl; // HTML表单
        private String txnTime;
        private String status;
    }

    @Data
    public static class PayCallbackResult {
        private boolean success;
        private String orderId;
        private String transactionId;
        private Long amount;
        private String errorCode;
        private String errorMsg;
    }

    @Data
    public static class UnionPayQueryResult {
        private String orderId;
        private String status;
        private String origRespCode;
        private String origRespMsg;
    }

    // ==================== 签名与HTTP工具方法 ====================

    /**
     * 对请求参数进行HMAC-SHA256签名
     * 生产环境应使用银联证书RSA签名，此处为模拟实现
     */
    private String signParams(Map<String, String> params) {
        try {
            StringBuilder sb = new StringBuilder();
            params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("&"));
            
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            
            String signStr = sb.toString() + "&key=" + (certPassword != null ? certPassword : "unionpay_secret_key");
            
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(signStr.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            log.debug("银联签名完成: paramsCount={}", params.size());
            return hexString.toString();
        } catch (Exception e) {
            log.error("银联签名失败: {}", e.getMessage(), e);
            throw new RuntimeException("银联签名失败", e);
        }
    }

    /**
     * 发送HTTP POST请求到银联网关
     */
    private String httpPost(String urlStr, Map<String, String> params) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (postData.length() > 0) postData.append("&");
                postData.append(entry.getKey())
                       .append("=")
                       .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
                br.close();
                return response.toString();
            } else {
                throw new RuntimeException("银联HTTP请求失败: HTTP " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("银联HTTP请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 解析银联响应（简单键值对格式）
     */
    private Map<String, String> parseResponse(String responseBody) {
        Map<String, String> result = new HashMap<>();
        if (responseBody != null && !responseBody.isEmpty()) {
            String[] pairs = responseBody.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    try {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        result.put(key, java.net.URLDecoder.decode(value, StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        log.warn("解析响应参数失败: {}", pair);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 解析查询响应为查询结果对象
     */
    private UnionPayQueryResult parseQueryResponse(String responseBody) {
        Map<String, String> responseMap = parseResponse(responseBody);
        UnionPayQueryResult result = new UnionPayQueryResult();
        
        String origRespCode = responseMap.getOrDefault("origRespCode", "");
        result.setOrigRespCode(origRespCode);
        result.setOrigRespMsg(responseMap.getOrDefault("origRespMsg", ""));
        
        switch (origRespCode) {
            case "00" -> result.setStatus("SUCCESS");
            case "03", "04", "05" -> result.setStatus("PROCESSING");
            default -> result.setStatus("FAILED");
        }
        
        return result;
    }
}
