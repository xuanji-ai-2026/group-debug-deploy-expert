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
 * 微信公众号发布服务
 * 基于微信公众号官方API + WxJava最佳实践 (29.9k⭐ GitHub)
 * 支持图文消息、图片、视频等多种内容类型发布
 *
 * API文档: https://developers.weixin.qq.com/doc/offiaccount/Publish/Publish.html
 * 参考项目: https://github.com/Wechat-Group/WxJava (29.9k stars)
 *
 * @author 北极星AI团队
 * @version 2.0 (开源集成版)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPublishService {

    @Value("${wechat.api.base-url:https://api.weixin.qq.com}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * 发布图文消息到微信公众号
     *
     * @param accessToken 微信公众号access_token
     * @param request 发布请求参数
     * @return 发布结果
     */
    public PublishResult publishArticle(String accessToken, ArticlePublishRequest request) {
        log.info("开始发布微信公众号文章: title={}", request.getTitle());

        try {
            Map<String, Object> articleData = new HashMap<>();
            articleData.put("title", request.getTitle());
            articleData.put("author", request.getAuthor());
            articleData.put("digest", request.getDigest());
            articleData.put("content", request.getContent());
            articleData.put("content_source_url", request.getContentSourceUrl());
            articleData.put("thumb_media_id", request.getThumbMediaId());
            articleData.put("need_open_comment", request.getNeedOpenComment() ? 1 : 0);
            articleData.put("only_fans_can_comment", request.getOnlyFansCanComment() ? 1 : 0);

            Map<String, Object> params = new HashMap<>();
            params.put("articles", new Map[]{articleData});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(params), headers);

            String url = baseUrl + "/cgi-bin/draft/add?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.containsKey("media_id")) {
                    String mediaId = result.getString("media_id");

                    log.info("微信公众号草稿创建成功: mediaId={}", mediaId);

                    boolean publishSuccess = publishDraft(accessToken, mediaId);

                    if (publishSuccess) {
                        return PublishResult.success(mediaId, "WECHAT", "文章发布成功");
                    } else {
                        return PublishResult.failure("WECHAT", "草稿创建成功但发布失败");
                    }
                } else {
                    int errcode = result.getIntValue("errcode", -1);
                    String errmsg = result.containsKey("errmsg") ? result.getString("errmsg") : "未知错误";
                    throw new RuntimeException("微信API错误[" + errcode + "]: " + errmsg);
                }
            }

            return PublishResult.failure("WECHAT", "HTTP请求失败: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("微信公众号文章发布失败: {}", e.getMessage(), e);
            return PublishResult.failure("WECHAT", "发布失败: " + e.getMessage());
        }
    }

    /**
     * 发布草稿
     */
    private boolean publishDraft(String accessToken, String mediaId) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("media_id", mediaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(params), headers);

            String url = baseUrl + "/cgi-bin/freepublish/submit?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                return result.containsKey("publish_id");
            }

            return false;
        } catch (Exception e) {
            log.error("发布草稿失败: mediaId={}, error={}", mediaId, e.getMessage());
            return false;
        }
    }

    /**
     * 上传临时图片素材
     */
    public String uploadTempImage(String accessToken, String imageUrl) {
        try {
            byte[] imageBytes = downloadImage(imageUrl);

            org.springframework.util.LinkedMultiValueMap<String, Object> body =
                    new org.springframework.util.LinkedMultiValueMap<>();
            body.add("media",
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

            String url = baseUrl + "/cgi-bin/media/upload?access_token=" + accessToken
                    + "&type=image";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result.containsKey("media_id")) {
                    return result.getString("media_id");
                }
            }

            return null;
        } catch (Exception e) {
            log.error("上传临时图片素材失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 下载图片
     */
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

    /**
     * 获取发布状态
     */
    public PublishStatusResult getPublishStatus(String accessToken, String publishId) {
        try {
            String url = baseUrl + "/cgi-bin/freepublish/get?access_token=" + accessToken
                    + "&publish_id=" + publishId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                PublishStatusResult statusResult = new PublishStatusResult();
                statusResult.setPublishId(publishId);
                statusResult.setPublishStatus(result.getIntValue("publish_status", -1));
                statusResult.setArticleId(result.getString("article_id"));
                statusResult.setArticleDetail(result.getJSONObject("article_detail"));
                statusResult.failMsg = result.getString("fail_msg");

                return statusResult;
            }
        } catch (Exception e) {
            log.error("获取发布状态失败: {}", e.getMessage());
        }

        return null;
    }

    @Data
    public static class ArticlePublishRequest {
        private String title;
        private String author;
        private String digest;
        private String content; // HTML格式富文本内容
        private String contentSourceUrl;
        private String thumbMediaId; // 封面图片素材ID
        private Boolean needOpenComment = true;
        private Boolean onlyFansCanComment = false;
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
    public static class PublishStatusResult {
        private String publishId;
        private Integer publishStatus;
        private String articleId;
        private JSONObject articleDetail;
        private String failMsg;
    }

    @Data
    public static class ArticleDeleteRequest {
        private String articleId;
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

    public DeleteResult deleteArticle(String accessToken, ArticleDeleteRequest request) {
        log.info("开始删除微信公众号文章: articleId={}", request.getArticleId());

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("article_id", request.getArticleId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(
                    JSON.toJSONString(params), headers);

            String url = baseUrl + "/cgi-bin/freepublish/delete?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());

                if (result.getIntValue("errcode", -1) == 0) {
                    log.info("微信公众号文章删除成功: articleId={}", request.getArticleId());
                    return DeleteResult.success(request.getArticleId(), "WECHAT", "文章删除成功");
                } else {
                    int errcode = result.getIntValue("errcode", -1);
                    String errmsg = result.containsKey("errmsg") ? result.getString("errmsg") : "未知错误";
                    return DeleteResult.failure("WECHAT", "删除失败[" + errcode + "]: " + errmsg);
                }
            }

            return DeleteResult.failure("WECHAT", "HTTP请求失败: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("微信公众号文章删除失败: {}", e.getMessage(), e);
            return DeleteResult.failure("WECHAT", "删除失败: " + e.getMessage());
        }
    }
}
