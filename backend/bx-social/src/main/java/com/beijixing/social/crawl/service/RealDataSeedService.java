package com.beijixing.social.crawl.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;
import com.beijixing.social.crawl.mapper.CrawlTaskMapper;
import com.beijixing.social.crawl.mapper.SocialCommentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RealDataSeedService {

    private static final Logger log = LoggerFactory.getLogger(RealDataSeedService.class);

    private final SocialCommentMapper commentMapper;
    private final CrawlTaskMapper crawlTaskMapper;
    private final AiIntentAnalysisV2Service aiAnalysisService;
    private final LeadPenetrationService leadPenetrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BILIBILI_API = "https://api.bilibili.com";

    private static final Map<String, String> SEED_VIDEOS = new LinkedHashMap<>();

    static {
        SEED_VIDEOS.put("BV1GJ411x7h7", "AI人工智能");
        SEED_VIDEOS.put("BV1Kb411W7oM", "Python教程");
        SEED_VIDEOS.put("BV1Y54y1Q7EF", "SaaS软件");
        SEED_VIDEOS.put("BV1op4y1Q7V3", "企业管理软件");
        SEED_VIDEOS.put("BV1eK4y1k7VL", "数字化转型");
    }

    @Transactional(rollbackFor = Exception.class)
    public SeedResult seedFromBilibili() {
        log.info("=== 开始真实数据种子：B站评论爬取 ===");

        SeedResult seedResult = new SeedResult();
        seedResult.setStartTime(LocalDateTime.now());

        for (Map.Entry<String, String> entry : SEED_VIDEOS.entrySet()) {
            String bvid = entry.getKey();
            String topic = entry.getValue();

            try {
                log.info("爬取B站视频: BV={} 主题={}", bvid, topic);
                CrawlTask task = createCrawlTask(bvid, topic);

                long aid = resolveAidFromBvid(bvid);
                if (aid <= 0) {
                    log.warn("无法解析BV号: {}", bvid);
                    continue;
                }

                List<SocialComment> comments = crawlBilibiliComments(aid, bvid, task.getId());
                log.info("视频 {} 爬取到 {} 条评论", bvid, comments.size());

                int analyzed = analyzeComments(comments);
                seedResult.addVideoResult(bvid, topic, comments.size(), analyzed);

                task.setTotalCommentsFound(comments.size());
                task.setHighIntentCount(analyzed);
                task.setStatus(2);
                task.setProgressPercent(100);
                task.setEndTime(LocalDateTime.now());
                crawlTaskMapper.updateById(task);

                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("爬取视频 {} 失败: {}", bvid, e.getMessage());
                seedResult.addError(bvid, e.getMessage());
            }
        }

        int leadsGenerated = generateLeadsFromHighIntentComments();
        seedResult.setLeadsGenerated(leadsGenerated);
        seedResult.setEndTime(LocalDateTime.now());

        log.info("=== 真实数据种子完成: 总评论={}, 高意向={}, 生成商机={} ===",
                seedResult.getTotalComments(), seedResult.getTotalHighIntent(), leadsGenerated);

        return seedResult;
    }

    public SeedResult seedFromBilibiliVideo(String bvid) {
        log.info("爬取指定B站视频: BV={}", bvid);

        SeedResult seedResult = new SeedResult();
        seedResult.setStartTime(LocalDateTime.now());

        try {
            CrawlTask task = createCrawlTask(bvid, "自定义视频");

            long aid = resolveAidFromBvid(bvid);
            if (aid <= 0) {
                throw new RuntimeException("无法解析BV号: " + bvid);
            }

            List<SocialComment> comments = crawlBilibiliComments(aid, bvid, task.getId());
            log.info("视频 {} 爬取到 {} 条评论", bvid, comments.size());

            int analyzed = analyzeComments(comments);
            seedResult.addVideoResult(bvid, "自定义", comments.size(), analyzed);

            task.setTotalCommentsFound(comments.size());
            task.setHighIntentCount(analyzed);
            task.setStatus(2);
            task.setProgressPercent(100);
            task.setEndTime(LocalDateTime.now());
            crawlTaskMapper.updateById(task);

            int leadsGenerated = generateLeadsFromHighIntentComments();
            seedResult.setLeadsGenerated(leadsGenerated);

        } catch (Exception e) {
            log.error("爬取视频 {} 失败: {}", bvid, e.getMessage());
            seedResult.addError(bvid, e.getMessage());
        }

        seedResult.setEndTime(LocalDateTime.now());
        return seedResult;
    }

    private CrawlTask createCrawlTask(String bvid, String topic) {
        CrawlTask task = new CrawlTask();
        task.setTaskName("真实数据种子-" + topic);
        task.setTaskType("REAL_SEED");
        task.setPlatformCode("BILIBILI");
        task.setTargetType("VIDEO");
        task.setTargetId(bvid);
        task.setTargetUrl("https://www.bilibili.com/video/" + bvid);
        task.setKeywords(topic);
        task.setMaxCrawlCount(500);
        task.setStatus(1);
        task.setProgressPercent(0);
        task.setStartTime(LocalDateTime.now());
        task.setCreatedBy(1L);
        crawlTaskMapper.insert(task);
        return task;
    }

    private long resolveAidFromBvid(String bvid) {
        try {
            String url = BILIBILI_API + "/x/web-interface/view?bvid=" + bvid;
            HttpHeaders headers = createBilibiliHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                if (json.getInteger("code") == 0 && json.containsKey("data")) {
                    long aid = json.getJSONObject("data").getLongValue("aid");
                    log.info("BV号 {} 解析为 AID: {}", bvid, aid);
                    return aid;
                } else {
                    log.warn("B站视频信息API返回错误: code={}, message={}",
                            json.getInteger("code"), json.getString("message"));
                }
            }
        } catch (Exception e) {
            log.error("解析BV号失败: {} - {}", bvid, e.getMessage());
        }
        return -1;
    }

    private List<SocialComment> crawlBilibiliComments(long aid, String bvid, Long taskId) {
        List<SocialComment> allComments = new ArrayList<>();
        int maxPages = 5;

        for (int page = 1; page <= maxPages; page++) {
            try {
                String url = String.format("%s/x/v2/reply?type=1&oid=%d&ps=20&pn=%d&sort=0",
                        BILIBILI_API, aid, page);

                HttpHeaders headers = createBilibiliHeaders();
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    log.warn("B站评论API返回非200: page={}", page);
                    break;
                }

                JSONObject json = JSON.parseObject(response.getBody());
                if (json.getInteger("code") != 0) {
                    log.warn("B站评论API错误: code={}, message={}",
                            json.getInteger("code"), json.getString("message"));
                    break;
                }

                JSONObject data = json.getJSONObject("data");
                if (data == null) break;

                JSONArray replies = data.getJSONArray("replies");
                if (replies == null || replies.isEmpty()) {
                    log.info("第{}页无更多评论，停止翻页", page);
                    break;
                }

                for (int i = 0; i < replies.size(); i++) {
                    JSONObject replyObj = replies.getJSONObject(i);
                    SocialComment comment = parseBilibiliComment(replyObj, bvid, taskId);
                    if (comment != null) {
                        allComments.add(comment);

                        JSONArray subReplies = replyObj.getJSONArray("replies");
                        if (subReplies != null && !subReplies.isEmpty()) {
                            for (int j = 0; j < Math.min(subReplies.size(), 5); j++) {
                                SocialComment subComment = parseBilibiliComment(
                                        subReplies.getJSONObject(j), bvid, taskId);
                                if (subComment != null) {
                                    subComment.setParentCommentId(comment.getCommentId());
                                    subComment.setIsReply(true);
                                    allComments.add(subComment);
                                }
                            }
                        }
                    }
                }

                log.info("第{}页获取{}条评论，累计{}条", page, replies.size(), allComments.size());

                Thread.sleep(1500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("爬取第{}页评论失败: {}", page, e.getMessage());
                break;
            }
        }

        for (SocialComment comment : allComments) {
            try {
                SocialComment existing = commentMapper.selectByUniqueKey(
                        comment.getPlatformCode(), comment.getContentId(), comment.getCommentId());
                if (existing == null) {
                    commentMapper.insert(comment);
                }
            } catch (Exception e) {
                log.debug("评论已存在或插入失败: commentId={}", comment.getCommentId());
            }
        }

        return allComments;
    }

    private SocialComment parseBilibiliComment(JSONObject replyObj, String bvid, Long taskId) {
        try {
            SocialComment comment = new SocialComment();
            comment.setPlatformCode("BILIBILI");
            comment.setContentId(bvid);
            comment.setContentType("VIDEO");
            comment.setCrawlTaskId(taskId);
            comment.setCrawlSource("REAL_SEED");

            comment.setCommentId(String.valueOf(replyObj.getLongValue("rpid")));

            JSONObject content = replyObj.getJSONObject("content");
            if (content != null) {
                comment.setCommentText(content.getString("message"));
            }

            JSONObject member = replyObj.getJSONObject("member");
            if (member != null) {
                comment.setAuthorId(String.valueOf(member.getLongValue("mid")));
                comment.setAuthorName(member.getString("uname"));
                comment.setAuthorAvatar(member.getString("avatar"));

                String vipStatus = member.getString("vip_status");
                comment.setUserVerified("1".equals(vipStatus));

                JSONObject levelInfo = member.getJSONObject("level_info");
                if (levelInfo != null) {
                    comment.setUserFollowerCount(levelInfo.getInteger("current_level"));
                }
            }

            comment.setLikeCount(replyObj.getInteger("like"));
            comment.setReplyCount(replyObj.getInteger("rcount"));
            comment.setIsReply(false);

            long ctime = replyObj.getLongValue("ctime");
            if (ctime > 0) {
                comment.setPublishTime(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(ctime), ZoneId.systemDefault()));
            } else {
                comment.setPublishTime(LocalDateTime.now());
            }

            comment.setStatus(0);
            comment.setLeadGenerated(false);
            comment.setMessageSent(false);
            comment.setDeleted(0);
            comment.setCreateTime(LocalDateTime.now());
            comment.setUpdateTime(LocalDateTime.now());

            comment.setRawData(replyObj.toJSONString());

            return comment;

        } catch (Exception e) {
            log.warn("解析B站评论失败: {}", e.getMessage());
            return null;
        }
    }

    private int analyzeComments(List<SocialComment> comments) {
        int highIntentCount = 0;

        for (SocialComment comment : comments) {
            try {
                if (comment.getCommentText() == null || comment.getCommentText().trim().isEmpty()) {
                    continue;
                }

                AiIntentAnalysisV2Service.IntentAnalysisResult result = aiAnalysisService.analyzeComment(comment);

                if (result != null && result.isHighIntent()) {
                    highIntentCount++;
                }

            } catch (Exception e) {
                log.debug("分析评论失败: commentId={}, error={}", comment.getCommentId(), e.getMessage());
            }
        }

        return highIntentCount;
    }

    private int generateLeadsFromHighIntentComments() {
        try {
            List<CrawlTask> seedTasks = crawlTaskMapper.selectByStatus(2, 20);
            int totalLeads = 0;

            for (CrawlTask task : seedTasks) {
                if (!"REAL_SEED".equals(task.getTaskType())) continue;

                try {
                    LeadPenetrationService.LeadGenerationCriteria criteria =
                            new LeadPenetrationService.LeadGenerationCriteria();
                    criteria.setMinScore(50);
                    criteria.setAutoAssign(false);
                    criteria.setGenerateFollowUpTask(false);
                    criteria.setMaxLeads(30);

                    LeadPenetrationService.PenetrationResult result =
                            leadPenetrationService.generateLeadsFromComments(task.getId(), criteria);

                    if (result.getGeneratedCount() > 0) {
                        totalLeads += result.getGeneratedCount();
                        log.info("任务 {} 生成 {} 条商机", task.getId(), result.getGeneratedCount());
                    }

                } catch (Exception e) {
                    log.error("从任务 {} 生成商机失败: {}", task.getId(), e.getMessage());
                }
            }

            return totalLeads;

        } catch (Exception e) {
            log.error("生成商机失败: {}", e.getMessage());
            return 0;
        }
    }

    private HttpHeaders createBilibiliHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        headers.set("Referer", "https://www.bilibili.com/");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9");
        return headers;
    }

    public static class SeedResult {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalComments;
        private int totalHighIntent;
        private int leadsGenerated;
        private List<VideoSeedResult> videoResults = new ArrayList<>();
        private Map<String, String> errors = new LinkedHashMap<>();

        public void addVideoResult(String bvid, String topic, int comments, int highIntent) {
            VideoSeedResult result = new VideoSeedResult();
            result.setBvid(bvid);
            result.setTopic(topic);
            result.setCommentCount(comments);
            result.setHighIntentCount(highIntent);
            videoResults.add(result);
            totalComments += comments;
            totalHighIntent += highIntent;
        }

        public void addError(String bvid, String error) {
            errors.put(bvid, error);
        }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public int getTotalComments() { return totalComments; }
        public void setTotalComments(int totalComments) { this.totalComments = totalComments; }
        public int getTotalHighIntent() { return totalHighIntent; }
        public void setTotalHighIntent(int totalHighIntent) { this.totalHighIntent = totalHighIntent; }
        public int getLeadsGenerated() { return leadsGenerated; }
        public void setLeadsGenerated(int leadsGenerated) { this.leadsGenerated = leadsGenerated; }
        public List<VideoSeedResult> getVideoResults() { return videoResults; }
        public void setVideoResults(List<VideoSeedResult> videoResults) { this.videoResults = videoResults; }
        public Map<String, String> getErrors() { return errors; }
        public void setErrors(Map<String, String> errors) { this.errors = errors; }
    }

    public static class VideoSeedResult {
        private String bvid;
        private String topic;
        private int commentCount;
        private int highIntentCount;

        public String getBvid() { return bvid; }
        public void setBvid(String bvid) { this.bvid = bvid; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
        public int getHighIntentCount() { return highIntentCount; }
        public void setHighIntentCount(int highIntentCount) { this.highIntentCount = highIntentCount; }
    }
}
