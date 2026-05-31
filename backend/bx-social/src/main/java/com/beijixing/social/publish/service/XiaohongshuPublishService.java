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
public class XiaohongshuPublishService {

    @Value("${xiaohongshu.api.base-url:https://open.xiaohongsho.com}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public PublishResult publishNote(String accessToken, NotePublishRequest request) {
        log.info("开始发布小红书笔记: title={}, type={}", request.getTitle(), request.getNoteType());

        try {
            if ("VIDEO".equals(request.getNoteType())) {
                return publishVideoNote(accessToken, request);
            } else {
                return publishImageNote(accessToken, request);
            }
        } catch (Exception e) {
            log.error("小红书笔记发布失败: {}", e.getMessage(), e);
            return PublishResult.builder()
                    .success(false)
                    .platform("XIAOHONGSHU")
                    .message("发布失败: " + e.getMessage())
                    .build();
        }
    }

    private PublishResult publishVideoNote(String accessToken, NotePublishRequest request) throws Exception {
        String videoId = uploadVideo(accessToken, request.getVideoUrl());

        Map<String, Object> params = new HashMap<>();
        params.put("video_id", videoId);
        params.put("title", request.getTitle());
        params.put("content", buildContent(request));
        params.put("topics", JSON.toJSONString(request.getTopics()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(params), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/sns/v2/note",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            
            JSONObject data = result.getJSONObject("data");
            if (data != null) {
                String noteId = data.getString("note_id");

                log.info("小红书视频笔记发布成功: noteId={}", noteId);

                return PublishResult.builder()
                        .success(true)
                        .platformId(noteId)
                        .platform("XIAOHONGSHU")
                        .publishTime(LocalDateTime.now())
                        .message("视频笔记发布成功")
                        .build();
            }
        }

        return PublishResult.builder()
                .success(false)
                .platform("XIAOHONGSHU")
                .message("视频笔记发布失败")
                .build();
    }

    private PublishResult publishImageNote(String accessToken, NotePublishRequest request) throws Exception {
        List<String> imageIds = new java.util.ArrayList<>();

        for (String imageUrl : request.getImageUrls()) {
            String imageId = uploadImage(accessToken, imageUrl);
            imageIds.add(imageId);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("image_ids", imageIds);
        params.put("title", request.getTitle());
        params.put("content", buildContent(request));
        params.put("topics", JSON.toJSONString(request.getTopics()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(params), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/sns/v2/note",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            
            JSONObject data = result.getJSONObject("data");
            if (data != null) {
                String noteId = data.getString("note_id");

                log.info("小红书图文笔记发布成功: noteId={}", noteId);

                return PublishResult.builder()
                        .success(true)
                        .platformId(noteId)
                        .platform("XIAOHONGSHU")
                        .publishTime(LocalDateTime.now())
                        .message("图文笔记发布成功")
                        .build();
            }
        }

        return PublishResult.builder()
                .success(false)
                .platform("XIAOHONGSHU")
                .message("图文笔记发布失败")
                .build();
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
                baseUrl + "/api/sns/v1/upload/video",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            return result.getString("video_id");
        }

        throw new RuntimeException("视频上传失败");
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
                baseUrl + "/api/sns/v1/upload/image",
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            return result.getString("image_id");
        }

        throw new RuntimeException("图片上传失败");
    }

    private String buildContent(NotePublishRequest request) {
        StringBuilder content = new StringBuilder();
        
        content.append(request.getText() != null ? request.getText() : "");
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tag : request.getTags()) {
                content.append("\n#").append(tag);
            }
        }

        if (request.getLocation() != null) {
            content.append("\n📍").append(request.getLocation());
        }

        return content.toString();
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
    public static class NotePublishRequest {
        private String noteType;
        private String videoUrl;
        private List<String> imageUrls;
        private String title;
        private String text;
        private List<String> topics;
        private List<String> tags;
        private String location;
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

    public DeleteResult deleteNote(String accessToken, String noteId) {
        log.info("开始删除小红书笔记: noteId={}", noteId);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("note_id", noteId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(params), headers);

            String url = baseUrl + "/api/sns/v2/note/delete";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.getIntValue("code", -1) == 0) {
                    log.info("小红书笔记删除成功: noteId={}", noteId);
                    return DeleteResult.builder()
                            .success(true)
                            .platformId(noteId)
                            .platform("XIAOHONGSHU")
                            .deleteTime(LocalDateTime.now())
                            .message("笔记删除成功")
                            .build();
                } else {
                    int errorCode = result.getIntValue("code", -1);
                    String errorMsg = result.getString("message");
                    return DeleteResult.builder()
                            .success(false)
                            .platform("XIAOHONGSHU")
                            .message("删除失败[" + errorCode + "]: " + errorMsg)
                            .build();
                }
            }

            return DeleteResult.builder()
                    .success(false)
                    .platform("XIAOHONGSHU")
                    .message("HTTP请求失败: " + response.getStatusCode())
                    .build();

        } catch (Exception e) {
            log.error("小红书笔记删除失败: {}", e.getMessage(), e);
            return DeleteResult.builder()
                    .success(false)
                    .platform("XIAOHONGSHU")
                    .message("删除失败: " + e.getMessage())
                    .build();
        }
    }
}
