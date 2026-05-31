package com.beijixing.social.crawl.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.social.crawl.collection.entity.UserCollectionPool;
import com.beijixing.social.crawl.collection.entity.CollectionItem;
import com.beijixing.social.crawl.collection.mapper.UserCollectionPoolMapper;
import com.beijixing.social.crawl.collection.mapper.CollectionItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCollectionPoolService {

    private final UserCollectionPoolMapper poolMapper;
    private final CollectionItemMapper itemMapper;

    public UserCollectionPool createPool(String name, String platformCode, Long userId, String description) {
        UserCollectionPool pool = new UserCollectionPool();
        pool.setName(name);
        pool.setPlatformCode(platformCode.toUpperCase());
        pool.setUserId(userId);
        pool.setDescription(description);
        pool.setItemType("ACCOUNT");
        pool.setStatus(1);
        pool.setItemCount(0);
        pool.setCreateTime(LocalDateTime.now());
        pool.setUpdateTime(LocalDateTime.now());
        
        poolMapper.insert(pool);
        log.info("创建用户采集池: userId={}, name={}, platform={}", userId, name, platformCode);
        return pool;
    }

    public List<UserCollectionPool> listPools(Long userId, String platformCode) {
        LambdaQueryWrapper<UserCollectionPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCollectionPool::getUserId, userId)
               .eq(UserCollectionPool::getStatus, 1);
        if (platformCode != null && !platformCode.isEmpty()) {
            wrapper.eq(UserCollectionPool::getPlatformCode, platformCode.toUpperCase());
        }
        wrapper.orderByDesc(UserCollectionPool::getUpdateTime);
        
        return poolMapper.selectList(wrapper);
    }

    public UserCollectionPool getPoolById(Long poolId, Long userId) {
        LambdaQueryWrapper<UserCollectionPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCollectionPool::getId, poolId)
               .eq(UserCollectionPool::getUserId, userId)
               .eq(UserCollectionPool::getStatus, 1);
        return poolMapper.selectOne(wrapper);
    }

    @Transactional
    public CollectionItem addAccountToPool(Long poolId, String accountId, String accountName, 
                                            String avatarUrl, Long userId, String source) {
        UserCollectionPool pool = getPoolById(poolId, userId);
        if (pool == null) {
            throw new RuntimeException("采集池不存在");
        }

        LambdaQueryWrapper<CollectionItem> existCheck = new LambdaQueryWrapper<>();
        existCheck.eq(CollectionItem::getPoolId, poolId)
                  .eq(CollectionItem::getItemId, accountId);
        if (itemMapper.selectCount(existCheck) > 0) {
            throw new RuntimeException("该账号已存在于采集池中");
        }

        CollectionItem item = new CollectionItem();
        item.setPoolId(poolId);
        item.setItemId(accountId);
        item.setItemName(accountName);
        item.setItemUrl(buildAccountUrl(pool.getPlatformCode(), accountId));
        item.setAvatarUrl(avatarUrl);
        item.setItemType("ACCOUNT");
        item.setStatus(1);
        item.setSource(source);
        item.setLastCrawlTime(null);
        item.setCreateTime(LocalDateTime.now());

        itemMapper.insert(item);

        pool.setItemCount(pool.getItemCount() + 1);
        pool.setUpdateTime(LocalDateTime.now());
        poolMapper.updateById(pool);

        log.info("添加账号到采集池: poolId={}, account={}", poolId, accountId);
        return item;
    }

    @Transactional
    public CollectionItem addLinkToPool(Long poolId, String linkUrl, String title, 
                                         Long userId, String source) {
        UserCollectionPool pool = getPoolById(poolId, userId);
        if (pool == null) {
            throw new RuntimeException("采集池不存在");
        }

        String itemId = java.util.UUID.randomUUID().toString().substring(0, 12);

        CollectionItem item = new CollectionItem();
        item.setPoolId(poolId);
        item.setItemId(itemId);
        item.setItemName(title != null ? title : extractTitleFromUrl(linkUrl));
        item.setItemUrl(linkUrl);
        item.setItemType("LINK");
        item.setStatus(1);
        item.setSource(source);
        item.setCreateTime(LocalDateTime.now());

        itemMapper.insert(item);

        pool.setItemCount(pool.getItemCount() + 1);
        pool.setUpdateTime(LocalDateTime.now());
        poolMapper.updateById(pool);

        log.info("添加链接到采集池: poolId={}, link={}", poolId, linkUrl);
        return item;
    }

    public List<CollectionItem> getPoolItems(Long poolId, Long userId, int page, int size) {
        UserCollectionPool pool = getPoolById(poolId, userId);
        if (pool == null) {
            return List.of();
        }

        LambdaQueryWrapper<CollectionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionItem::getPoolId, poolId)
               .eq(CollectionItem::getStatus, 1)
               .orderByDesc(CollectionItem::getCreateTime);

        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + ", " + size);

        return itemMapper.selectList(wrapper);
    }

    @Transactional
    public boolean removeItemFromPool(Long poolId, String itemId, Long userId) {
        UserCollectionPool pool = getPoolById(poolId, userId);
        if (pool == null) {
            return false;
        }

        LambdaQueryWrapper<CollectionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionItem::getPoolId, poolId)
               .eq(CollectionItem::getItemId, itemId);
        
        CollectionItem item = itemMapper.selectOne(wrapper);
        if (item != null) {
            item.setStatus(0);
            itemMapper.updateById(item);

            pool.setItemCount(Math.max(0, pool.getItemCount() - 1));
            pool.setUpdateTime(LocalDateTime.now());
            poolMapper.updateById(pool);

            log.info("从采集池移除项目: poolId={}, itemId={}", poolId, itemId);
            return true;
        }
        return false;
    }

    public List<CollectionItem> getItemsReadyForCrawling(String platformCode, int limit) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(6);
        
        LambdaQueryWrapper<CollectionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionItem::getStatus, 1)
               .and(w -> w.isNull(CollectionItem::getLastCrawlTime)
                         .or()
                         .le(CollectionItem::getLastCrawlTime, threshold))
               .last("LIMIT " + limit);

        return itemMapper.selectList(wrapper);
    }

    private String buildAccountUrl(String platformCode, String accountId) {
        switch (platformCode.toUpperCase()) {
            case "DOUYIN":
                return "https://www.douyin.com/user/" + accountId;
            case "XIAOHONGSHU":
                return "https://www.xiaohongshu.com/user/profile/" + accountId;
            case "KUAISHOU":
                return "https://www.kuaishou.com/profile/" + accountId;
            case "WEIBO":
                return "https://weibo.com/u/" + accountId;
            case "BILIBILI":
                return "https://space.bilibili.com/" + accountId;
            default:
                return "#" + accountId;
        }
    }

    private String extractTitleFromUrl(String url) {
        try {
            if (url.contains("/explore/") || url.contains("/note/")) {
                return "小红书笔记_" + url.substring(url.lastIndexOf("/") + 1);
            } else if (url.contains("/video/")) {
                return "抖音视频_" + url.substring(url.lastIndexOf("/") + 1);
            }
            return url.substring(url.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return "链接_" + System.currentTimeMillis() % 10000;
        }
    }
}
