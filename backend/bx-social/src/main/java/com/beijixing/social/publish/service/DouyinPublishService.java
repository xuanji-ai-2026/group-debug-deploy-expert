package com.beijixing.social.publish.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinPublishService {

    @Value("${douyin.api.base-url:https://open.douyin-oauth.douyinsso.com}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public PublishResult publishVideo(String accessToken, VideoPublishRequest request) {
        log.info("开始发布抖音视频: title={}", request.getTitle());

        try {
            String videoId = uploadVideo(accessToken, request.getVideoUrl());
            
            if (request.getCoverUrl() != null && !request.getCoverUrl().isEmpty()) {
                uploadCover(accessToken, videoId, request.getCoverUrl());
            }

            Map<String, Object> publishParams = new HashMap<>();
            publishParams.put("video_id", videoId);
            publishParams.put("title", request.getTitle());

            if (request.getText() != null) {
                publishParams.put("text", request.getText());
            }

            if (request.getTopics() != null && !request.getTopics().isEmpty()) {
                publishParams.put("topics", JSON.toJSONString(request.getTopics()));
            }

            if (request.getMentionUsers() != null && !request.getMentionUsers().isEmpty()) {
                publishParams.put("mention_users", JSON.toJSONString(request.getMentionUsers()));
            }

            if (request.getLocation() != null) {
                publishParams.put("geo_info", JSON.toJSONString(request.getLocation()));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(publishParams), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/video/create/",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if ("0".equals(result.getString("error_code"))) {
                    log.info("抖音视频发布成功: item_id={}", result.getString("item_id"));

                    return PublishResult.builder()
                            .success(true)
                            .platformId(result.getString("item_id"))
                            .platform("DOUYIN")
                            .publishTime(LocalDateTime.now())
                            .message("发布成功")
                            .build();
                } else {
                    throw new RuntimeException("抖音API错误: " + result.getString("description"));
                }
            }

            return PublishResult.builder()
                    .success(false)
                    .platform("DOUYIN")
                    .message("HTTP请求失败: " + response.getStatusCode())
                    .build();

        } catch (Exception e) {
            log.error("抖音视频发布失败: {}", e.getMessage(), e);
            return PublishResult.builder()
                    .success(false)
                    .platform("DOUYIN")
                    .message("发布失败: " + e.getMessage())
                    .build();
        }
    }

    public PublishResult publishImagePost(String accessToken, ImagePublishRequest request) {
        log.info("开始发布抖音图文: title={}", request.getTitle());

        try {
            JSONObject imageIds = new JSONObject();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String imageUrl = request.getImageUrls().get(i);
                String imageId = uploadImage(accessToken, imageUrl);
                imageIds.put(String.valueOf(i), imageId);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("image_ids", imageIds.toJSONString());
            params.put("title", request.getTitle());
            params.put("text", request.getText() != null ? request.getText() : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(params), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/image/create/",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if ("0".equals(result.getString("error_code"))) {
                    return PublishResult.builder()
                            .success(true)
                            .platformId(result.getString("item_id"))
                            .platform("DOUYIN")
                            .publishTime(LocalDateTime.now())
                            .message("图文发布成功")
                            .build();
                }
            }

            return PublishResult.builder()
                    .success(false)
                    .platform("DOUYIN")
                    .message("图文发布失败")
                    .build();

        } catch (Exception e) {
            log.error("抖音图文发布失败: {}", e.getMessage(), e);
            return PublishResult.builder()
                    .success(false)
                    .platform("DOUYIN")
                    .message("发布失败: " + e.getMessage())
                    .build();
        }
    }

    private String uploadVideo(String accessToken, String videoUrl) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        org.springframework.util.MultiValueMap<String, Object> body = 
                new org.springframework.util.LinkedMultiValueMap<>();
        
        byte[] videoBytes = downloadFile(videoUrl);
        org.springframework.core.io.ByteArrayResource videoResource = 
                new org.springframework.core.io.ByteArrayResource(videoBytes) {
            @Override
            public String getFilename() {
                return "video.mp4";
            }
        };
        
        body.add("video", videoResource);

        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity = 
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/video/upload/",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            if ("0".equals(result.getString("error_code"))) {
                return result.getString("video_id");
            }
        }

        throw new RuntimeException("视频上传失败");
    }

    private void uploadCover(String accessToken, String videoId, String coverUrl) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        org.springframework.util.MultiValueMap<String, Object> body = 
                new org.springframework.util.LinkedMultiValueMap<>();
        
        byte[] coverBytes = downloadFile(coverUrl);
        org.springframework.core.io.ByteArrayResource coverResource = 
                new org.springframework.core.io.ByteArrayResource(coverBytes) {
            @Override
            public String getFilename() {
                return "cover.jpg";
            }
        };
        
        body.add("image_id", coverResource);
        body.add("video_id", videoId);

        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity = 
                new HttpEntity<>(body, headers);

        restTemplate.exchange(
                baseUrl + "/video/upload_cover/",
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    private String uploadImage(String accessToken, String imageUrl) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        org.springframework.util.MultiValueMap<String, Object> body = 
                new org.springframework.util.LinkedMultiValueMap<>();
        
        byte[] imageBytes = downloadFile(imageUrl);
        org.springframework.core.io.ByteArrayResource imageResource = 
                new org.springframework.core.io.ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };
        
        body.add("image", imageResource);

        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity = 
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/image/upload/",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            if ("0".equals(result.getString("error_code"))) {
                return result.getString("image_id");
            }
        }

        throw new RuntimeException("图片上传失败");
    }

    private byte[] downloadFile(String url) throws Exception {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("文件下载失败: " + url);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VideoPublishRequest {
        private String videoUrl;
        private String coverUrl;
        private String title;
        private String text;
        private List<String> topics;
        private List<String> mentionUsers;
        private Map<String, Object> location;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImagePublishRequest {
        private List<String> imageUrls;
        private String title;
        private String text;
        private List<String> topics;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PublishResult {
        private boolean success;
        private String platform;
        private String platformId;
        private LocalDateTime publishTime;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.Getter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DeleteResult {
        private boolean success;
        private String platform;
        private String platformId;
        private LocalDateTime deleteTime;
        private String message;
    }

    public DeleteResult deleteVideo(String accessToken, String itemId) {
        log.info("开始删除抖音视频: itemId={}", itemId);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("item_id", itemId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(params), headers);

            String url = baseUrl + "/video/delete/";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.getIntValue("error_code", -1) == 0) {
                    log.info("抖音视频删除成功: itemId={}", itemId);
                    return DeleteResult.builder()
                            .success(true)
                            .platformId(itemId)
                            .platform("DOUYIN")
                            .deleteTime(LocalDateTime.now())
                            .message("视频删除成功")
                            .build();
                } else {
                    int errorCode = result.getIntValue("error_code", -1);
                    String errorMsg = result.getString("description");
                    return DeleteResult.builder()
                            .success(false)
                            .platform("DOUYIN")
                            .message("删除失败[" + errorCode + "]: " + errorMsg)
                            .build();
                }
            }

            return DeleteResult.builder()
                    .success(false)
                    .platform("DOUYIN")
                    .message("HTTP请求失败: " + response.getStatusCode())
                    .build();

        } catch (Exception e) {
            log.error("抖音视频删除失败: {}", e.getMessage(), e);
            return DeleteResult.builder()
                    .success(false)
                    .platform("DOUYIN")
                    .message("删除失败: " + e.getMessage())
                    .build();
        }
    }
}
