package com.beijixing.social.crawl.engine;

import com.beijixing.social.crawl.entity.CrawlTask;
import com.beijixing.social.crawl.entity.SocialComment;

import java.util.List;

public interface PlatformCrawlerEngine {

    String getPlatformCode();

    String getPlatformName();

    CrawlTaskContext prepareContext(CrawlTask task);

    List<SocialComment> crawlComments(CrawlTaskContext context);

    void handleRateLimit(CrawlTaskContext context);

    boolean validateResponse(CrawlTaskContext context, Object response);

    default int getMaxConcurrentTasks() {
        return 3;
    }

    default long getDefaultRateLimitDelayMs() {
        return 2000;
    }

    default boolean supportsBatchCrawl() {
        return true;
    }
}
