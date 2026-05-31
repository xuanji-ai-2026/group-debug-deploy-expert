package com.beijixing.ai.adapter;

import com.beijixing.ai.model.*;

/**
 * AI模型适配器接口
 * 统一不同模型提供商的调用方式
 * 
 * @author 郑武 (EMP-AI-001)
 */
public interface ModelAdapter {
    
    /**
     * 获取适配器类型
     */
    String getType();
    
    /**
     * 文本生成
     */
    TextGenerationResponse generateText(TextGenerationRequest request);
    
    /**
     * 图片生成
     */
    ImageGenerationResponse generateImage(ImageGenerationRequest request);
    
    /**
     * 语音识别
     */
    SpeechRecognitionResponse recognizeSpeech(SpeechRecognitionRequest request);
    
    /**
     * 语音合成
     */
    SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request);
    
    /**
     * 意图识别
     */
    IntentRecognitionResponse recognizeIntent(IntentRecognitionRequest request);
    
    /**
     * 健康检查
     */
    boolean healthCheck();
}
