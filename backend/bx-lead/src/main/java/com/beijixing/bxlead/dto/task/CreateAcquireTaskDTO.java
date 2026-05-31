package com.beijixing.bxlead.dto.task;

import lombok.Data;

import java.util.List;

@Data
public class CreateAcquireTaskDTO {
    private String name;
    private List<String> platforms;
    private List<String> keywords;
    private String startTime;
    private String endTime;
}
