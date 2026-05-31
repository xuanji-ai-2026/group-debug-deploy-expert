package com.beijixing.bxlead.enums;

import lombok.Getter;

/**
 * 商机状态枚举
 * @author 朱怡
 * @since 1.0.0
 */
@Getter
public enum LeadStatus {
    
    NEW("NEW", "新建"),
    FOLLOWING("FOLLOWING", "跟进中"),
    QUOTED("QUOTED", "已报价"),
    NEGOTIATION("NEGOTIATION", "谈判中"),
    WON("WON", "成交"),
    LOST("LOST", "失败");
    
    private final String code;
    private final String desc;
    
    LeadStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static LeadStatus getByCode(String code) {
        for (LeadStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}