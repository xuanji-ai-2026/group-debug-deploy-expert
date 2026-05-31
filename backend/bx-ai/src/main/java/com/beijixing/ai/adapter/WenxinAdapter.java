package com.beijixing.ai.adapter;

import com.beijixing.ai.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 百度文心一言适配器
 * 
 * @author 郑武 (EMP-AI-001)
 */
@Slf4j
@Component
public class WenxinAdapter implements ModelAdapter {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String TYPE = "WENXIN";
    private static final String BASE_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop";
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public TextGenerationResponse generateText(TextGenerationRequest request) {
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        try {
            String accessToken = getAccessToken();
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.set("messages", objectMapper.valueToTree(buildMessages(request)));
            
            WebClient client = webClientBuilder.build();
            
            String response = client.post()
                    .uri(BASE_URL + "/chat/completions?access_token=" + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            resp -> resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException(body))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            JsonNode responseJson = objectMapper.readTree(response);
            String generatedText = responseJson.path("result").asText();
            
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
                    .model("ernie-bot-4")
                    .tokenUsage(TextGenerationResponse.TokenUsage.builder()
                            .totalTokens(responseJson.path("usage").path("total_tokens").asInt())
                            .build())
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("[{}] 文心一言文本生成失败", requestId, e);
            throw new RuntimeException("文本生成失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        throw new UnsupportedOperationException("文心一言暂不支持图片生成");
    }
    
    @Override
    public SpeechRecognitionResponse recognizeSpeech(SpeechRecognitionRequest request) {
        throw new UnsupportedOperationException("文心一言暂不支持语音识别");
    }
    
    @Override
    public SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request) {
        throw new UnsupportedOperationException("文心一言暂不支持语音合成");
    }
    
    @Override
    public IntentRecognitionResponse recognizeIntent(IntentRecognitionRequest request) {
        // 使用文本生成能力实现意图识别
        TextGenerationRequest tgRequest = TextGenerationRequest.builder()
                .userId(request.getUserId())
                .prompt("用户意图:" + request.getText())
                .systemPrompt("你是一个意图识别助手,请分析用户输入的意图")
                .build();
        
        TextGenerationResponse tgResponse = generateText(tgRequest);
        
        return IntentRecognitionResponse.builder()
                .requestId(tgResponse.getRequestId())
                .primaryIntent(IntentRecognitionResponse.Intent.builder()
                        .type("unknown")
                        .name("unknown")
                        .confidence(0.5)
                        .build())
                .createTime(LocalDateTime.now())
                .build();
    }
    
    @Override
    public boolean healthCheck() {
        try {
            TextGenerationRequest request = TextGenerationRequest.builder()
                    .userId("health_check")
                    .prompt("Hi")
                    .build();
            generateText(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 构建消息列表
     */
    private ArrayNode buildMessages(TextGenerationRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();
        
        if (request.getSystemPrompt() != null) {
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
        }
        
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", request.getPrompt());
        
        return messages;
    }
    
    /**
     * 获取访问令牌(简化实现,实际应从缓存获取)
     */
    private String getAccessToken() {
        String apiKey = System.getenv("WENXIN_API_KEY");
        String secretKey = System.getenv("WENXIN_SECRET_KEY");
        
        if (apiKey == null || secretKey == null) {
            throw new RuntimeException("文心一言API密钥未配置");
        }
        
        // 实际应调用百度OAuth API获取token
        // 这里简化处理
        return System.getenv("WENXIN_ACCESS_TOKEN");
    }
    
    private String generateRequestId() {
        return "wenxin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
