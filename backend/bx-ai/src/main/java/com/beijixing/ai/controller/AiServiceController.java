package com.beijixing.ai.controller;

import com.beijixing.ai.model.*;
import com.beijixing.ai.service.AiCoreService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI服务API控制器
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@RestController
@RequestMapping("/v1")
public class AiServiceController {
    
    @Autowired
    private AiCoreService aiCoreService;
    
    /**
     * 文案生成API
     * 响应时间: ≤30秒
     */
    @PostMapping("/text/generate")
    public ApiResponse<TextGenerationResponse> generateText(
            @Valid @RequestBody TextGenerationRequest request) {
        if (request == null) {
            return ApiResponse.error(400, "请求参数不能为空");
        }
        log.info("收到文案生成请求:userId={}", request.getUserId());
        
        try {
            TextGenerationResponse response = aiCoreService.generateContent(request);
            return ApiResponse.success(response, "文案生成成功");
        } catch (Exception e) {
            log.error("文案生成失败", e);
            return ApiResponse.error(500, "文案生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量文案生成API
     */
    @PostMapping("/text/batch-generate")
    public ApiResponse<List<TextGenerationResponse>> batchGenerateText(
            @Valid @RequestBody List<TextGenerationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ApiResponse.error(400, "请求参数不能为空");
        }
        log.info("收到批量文案生成请求,数量:{}" , requests.size());
        
        try {
            List<TextGenerationResponse> responses = aiCoreService.batchGenerateContent(requests);
            return ApiResponse.success(responses, "批量文案生成成功");
        } catch (Exception e) {
            log.error("批量文案生成失败", e);
            return ApiResponse.error(500, "批量文案生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片生成API
     * 响应时间: ≤60秒
     */
    @PostMapping("/image/generate")
    public ApiResponse<ImageGenerationResponse> generateImage(
            @Valid @RequestBody ImageGenerationRequest request) {
        if (request == null) {
            return ApiResponse.error(400, "请求参数不能为空");
        }
        log.info("收到图片生成请求:userId={}", request.getUserId());
        
        try {
            ImageGenerationResponse response = aiCoreService.generateImage(request);
            return ApiResponse.success(response, "图片生成成功");
        } catch (Exception e) {
            log.error("图片生成失败", e);
            return ApiResponse.error(500, "图片生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 语音识别API (ASR)
     */
    @PostMapping("/speech/recognize")
    public ApiResponse<SpeechRecognitionResponse> recognizeSpeech(
            @Valid @RequestBody SpeechRecognitionRequest request) {
        if (request == null) {
            return ApiResponse.error(400, "请求参数不能为空");
        }
        log.info("收到语音识别请求:userId={}", request.getUserId());
        
        try {
            SpeechRecognitionResponse response = aiCoreService.recognizeSpeech(request);
            return ApiResponse.success(response, "语音识别成功");
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return ApiResponse.error(500, "语音识别失败: " + e.getMessage());
        }
    }
    
    /**
     * 语音合成API (TTS)
     */
    @PostMapping("/speech/synthesize")
    public ApiResponse<SpeechSynthesisResponse> synthesizeSpeech(
            @Valid @RequestBody SpeechSynthesisRequest request) {
        if (request == null) {
            return ApiResponse.error(400, "请求参数不能为空");
        }
        log.info("收到语音合成请求:userId={}", request.getUserId());
        
        try {
            SpeechSynthesisResponse response = aiCoreService.synthesizeSpeech(request);
            return ApiResponse.success(response, "语音合成成功");
        } catch (Exception e) {
            log.error("语音合成失败", e);
            return ApiResponse.error(500, "语音合成失败: " + e.getMessage());
        }
    }
    
    /**
     * 意图识别API
     */
    @PostMapping("/intent/recognize")
    public ApiResponse<IntentRecognitionResponse> recognizeIntent(
            @Valid @RequestBody IntentRecognitionRequest request) {
        if (request == null) {
            return ApiResponse.error(400, "请求参数不能为空");
        }
        log.info("收到意图识别请求:userId={}", request.getUserId());
        
        try {
            IntentRecognitionResponse response = aiCoreService.recognizeIntent(request);
            return ApiResponse.success(response, "意图识别成功");
        } catch (Exception e) {
            log.error("意图识别失败", e);
            return ApiResponse.error(500, "意图识别失败: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK", "AI服务正常运行");
    }
}
