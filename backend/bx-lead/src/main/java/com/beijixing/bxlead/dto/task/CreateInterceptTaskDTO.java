package com.beijixing.bxlead.dto.task;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateInterceptTaskDTO {
    private String name;
    private String targetPlatform;
    private String targetType;
    private List<String> keywords;
    private List<String> competitorAccounts;
    private Map<String, Object> filterRules;
    private Integer dailyLimit = 100;
    private String startTime;
}
