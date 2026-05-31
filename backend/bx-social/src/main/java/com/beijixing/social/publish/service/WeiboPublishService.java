package com.beijixing.social.publish.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 微博发布服务
 * 基于微博开放平台官方API (https://open.weibo.com/)
 * 支持文本、图文、视频等多种内容类型发布
 *
 * API文档: https://open.weibo.com/wiki/index.php/API%E6%96%87%E6%A1%A3_V2
 * 参考实践: CSDN优秀案例 + GitHub weibo4j项目
 *
 * @author 北极星AI团队
 * @version 2.0 (开源集成版)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeiboPublishService {

    @Value("${weibo.api.base-url:https://api.weibo.com}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * 发布文本微博
     *
     * @param accessToken 微博access_token
     * @param text 微博文本内容（不超过2000字）
     * @return 发布结果
     */
    public PublishResult publishText(String accessToken, String text) {
        log.info("开始发布微博: text={}", text.length() > 50 ? text.substring(0, 50) + "..." : text);

        try {
            Map<String, String> params = new HashMap<>();
            params.put("status", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            String url = baseUrl + "/2/statuses/share.json?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.containsKey("id")) {
                    String weiboId = result.getString("idstr") != null ?
                            result.getString("idstr") : result.getString("id");

                    log.info("微博发布成功: weiboId={}", weiboId);

                    return PublishResult.success(weiboId, "WEIBO", "微博发布成功");
                } else {
                    int errorCode = result.getIntValue("error_code", -1);
                    String errorMsg = result.containsKey("error") ? result.getString("error") : "未知错误";
                    throw new RuntimeException("微博API错误[" + errorCode + "]: " + errorMsg);
                }
            }

            return PublishResult.failure("WEIBO", "HTTP请求失败: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("微博发布失败: {}", e.getMessage(), e);
            return PublishResult.failure("WEIBO", "发布失败: " + e.getMessage());
        }
    }

    /**
     * 发布图文微博（带图片）
     *
     * @param accessToken 微博access_token
     * @param request 图文发布请求
     * @return 发布结果
     */
    public PublishResult publishImageText(String accessToken, ImageTextPublishRequest request) {
        log.info("开始发布图文微博: title={}", request.getTitle());

        try {
            String picIds = uploadImages(accessToken, request.getImageUrls());

            StringBuilder statusBuilder = new StringBuilder();
            if (request.getTitle() != null && !request.getTitle().isEmpty()) {
                statusBuilder.append(request.getTitle()).append("\n");
            }
            if (request.getText() != null && !request.getText().isEmpty()) {
                statusBuilder.append(request.getText());
            }

            Map<String, String> params = new HashMap<>();
            params.put("status", statusBuilder.toString());
            if (picIds != null && !picIds.isEmpty()) {
                params.put("pic_id", picIds);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            String url = baseUrl + "/2/statuses/share.json?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.containsKey("id")) {
                    String weiboId = result.getString("idstr") != null ?
                            result.getString("idstr") : result.getString("id");

                    log.info("图文微博发布成功: weiboId={}", weiboId);

                    return PublishResult.success(weiboId, "WEIBO", "图文微博发布成功");
                }
            }

            return PublishResult.failure("WEIBO", "图文微博发布失败");

        } catch (Exception e) {
            log.error("图文微博发布失败: {}", e.getMessage(), e);
            return PublishResult.failure("WEIBO", "发布失败: " + e.getMessage());
        }
    }

    /**
     * 上传图片到微博
     */
    private String uploadImages(String accessToken, java.util.List<String> imageUrls) throws Exception {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }

        StringBuilder picIds = new StringBuilder();

        for (String imageUrl : imageUrls) {
            try {
                byte[] imageBytes = downloadImage(imageUrl);

                org.springframework.util.LinkedMultiValueMap<String, Object> body =
                        new org.springframework.util.LinkedMultiValueMap<>();
                body.add("pic",
                        new org.springframework.core.io.ByteArrayResource(imageBytes) {
                            @Override
                            public String getFilename() {
                                return "image.jpg";
                            }
                        });

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                        new HttpEntity<>(body, headers);

                String url = baseUrl + "/2/statuses/upload.json?access_token=" + accessToken;
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JSONObject result = JSON.parseObject(response.getBody());
                    if (result.containsKey("pics")) {
                        JSONObject pics = result.getJSONObject("pics");
                        if (pics != null && pics.containsKey("pic_id")) {
                            if (picIds.length() > 0) {
                                picIds.append(",");
                            }
                            picIds.append(pics.getString("pic_id"));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("上传图片失败: url={}, error={}", imageUrl, e.getMessage());
            }
        }

        return picIds.toString();
    }

    private byte[] downloadImage(String imageUrl) throws Exception {
        ResponseEntity<byte[]> response = restTemplate.exchange(
                imageUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        throw new RuntimeException("下载图片失败: " + response.getStatusCode());
    }

    @Data
    public static class ImageTextPublishRequest {
        private String title; // 标题（可选）
        private String text;  // 正文内容
        private java.util.List<String> imageUrls; // 图片URL列表
    }

    @Data
    public static class PublishResult {
        private Boolean success;
        private String platformId;
        private String platform;
        private LocalDateTime publishTime;
        private String message;

        public PublishResult() {}

        public PublishResult(Boolean success, String platformId, String platform, LocalDateTime publishTime, String message) {
            this.success = success;
            this.platformId = platformId;
            this.platform = platform;
            this.publishTime = publishTime;
            this.message = message;
        }

        public static PublishResult success(String platformId, String platform, String message) {
            return new PublishResult(true, platformId, platform, LocalDateTime.now(), message);
        }

        public static PublishResult failure(String platform, String message) {
            return new PublishResult(false, null, platform, null, message);
        }
    }

    @Data
    public static class DeleteResult {
        private Boolean success;
        private String platformId;
        private String platform;
        private LocalDateTime deleteTime;
        private String message;

        public DeleteResult() {}

        public DeleteResult(Boolean success, String platformId, String platform, LocalDateTime deleteTime, String message) {
            this.success = success;
            this.platformId = platformId;
            this.platform = platform;
            this.deleteTime = deleteTime;
            this.message = message;
        }

        public static DeleteResult success(String platformId, String platform, String message) {
            return new DeleteResult(true, platformId, platform, LocalDateTime.now(), message);
        }

        public static DeleteResult failure(String platform, String message) {
            return new DeleteResult(false, null, platform, null, message);
        }
    }

    public DeleteResult deleteStatus(String accessToken, String statusId) {
        log.info("开始删除微博: statusId={}", statusId);

        try {
            Map<String, String> params = new HashMap<>();
            params.put("id", statusId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            String url = baseUrl + "/2/statuses/destroy.json?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.containsKey("id")) {
                    log.info("微博删除成功: statusId={}", statusId);
                    return DeleteResult.success(statusId, "WEIBO", "微博删除成功");
                } else {
                    int errorCode = result.getIntValue("error_code", -1);
                    String errorMsg = result.containsKey("error") ? result.getString("error") : "未知错误";
                    return DeleteResult.failure("WEIBO", "删除失败[" + errorCode + "]: " + errorMsg);
                }
            }

            return DeleteResult.failure("WEIBO", "HTTP请求失败: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("微博删除失败: {}", e.getMessage(), e);
            return DeleteResult.failure("WEIBO", "删除失败: " + e.getMessage());
        }
    }
}
