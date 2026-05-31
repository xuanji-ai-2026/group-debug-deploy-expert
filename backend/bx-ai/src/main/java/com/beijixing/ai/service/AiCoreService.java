package com.beijixing.ai.service;

import com.beijixing.ai.adapter.ModelAdapter;
import com.beijixing.ai.adapter.VolcengineAdapter;
import com.beijixing.ai.adapter.WenxinAdapter;
import com.beijixing.ai.gateway.AiModelGateway;
import com.beijixing.ai.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI核心服务
 * 统一的AI能力入口
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Service
public class AiCoreService {
    
    @Autowired
    private AiModelGateway modelGateway;
    
    @Autowired
    private VolcengineAdapter volcengineAdapter;
    
    @Autowired
    private WenxinAdapter wenxinAdapter;
    
    @Autowired
    private AiLogService logService;
    
    /**
     * 文案生成
     */
    public TextGenerationResponse generateContent(TextGenerationRequest request) {
        log.info("开始文案生成:userId={}, contentType={}", request.getUserId(), request.getContentType());
        
        AiModelGateway.GatewayResult<TextGenerationResponse> result = modelGateway.executeWithGateway(
                request.getUserId(),
                "text-generation",
                request.getIdempotencyKey(),
                provider -> {
                    ModelAdapter adapter = selectAdapter(provider.getType());
                    return adapter.generateText(request);
                }
        );
        
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        
        // 保存日志
        saveLog(request.getUserId(), "text-generation", result);
        
        TextGenerationResponse response = result.getData();
        response.setRequestId(result.getRequestId());
        response.setIsFallback(result.getIsFallback());
        
        return response;
    }
    
    /**
     * 图片生成
     */
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        log.info("开始图片生成:userId={}, size={}", request.getUserId(), request.getSize());
        
        AiModelGateway.GatewayResult<ImageGenerationResponse> result = modelGateway.executeWithGateway(
                request.getUserId(),
                "image-generation",
                request.getIdempotencyKey(),
                provider -> {
                    ModelAdapter adapter = selectAdapter(provider.getType());
                    return adapter.generateImage(request);
                }
        );
        
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        
        saveLog(request.getUserId(), "image-generation", result);
        
        ImageGenerationResponse response = result.getData();
        response.setRequestId(result.getRequestId());
        response.setIsFallback(result.getIsFallback());
        
        return response;
    }
    
    /**
     * 语音识别
     */
    public SpeechRecognitionResponse recognizeSpeech(SpeechRecognitionRequest request) {
        log.info("开始语音识别:userId={}", request.getUserId());
        
        AiModelGateway.GatewayResult<SpeechRecognitionResponse> result = modelGateway.executeWithGateway(
                request.getUserId(),
                "speech-recognition",
                null,
                provider -> {
                    ModelAdapter adapter = selectAdapter(provider.getType());
                    return adapter.recognizeSpeech(request);
                }
        );
        
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        
        saveLog(request.getUserId(), "speech-recognition", result);
        
        SpeechRecognitionResponse response = result.getData();
        response.setRequestId(result.getRequestId());
        
        return response;
    }
    
    /**
     * 语音合成
     */
    public SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request) {
        log.info("开始语音合成:userId={}, textLength={}", request.getUserId(), 
                request.getText() != null ? request.getText().length() : 0);
        
        AiModelGateway.GatewayResult<SpeechSynthesisResponse> result = modelGateway.executeWithGateway(
                request.getUserId(),
                "speech-synthesis",
                null,
                provider -> {
                    ModelAdapter adapter = selectAdapter(provider.getType());
                    return adapter.synthesizeSpeech(request);
                }
        );
        
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        
        saveLog(request.getUserId(), "speech-synthesis", result);
        
        SpeechSynthesisResponse response = result.getData();
        response.setRequestId(result.getRequestId());
        
        return response;
    }
    
    /**
     * 意图识别
     */
    public IntentRecognitionResponse recognizeIntent(IntentRecognitionRequest request) {
        log.info("开始意图识别:userId={}, text={}", request.getUserId(), 
                request.getText() != null ? request.getText().substring(0, Math.min(50, request.getText().length())) + "..." : "");
        
        AiModelGateway.GatewayResult<IntentRecognitionResponse> result = modelGateway.executeWithGateway(
                request.getUserId(),
                "intent-recognition",
                null,
                provider -> {
                    ModelAdapter adapter = selectAdapter(provider.getType());
                    return adapter.recognizeIntent(request);
                }
        );
        
        if (!result.isSuccess()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        
        saveLog(request.getUserId(), "intent-recognition", result);
        
        IntentRecognitionResponse response = result.getData();
        response.setRequestId(result.getRequestId());
        
        return response;
    }
    
    /**
     * 批量文案生成
     */
    public List<TextGenerationResponse> batchGenerateContent(List<TextGenerationRequest> requests) {
        List<TextGenerationResponse> responses = new ArrayList<>();
        
        for (TextGenerationRequest request : requests) {
            try {
                responses.add(generateContent(request));
            } catch (Exception e) {
                log.error("批量生成失败:userId={}", request.getUserId(), e);
                responses.add(null);
            }
        }
        
        return responses;
    }
    
    /**
     * 根据提供商类型选择适配器
     */
    private ModelAdapter selectAdapter(String providerType) {
        return switch (providerType) {
            case "VOLCENGINE" -> volcengineAdapter;
            case "WENXIN" -> wenxinAdapter;
            default -> volcengineAdapter; // 默认使用火山引擎
        };
    }
    
    /**
     * 保存调用日志
     */
    private <T> void saveLog(String userId, String requestType, AiModelGateway.GatewayResult<T> result) {
        try {
            AiModelLog logEntry = AiModelLog.builder()
                    .requestId(result.getRequestId())
                    .userId(userId)
                    .requestType(requestType)
                    .provider(result.getProviderName())
                    //.model(result.getProviderId())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                    .responseTime(result.getResponseTime())
                    .isFallback(result.getIsFallback())
                    .errorMessage(result.getErrorMessage())
                    .build();
            
            logService.saveLog(logEntry);
        } catch (Exception e) {
            log.error("保存调用日志失败", e);
        }
    }
}
