package com.beijixing.bxlead.service;

/**
 * AI意向分析服务接口
 * @author 朱怡
 * @since 1.0.0
 */
public interface AiIntentAnalysisService {
    
    /**
     * 分析内容意向评分
     * @param content 内容文本
     * @return 意向评分 (0-100)
     */
    Integer analyzeIntentScore(String content);
    
    /**
     * 提取关键信息
     * @param content 内容文本
     * @return JSON格式关键信息
     */
    String extractKeyInfo(String content);
    
    /**
     * 分析竞品提及
     * @param content 内容文本
     * @return 竞品名称，如未提及返回null
     */
    String detectCompetitor(String content);
    
    /**
     * 生成AI分析结果
     * @param content 内容文本
     * @return 分析结果描述
     */
    String generateAnalysisResult(String content);
}