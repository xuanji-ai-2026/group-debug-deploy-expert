package com.beijixing.bxlead.enums;

import lombok.Getter;

/**
 * 商机等级枚举
 * @author 朱怡
 * @since 1.0.0
 */
@Getter
public enum LeadLevel {
    
    A("A", "A级-高意向", 90),
    B("B", "B级-中意向", 70),
    C("C", "C级-低意向", 50),
    D("D", "D级-潜在客户", 30);
    
    private final String code;
    private final String desc;
    private final Integer minScore;
    
    LeadLevel(String code, String desc, Integer minScore) {
        this.code = code;
        this.desc = desc;
        this.minScore = minScore;
    }
    
    public static LeadLevel getByScore(Integer score) {
        if (score >= 90) return A;
        if (score >= 70) return B;
        if (score >= 50) return C;
        return D;
    }
    
    public static LeadLevel getByCode(String code) {
        for (LeadLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}