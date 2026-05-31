package com.beijixing.social.message.controller;

import com.beijixing.social.message.entity.MessageTemplate;
import com.beijixing.social.message.service.AutoMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社交消息控制器
 *
 * 原名: MessageController (已重命名避免与bx-message.MessageController冲突)
 * 功能: 社交平台自动消息发送、模板管理
 * 最后更新: 2026-05-20 (极简单体版)
 */
@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class SocialMessageController {

    private final AutoMessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = request.get("templateId") != null ?
                    Long.valueOf(request.get("templateId").toString()) : null;

            AutoMessageService.MessageResult result = messageService.sendPersonalizedMessage(
                    null, templateId);

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-send")
    public ResponseEntity<?> batchSendMessage(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> commentIds = (List<Long>) request.get("commentIds");
            if (commentIds != null && !commentIds.isEmpty()) {
                log.debug("批量发送消息，评论数: {}", commentIds.size());
            }
            Long templateId = request.get("templateId") != null ?
                    Long.valueOf(request.get("templateId").toString()) : null;
            int maxConcurrent = request.get("maxConcurrent") != null ?
                    (Integer) request.get("maxConcurrent") : 5;
            int intervalMs = request.get("intervalMs") != null ?
                    (Integer) request.get("intervalMs") : 30000;

            AutoMessageService.BatchMessageResult result =
                    messageService.batchSendMessages(null, templateId, maxConcurrent, intervalMs);

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("批量发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/template/create")
    public ResponseEntity<?> createTemplate(@RequestBody AutoMessageService.CreateTemplateRequest request) {
        try {
            MessageTemplate template = messageService.createCustomTemplate(request);
            return ResponseEntity.ok().body(template);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/template/generate-ai")
    public ResponseEntity<?> generateAiTemplates(@RequestBody Map<String, String> request) {
        try {
            String platformCode = request.get("platformCode");
            String intentLevel = request.get("intentLevel");
            String industry = request.getOrDefault("industry", "通用");
            String productName = request.getOrDefault("productName", "");

            List<MessageTemplate> templates = messageService.generateAiTemplates(
                    platformCode, intentLevel, industry, productName);

            return ResponseEntity.ok().body(templates);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("AI生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<?> getTemplates(
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) String intentLevel) {

        List<MessageTemplate> templates = messageService.getTemplates(platformCode, intentLevel);

        return ResponseEntity.ok().body(templates);
    }
}