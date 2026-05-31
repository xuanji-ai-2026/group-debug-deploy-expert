package com.beijixing.ai.adapter;

import com.beijixing.ai.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 火山引擎Ark模型适配器
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Component
public class VolcengineAdapter implements ModelAdapter {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${volcengine.api.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;
    
    @Value("${volcengine.api.api-key:}")
    private String apiKey;
    
    private static final String TYPE = "VOLCENGINE";
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public TextGenerationResponse generateText(TextGenerationRequest request) {
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getEndpointForTask("text-generation"));
            
            ArrayNode messages = requestBody.putArray("messages");
            
            // 系统提示词
            if (request.getSystemPrompt() != null) {
                ObjectNode systemMsg = messages.addObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", request.getSystemPrompt());
            }
            
            // 用户提示词
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", buildPrompt(request));
            
            requestBody.put("temperature", request.getTemperature());
            requestBody.put("max_tokens", request.getMaxLength() != null ? request.getMaxLength() : 2000);
            
            // 发送请求
            String response = sendRequest("/chat/completions", requestBody);
            JsonNode responseJson = objectMapper.readTree(response);
            
            // 解析响应
            String generatedText = responseJson.path("choices").get(0).path("message").path("content").asText();
            JsonNode usage = responseJson.path("usage");
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            List<TextGenerationResponse.GeneratedContent> contents = new ArrayList<>();
            contents.add(TextGenerationResponse.GeneratedContent.builder()
                    .index(0)
                    .text(generatedText)
                    .wordCount(generatedText.length())
                    .build());
            
            return TextGenerationResponse.builder()
                    .requestId(requestId)
                    .contents(contents)
                    .model(responseJson.path("model").asText())
                    .tokenUsage(TextGenerationResponse.TokenUsage.builder()
                            .promptTokens(usage.path("prompt_tokens").asInt())
                            .completionTokens(usage.path("completion_tokens").asInt())
                            .totalTokens(usage.path("total_tokens").asInt())
                            .build())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("[{}] 火山引擎文本生成失败", requestId, e);
            throw new RuntimeException("文本生成失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getEndpointForTask("image-generation"));
            requestBody.put("prompt", request.getPrompt());
            
            if (request.getNegativePrompt() != null) {
                requestBody.put("negative_prompt", request.getNegativePrompt());
            }
            
            requestBody.put("n", request.getN());
            requestBody.put("size", request.getSize());
            requestBody.put("response_format", request.getResponseFormat());
            
            String response = sendRequest("/images/generations", requestBody);
            JsonNode responseJson = objectMapper.readTree(response);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            List<ImageGenerationResponse.GeneratedImage> images = new ArrayList<>();
            JsonNode dataArray = responseJson.path("data");
            for (int i = 0; i < dataArray.size(); i++) {
                JsonNode imgNode = dataArray.get(i);
                images.add(ImageGenerationResponse.GeneratedImage.builder()
                        .index(i)
                        .url(imgNode.path("url").asText())
                        .b64Json(imgNode.path("b64_json").asText())
                        .build());
            }
            
            return ImageGenerationResponse.builder()
                    .requestId(requestId)
                    .images(images)
                    .model(responseJson.path("model").asText())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("[{}] 火山引擎图片生成失败", requestId, e);
            throw new RuntimeException("图片生成失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public SpeechRecognitionResponse recognizeSpeech(SpeechRecognitionRequest request) {
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getEndpointForTask("asr"));
            
            if (request.getAudioUrl() != null) {
                requestBody.put("audio_url", request.getAudioUrl());
            } else if (request.getAudioBase64() != null) {
                requestBody.put("audio_base64", request.getAudioBase64());
            }
            
            requestBody.put("language", request.getLanguage());
            
            String response = sendRequest("/audio/transcriptions", requestBody);
            JsonNode responseJson = objectMapper.readTree(response);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return SpeechRecognitionResponse.builder()
                    .requestId(requestId)
                    .text(responseJson.path("text").asText())
                    .language(request.getLanguage())
                    .duration(responseJson.path("duration").asDouble())
                    .confidence(responseJson.path("confidence").asDouble())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("[{}] 火山引擎语音识别失败", requestId, e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request) {
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getEndpointForTask("tts"));
            requestBody.put("input", request.getText());
            requestBody.put("voice", request.getVoice());
            requestBody.put("speed", request.getSpeed());
            requestBody.put("pitch", request.getPitch());
            requestBody.put("response_format", request.getFormat());
            
            String response = sendRequest("/audio/speech", requestBody);
            JsonNode responseJson = objectMapper.readTree(response);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return SpeechSynthesisResponse.builder()
                    .requestId(requestId)
                    .audioUrl(responseJson.path("url").asText())
                    .format(request.getFormat())
                    .duration(responseJson.path("duration").asDouble())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("[{}] 火山引擎语音合成失败", requestId, e);
            throw new RuntimeException("语音合成失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public IntentRecognitionResponse recognizeIntent(IntentRecognitionRequest request) {
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建意图识别提示词
            String prompt = buildIntentPrompt(request);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getEndpointForTask("intent-recognition"));
            
            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 500);
            
            String response = sendRequest("/chat/completions", requestBody);
            JsonNode responseJson = objectMapper.readTree(response);
            
            String result = responseJson.path("choices").get(0).path("message").path("content").asText();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 解析意图结果
            return parseIntentResult(requestId, result, responseTime);
            
        } catch (Exception e) {
            log.error("[{}] 火山引擎意图识别失败", requestId, e);
            throw new RuntimeException("意图识别失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean healthCheck() {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", getEndpointForTask("text-generation"));
            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", "Hi");
            requestBody.put("max_tokens", 5);
            
            sendRequest("/chat/completions", requestBody);
            return true;
        } catch (Exception e) {
            log.warn("火山引擎健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 发送HTTP请求
     */
    private String sendRequest(String path, ObjectNode requestBody) {
        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        return client.post()
                .uri(path)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("API错误: " + errorBody)))
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .block();
    }
    
    /**
     * 获取任务对应的模型端点
     */
    private String getEndpointForTask(String task) {
        // 从配置中获取端点ID
        return switch (task) {
            case "text-generation" -> System.getenv().getOrDefault("VOLC_ENDPOINT_PRO_32K", "doubao-pro-32k");
            case "image-generation" -> System.getenv().getOrDefault("VOLC_ENDPOINT_VISION", "doubao-vision");
            case "asr" -> System.getenv().getOrDefault("VOLC_ENDPOINT_ASR", "asr");
            case "tts" -> System.getenv().getOrDefault("VOLC_ENDPOINT_TTS", "tts");
            case "intent-recognition" -> System.getenv().getOrDefault("VOLC_ENDPOINT_LITE_4K", "doubao-lite-4k");
            default -> "doubao-pro-32k";
        };
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(TextGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        if (request.getContentType() != null) {
            prompt.append("请生成").append(request.getContentType()).append("内容。");
        }
        
        if (request.getTargetPlatform() != null) {
            prompt.append("目标平台:").append(request.getTargetPlatform()).append("。");
        }
        
        if (request.getTone() != null) {
            prompt.append("语气风格:").append(request.getTone()).append("。");
        }
        
        prompt.append("\n\n").append(request.getPrompt());
        
        return prompt.toString();
    }
    
    /**
     * 构建意图识别提示词
     */
    private String buildIntentPrompt(IntentRecognitionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下用户输入的意图,以JSON格式返回结果。\n\n");
        prompt.append("用户输入:").append(request.getText()).append("\n\n");
        prompt.append("可选意图类别:\n");
        prompt.append("- lead_inquiry: 商机咨询\n");
        prompt.append("- product_query: 产品查询\n");
        prompt.append("- price_query: 价格查询\n");
        prompt.append("- complaint: 投诉反馈\n");
        prompt.append("- support_request: 技术支持\n");
        prompt.append("- appointment: 预约服务\n");
        prompt.append("- general_chat: 闲聊\n\n");
        prompt.append("返回格式:{\"intent\": \"意图类型\", \"confidence\": 0.95, \"entities\": {}}");
        
        return prompt.toString();
    }
    
    /**
     * 解析意图识别结果
     */
    private IntentRecognitionResponse parseIntentResult(String requestId, String result, long responseTime) {
        try {
            JsonNode json = objectMapper.readTree(result);
            
            IntentRecognitionResponse.Intent primaryIntent = IntentRecognitionResponse.Intent.builder()
                    .type(json.path("intent").asText("unknown"))
                    .name(json.path("intent").asText("unknown"))
                    .confidence(json.path("confidence").asDouble(0.5))
                    .build();
            
            return IntentRecognitionResponse.builder()
                    .requestId(requestId)
                    .primaryIntent(primaryIntent)
                    .confidence(primaryIntent.getConfidence())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.warn("解析意图结果失败,返回默认结果", e);
            return IntentRecognitionResponse.builder()
                    .requestId(requestId)
                    .primaryIntent(IntentRecognitionResponse.Intent.builder()
                            .type("unknown")
                            .name("unknown")
                            .confidence(0.0)
                            .build())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
        }
    }
    
    private String generateRequestId() {
        return "volc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
