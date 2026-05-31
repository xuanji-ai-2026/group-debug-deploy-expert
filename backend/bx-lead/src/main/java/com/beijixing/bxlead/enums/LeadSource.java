package com.beijixing.bxlead.enums;

import lombok.Getter;

/**
 * 商机来源枚举
 * @author 朱怡
 * @since 1.0.0
 */
@Getter
public enum LeadSource {
    
    INTERCEPT("INTERCEPT", "同业截客"),
    WEBSITE("WEBSITE", "官网注册"),
    PHONE("PHONE", "电话咨询"),
    REFERRAL("REFERRAL", "客户推荐"),
    EXHIBITION("EXHIBITION", "展会活动"),
    SOCIAL("SOCIAL", "社交媒体"),
    AD("AD", "广告投放"),
    OTHER("OTHER", "其他");
    
    private final String code;
    private final String desc;
    
    LeadSource(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}