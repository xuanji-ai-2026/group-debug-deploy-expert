package com.beijixing.schedule.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.billing.entity.ConsumptionRecord;
import com.beijixing.billing.mapper.ConsumptionRecordMapper;
import com.beijixing.message.dto.SendNotificationRequest;
import com.beijixing.message.enums.NotificationType;
import com.beijixing.message.service.NotificationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI计费任务执行器
 * 负责每日统计AI模型调用量并生成账单
 */
@Slf4j
@Component
public class AiBillingExecutor extends BaseExecutor {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private ConsumptionRecordMapper consumptionRecordMapper;

    @Autowired(required = false)
    private NotificationService notificationService;

    @XxlJob("aiBillingJob")
    public void xxlExecute() {
        String params = XxlJobHelper.getJobParam();
        execute(params);
    }

    @Override
    protected String doExecute(String params) {
        log.info("开始执行AI计费任务");
        XxlJobHelper.log("开始执行AI计费任务");

        LocalDate billingDate = LocalDate.now().minusDays(1);

        Map<String, ModelUsage> usageMap =统计ModelUsage(billingDate);

        List<BillingItem> billingItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<String, ModelUsage> entry : usageMap.entrySet()) {
            ModelUsage usage = entry.getValue();
            BigDecimal amount = calculateAmount(usage);
            totalAmount = totalAmount.add(amount);

            BillingItem item = new BillingItem();
            item.setModelName(entry.getKey());
            item.setCallCount(usage.callCount);
            item.setTokenCount(usage.tokenCount);
            item.setUnitPrice(usage.unitPrice);
            item.setAmount(amount);
            billingItems.add(item);
        }

        String billNo = generateBillNo(billingDate);
        saveBill(billNo, billingDate, billingItems, totalAmount);

        sendBillNotification(billNo, totalAmount, billingItems.size());

        Map<String, Object> result = new HashMap<>();
        result.put("billNo", billNo);
        result.put("billingDate", billingDate.toString());
        result.put("totalAmount", totalAmount);
        result.put("itemCount", billingItems.size());

        return toJson(result);
    }

    @Override
    protected String getJobName() {
        return "AI计费任务";
    }

    @Override
    protected String getJobType() {
        return "ai_billing";
    }

    @Override
    protected String getLockKey(String params) {
        return LocalDate.now().toString();
    }

    @Override
    protected int getTimeoutSeconds() {
        return 600;
    }

    /**
     * 统计模型使用量
     * 从ConsumptionRecord表按日期和模块统计各模型的调用数据
     */
    private Map<String, ModelUsage> 统计ModelUsage(LocalDate date) {
        Map<String, ModelUsage> usageMap = new HashMap<>();

        try {
            if (consumptionRecordMapper == null) {
                log.warn("ConsumptionRecordMapper未注入，跳过AI使用量统计，返回默认值");
                ModelUsage defaultUsage = new ModelUsage();
                defaultUsage.callCount = 0;
                defaultUsage.tokenCount = 0;
                defaultUsage.unitPrice = BigDecimal.ZERO;
                usageMap.put("default", defaultUsage);
                return usageMap;
            }

            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LambdaQueryWrapper<ConsumptionRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(ConsumptionRecord::getCallTime, startOfDay)
                   .le(ConsumptionRecord::getCallTime, endOfDay)
                   .eq(ConsumptionRecord::getDeleted, 0)
                   .isNotNull(ConsumptionRecord::getModule);

            List<ConsumptionRecord> records = consumptionRecordMapper.selectList(wrapper);

            for (ConsumptionRecord record : records) {
                String modelName = record.getModule();
                if (modelName == null || modelName.isEmpty()) {
                    modelName = "unknown";
                }

                ModelUsage usage = usageMap.computeIfAbsent(modelName, k -> {
                    ModelUsage u = new ModelUsage();
                    u.callCount = 0;
                    u.tokenCount = 0;
                    u.unitPrice = new BigDecimal(record.getUnitPrice() != null ? record.getUnitPrice() : 0).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
                    return u;
                });

                usage.callCount++;
                if (record.getTokenCount() != null) {
                    usage.tokenCount += record.getTokenCount();
                }
            }

            if (!usageMap.isEmpty()) {
                log.info("AI使用量统计完成: date={}, modelCount={}, records={}",
                        date, usageMap.size(), records.size());
            } else {
                log.info("指定日期无AI调用记录: date={}", date);
                ModelUsage defaultUsage = new ModelUsage();
                defaultUsage.callCount = 0;
                defaultUsage.tokenCount = 0;
                defaultUsage.unitPrice = BigDecimal.ZERO;
                usageMap.put("default", defaultUsage);
            }
        } catch (Exception e) {
            log.error("统计AI模型使用量异常: date={}", date, e);
            ModelUsage fallback = new ModelUsage();
            fallback.callCount = 0;
            fallback.tokenCount = 0;
            fallback.unitPrice = BigDecimal.ZERO;
            usageMap.put("error_fallback", fallback);
        }

        return usageMap;
    }

    /**
     * 计算费用
     */
    private BigDecimal calculateAmount(ModelUsage usage) {
        return BigDecimal.valueOf(usage.tokenCount).multiply(usage.unitPrice);
    }

    /**
     * 生成账单号
     */
    private String generateBillNo(LocalDate date) {
        return "AI-" + date.toString().replace("-", "") + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * 保存账单到数据库
     * 将账单明细写入bx_ai_bill表，同时记录各模型费用明细
     */
    private void saveBill(String billNo, LocalDate billingDate,
                          List<BillingItem> items, BigDecimal totalAmount) {
        try {
            String insertBillSql = "INSERT INTO bx_ai_bill (bill_no, billing_date, total_amount, item_count, status, create_time) " +
                    "VALUES (?, ?, ?, ?, 'PAID', NOW())";
            jdbcTemplate.update(insertBillSql, billNo, billingDate, totalAmount, items.size());

            for (BillingItem item : items) {
                String insertItemSql = "INSERT INTO bx_ai_bill_item (bill_no, model_name, call_count, token_count, unit_price, amount, create_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW())";
                jdbcTemplate.update(insertItemSql,
                        billNo, item.getModelName(), item.getCallCount(),
                        item.getTokenCount(), item.getUnitPrice(), item.getAmount());
            }

            log.info("AI账单已保存: billNo={}, date={}, totalAmount={}, itemCount={}",
                    billNo, billingDate, totalAmount, items.size());
        } catch (Exception e) {
            log.error("保存AI账单异常: billNo={}", billNo, e);
        }
    }

    /**
     * 发送账单通知
     * 通过NotificationService发送账单通知给管理员
     */
    private void sendBillNotification(String billNo, BigDecimal totalAmount, int itemCount) {
        try {
            if (notificationService == null) {
                log.warn("NotificationService未注入，跳过账单通知发送");
                return;
            }

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .title("AI账单通知")
                    .content(String.format(
                            "AI账单已生成！\n" +
                                    "账单编号: %s\n" +
                                    "账单日期: %s\n" +
                                    "消费项目数: %d\n" +
                                    "总金额: %.2f 元\n\n" +
                                    "请登录系统查看详细账单。",
                            billNo,
                            LocalDate.now().minusDays(1).toString(),
                            itemCount,
                            totalAmount.doubleValue()))
                    .notificationType(NotificationType.POINTS_CHANGE)
                    .tenantId(1L)
                    .businessId(billNo)
                    .businessType("AI_BILLING")
                    .build();

            notificationService.sendNotification(request);
            log.info("AI账单通知已发送: billNo={}, totalAmount={}, itemCount={}",
                    billNo, totalAmount, itemCount);
        } catch (Exception e) {
            log.error("发送AI账单通知异常: billNo={}", billNo, e);
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    static class ModelUsage {
        long callCount;
        long tokenCount;
        BigDecimal unitPrice;
    }

    @Data
    static class BillingItem {
        String modelName;
        long callCount;
        long tokenCount;
        BigDecimal unitPrice;
        BigDecimal amount;
    }
}
