package com.beijixing.social.crawl.collection.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("collection_item")
public class CollectionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("pool_id")
    private Long poolId;

    @TableField("item_id")
    private String itemId;

    @TableField("item_name")
    private String itemName;

    @TableField("item_url")
    private String itemUrl;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("item_type")
    private String itemType;

    @TableField("status")
    private Integer status;

    @TableField("source")
    private String source;

    @TableField("last_crawl_time")
    private LocalDateTime lastCrawlTime;

    @TableField("crawl_count")
    private Integer crawlCount;

    @TableField("tags")
    private String tags;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPoolId() { return poolId; }
    public void setPoolId(Long poolId) { this.poolId = poolId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemUrl() { return itemUrl; }
    public void setItemUrl(String itemUrl) { this.itemUrl = itemUrl; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getLastCrawlTime() { return lastCrawlTime; }
    public void setLastCrawlTime(LocalDateTime lastCrawlTime) { this.lastCrawlTime = lastCrawlTime; }
    public Integer getCrawlCount() { return crawlCount; }
    public void setCrawlCount(Integer crawlCount) { this.crawlCount = crawlCount; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
