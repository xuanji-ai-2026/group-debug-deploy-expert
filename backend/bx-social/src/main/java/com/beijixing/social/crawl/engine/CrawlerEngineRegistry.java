package com.beijixing.social.crawl.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CrawlerEngineRegistry {

    private final Map<String, PlatformCrawlerEngine> engines = new ConcurrentHashMap<>();

    public void registerEngine(PlatformCrawlerEngine engine) {
        engines.put(engine.getPlatformCode().toUpperCase(), engine);
    }

    public PlatformCrawlerEngine getEngine(String platformCode) {
        return engines.get(platformCode.toUpperCase());
    }

    public boolean hasEngine(String platformCode) {
        return engines.containsKey(platformCode.toUpperCase());
    }

    public Map<String, PlatformCrawlerEngine> getAllEngines() {
        return Map.copyOf(engines);
    }
}
