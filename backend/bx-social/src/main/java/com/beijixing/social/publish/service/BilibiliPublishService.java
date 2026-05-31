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
 * B站（哔哩哔哩）发布服务
 * 基于B站开放平台API (https://open.bilibili.com/)
 * 支持视频、专栏文章等多种内容类型发布
 *
 * API文档: https://open.bilibili.com/doc/#
 * 备选方案: Playwright浏览器自动化（参考MediaCrawler 27.7k⭐）
 *
 * @author 北极星AI团队
 * @version 2.0 (开源集成版)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BilibiliPublishService {

    @Value("${bilibili.api.base-url:https://api.bilibili.com}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * 发布视频到B站
     *
     * @param accessToken B站access_token
     * @param request 视频发布请求
     * @return 发布结果
     */
    public PublishResult publishVideo(String accessToken, VideoPublishRequest request) {
        log.info("开始发布B站视频: title={}", request.getTitle());

        try {
            String videoId = uploadVideo(accessToken, request);

            if (request.getCoverUrl() != null && !request.getCoverUrl().isEmpty()) {
                uploadCover(accessToken, videoId, request.getCoverUrl());
            }

            Map<String, Object> submitParams = new HashMap<>();
            submitParams.put("copyright", request.getCopyright()); // 1=原创, 2=转载
            submitParams.put("source", request.getSource());
            submitParams.put("tid", request.getTid()); // 分区ID
            submitParams.put("cover", request.getCoverUrl());
            submitParams.put("title", request.getTitle());
            submitParams.put("desc", request.getDescription());
            submitParams.put("tag", String.join(",", request.getTags()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(submitParams), headers);

            String url = baseUrl + "/x/web/archive/submit";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.getIntValue("code", -1) == 0) {
                    JSONObject data = result.getJSONObject("data");
                    if (data != null) {
                        String bvid = data.getString("bvid");

                        log.info("B站视频发布成功: bvid={}", bvid);

                        return PublishResult.success(bvid, "BILIBILI", "视频发布成功");
                    }
                } else {
                    throw new RuntimeException("B站API错误: " + result.getString("message"));
                }
            }

            return PublishResult.failure("BILIBILI", "HTTP请求失败: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("B站视频发布失败: {}", e.getMessage(), e);
            return PublishResult.failure("BILIBILI", "发布失败: " + e.getMessage());
        }
    }

    /**
     * 上传视频文件
     */
    private String uploadVideo(String accessToken, VideoPublishRequest request) throws Exception {
        byte[] videoBytes = downloadFile(request.getVideoUrl());

        org.springframework.util.LinkedMultiValueMap<String, Object> body =
                new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file",
                new org.springframework.core.io.ByteArrayResource(videoBytes) {
                    @Override
                    public String getFilename() {
                        return "video.mp4";
                    }
                });
        body.add("name", "video.mp4");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                new HttpEntity<>(body, headers);

        String url = baseUrl + "/x/web/archive/submit?access_token=" + accessToken;
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject result = JSON.parseObject(response.getBody());
            if (result.getIntValue("code", -1) == 0) {
                JSONObject data = result.getJSONObject("data");
                if (data != null) {
                    return data.getString("video_id");
                }
            }
        }

        throw new RuntimeException("视频上传失败");
    }

    /**
     * 上传封面图片
     */
    private void uploadCover(String accessToken, String videoId, String coverUrl) throws Exception {
        byte[] imageBytes = downloadImage(coverUrl);

        org.springframework.util.LinkedMultiValueMap<String, Object> body =
                new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file",
                new org.springframework.core.io.ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return "cover.jpg";
                    }
                });
        body.add("video_id", videoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                new HttpEntity<>(body, headers);

        String url = baseUrl + "/x/web/archive/cover/up?access_token=" + accessToken;
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private byte[] downloadFile(String fileUrl) throws Exception {
        ResponseEntity<byte[]> response = restTemplate.exchange(
                fileUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        throw new RuntimeException("下载文件失败: " + response.getStatusCode());
    }

    private byte[] downloadImage(String imageUrl) throws Exception {
        return downloadFile(imageUrl);
    }

    @Data
    public static class VideoPublishRequest {
        private String title; // 标题
        private String description; // 简介/描述
        private String videoUrl; // 视频文件URL
        private String coverUrl; // 封面图片URL
        private Integer copyright = 1; // 1=原创, 2=转载
        private String source; // 转载来源
        private Integer tid = 21; // 分区ID（默认21=科技区）
        private java.util.List<String> tags; // 标签列表
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

    public DeleteResult deleteVideo(String accessToken, String bvid) {
        log.info("开始删除B站视频: bvid={}", bvid);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("bvid", bvid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(params), headers);

            String url = baseUrl + "/x/web/archive/delete";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.getIntValue("code", -1) == 0) {
                    log.info("B站视频删除成功: bvid={}", bvid);
                    return DeleteResult.success(bvid, "BILIBILI", "视频删除成功");
                } else {
                    return DeleteResult.failure("BILIBILI", "删除失败: " + result.getString("message"));
                }
            }

            return DeleteResult.failure("BILIBILI", "HTTP请求失败: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("B站视频删除失败: {}", e.getMessage(), e);
            return DeleteResult.failure("BILIBILI", "删除失败: " + e.getMessage());
        }
    }
}
