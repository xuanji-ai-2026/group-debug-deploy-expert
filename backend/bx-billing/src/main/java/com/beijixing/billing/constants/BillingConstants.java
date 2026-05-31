package com.beijixing.billing.constants;

/**
 * 计费相关常量
 */
public class BillingConstants {
    
    // Token计算: 1 Token = 10积分 (分)
    public static final int TOKEN_UNIT_PRICE = 10;
    
    // 资源占用费: 每分钟 1积分 (分)
    public static final int RESOURCE_USAGE_FEE_PER_MIN = 1;
    
    // 账户状态
    public static final int ACCOUNT_STATUS_ACTIVE = 1;
    public static final int ACCOUNT_STATUS_FROZEN = 2;
    public static final int ACCOUNT_STATUS_DISABLED = 0;
    
    // 订单状态
    public static final int ORDER_STATUS_PENDING = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_CANCELLED = 2;
    public static final int ORDER_STATUS_REFUNDED = 3;
    
    // 支付类型
    public static final int PAY_TYPE_WECHAT = 1;
    public static final int PAY_TYPE_ALIPAY = 2;
    public static final int PAY_TYPE_BALANCE = 3;
    
    // 订单类型
    public static final int ORDER_TYPE_RECHARGE = 1;
    public static final int ORDER_TYPE_CONSUMPTION = 2;
    public static final int ORDER_TYPE_PACKAGE = 3;
    public static final int ORDER_TYPE_REFUND = 4;
    
    // 套餐类型
    public static final String PACKAGE_BASIC = "basic";
    public static final String PACKAGE_ADVANCED = "advanced";
    public static final String PACKAGE_ANNUAL = "annual";
    public static final String PACKAGE_LIFETIME = "lifetime";
    
    // 发票状态
    public static final int INVOICE_STATUS_PENDING = 0;
    public static final int INVOICE_STATUS_PROCESSING = 1;
    public static final int INVOICE_STATUS_COMPLETED = 2;
    public static final int INVOICE_STATUS_REJECTED = 3;
    
    // Redis Key前缀
    public static final String REDIS_KEY_ACCOUNT = "billing:account:";
    public static final String REDIS_KEY_ORDER = "billing:order:";
    public static final String REDIS_KEY_LOCK = "billing:lock:";
    
    // 充值优惠阶梯
    public static final int[][] RECHARGE_BONUS_TIERS = {
        {10000, 500},      // 充100送5
        {50000, 5000},     // 充500送50
        {100000, 15000},   // 充1000送150
        {500000, 100000}   // 充5000送1000
    };
}
