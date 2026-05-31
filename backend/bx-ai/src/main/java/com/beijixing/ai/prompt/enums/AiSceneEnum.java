package com.beijixing.ai.prompt.enums;

import lombok.Getter;

/**
 * AI应用场景枚举
 * 所有场景支持扩展，新增场景仅需要添加枚举值即可，无需修改其他代码
 * 每个场景对应不同的最佳模型和提示词模板
 */
@Getter
public enum AiSceneEnum {

    // ==============================================
    // 营销获客类场景
    // ==============================================
    MARKETING_MOMENTS("marketing_moments", "朋友圈文案", "text"),
    MARKETING_SHORT_VIDEO("marketing_short_video", "短视频脚本", "text"),
    MARKETING_POSTER("marketing_poster", "海报文案", "text"),
    MARKETING_SOFT_ARTICLE("marketing_soft_article", "推广软文", "text"),
    MARKETING_BAIT("marketing_bait", "引流诱饵文案", "text"),

    // ==============================================
    // 客户沟通类场景
    // ==============================================
    CUSTOMER_FOLLOW_UP("customer_follow_up", "客户跟进话术", "text"),
    CUSTOMER_RETURN_VISIT("customer_return_visit", "回访话术", "text"),
    CUSTOMER_COMPLAINT("customer_complaint", "投诉处理模板", "text"),
    CUSTOMER_FESTIVAL_GREETING("customer_festival_greeting", "节日祝福模板", "text"),
    CUSTOMER_GROUP_SALE("customer_group_sale", "群发售话术", "text"),

    // ==============================================
    // 内容创作类场景
    // ==============================================
    CONTENT_OFFICIAL_ACCOUNT("content_official_account", "公众号文章", "text"),
    CONTENT_XIAOHONGSHU("content_xiaohongshu", "小红书种草文案", "text"),
    CONTENT_DOUYIN_SCRIPT("content_douyin_script", "抖音脚本", "text"),
    CONTENT_PRODUCT_DETAIL("content_product_detail", "产品详情页文案", "text"),
    CONTENT_BRAND_STORY("content_brand_story", "品牌故事", "text"),

    // ==============================================
    // 设计生成类场景（多模态/图片生成模型）
    // ==============================================
    DESIGN_POSTER("design_poster", "营销海报", "image"),
    DESIGN_VIDEO_COVER("design_video_cover", "短视频封面", "image"),
    DESIGN_PRODUCT_MAIN_IMAGE("design_product_main_image", "产品主图", "image"),
    DESIGN_MATERIAL("design_material", "宣传物料设计", "image"),

    // ==============================================
    // 数据报告类场景
    // ==============================================
    DATA_SALES_ANALYSIS("data_sales_analysis", "销售数据分析报告", "text"),
    DATA_USER_PORTRAIT("data_user_portrait", "用户画像分析", "text"),
    DATA_BUSINESS_REVIEW("data_business_review", "经营复盘报告", "text"),
    DATA_ACTIVITY_EFFECT("data_activity_effect", "活动效果分析", "text"),

    // ==============================================
    // 消息通知类场景
    // ==============================================
    MESSAGE_SMS("message_sms", "短信模板", "text"),
    MESSAGE_PUSH("message_push", "APP推送模板", "text"),
    MESSAGE_WORK_WECHAT("message_work_wechat", "企业微信通知模板", "text"),
    MESSAGE_MARKETING_NOTICE("message_marketing_notice", "营销活动通知模板", "text");

    /**
     * 场景编码，唯一标识，和数据库、配置中的场景编码对应
     */
    private final String code;
    /**
     * 场景名称，用于界面展示
     */
    private final String name;
    /**
     * 场景需要的模型类型：text文本/image图片/speech语音
     * 路由时会自动匹配支持该类型的模型
     */
    private final String type;

    AiSceneEnum(String code, String name, String type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    /**
     * 根据编码获取场景枚举
     * @param code 场景编码
     * @return 场景枚举，不存在返回null
     */
    public static AiSceneEnum getByCode(String code) {
        for (AiSceneEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
