import Foundation

struct CrawlTask: Codable, Identifiable {
    let id: Int64?
    var taskName: String?
    var taskType: String?
    var platformCode: String?
    var targetId: String?
    var keywords: String?
    var maxCrawlCount: Int?
    var crawlIntervalSeconds: Int?
    var totalCommentsFound: Int?
    var highIntentCount: Int?
    var leadsGenerated: Int?
    var messagesSent: Int?
    var status: Int?
    var progressPercent: Int?
    var errorMessage: String?
    var startTime: String?
    var endTime: String?
    var createTime: String?
    var updateTime: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case taskName = "task_name"
        case taskType = "task_type"
        case platformCode = "platform_code"
        case targetId = "target_id"
        case keywords
        case maxCrawlCount = "max_crawl_count"
        case crawlIntervalSeconds = "crawl_interval_seconds"
        case totalCommentsFound = "total_comments_found"
        case highIntentCount = "high_intent_count"
        case leadsGenerated = "leads_generated"
        case messagesSent = "messages_sent"
        case status
        case progressPercent = "progress_percent"
        case errorMessage = "error_message"
        case startTime = "start_time"
        case endTime = "end_time"
        case createTime = "create_time"
        case updateTime = "update_time"
    }
    
    var platformDisplayName: String {
        switch platformCode?.uppercased() {
        case "DOUYIN":
            return "抖音"
        case "XIAOHONGSHU":
            return "小红书"
        case "KUAISHOU":
            return "快手"
        case "WEIBO":
            return "微博"
        case "BILIBILI":
            return "B站"
        default:
            return platformCode ?? "未知"
        }
    }
    
    var statusText: String {
        switch status {
        case 0:
            return "待执行"
        case 1:
            return "进行中"
        case 2:
            return "已完成"
        case 3:
            return "失败"
        default:
            return "未知"
        }
    }
}

struct SocialComment: Codable, Identifiable {
    let id: Int64?
    var commentId: String?
    var crawlTaskId: Int64?
    var platformCode: String?
    var contentId: String?
    var authorId: String?
    var authorName: String?
    var authorAvatar: String?
    var authorBio: String?
    var userFollowerCount: Int64?
    var userFollowingCount: Int64?
    var userVerified: Bool?
    var commentText: String?
    var likeCount: Int?
    var replyCount: Int?
    var publishTime: String?
    var extractedPhone: String?
    var extractedWechat: String?
    var hasPhoneContact: Bool?
    var hasWechatContact: Bool?
    var aiIntentScore: Int?
    var aiIntentLevel: String?
    var isHighIntent: Bool?
    var aiAnalysisResult: String?
    var leadGenerated: Bool?
    var generatedLeadId: Int64?
    var messageSent: Bool?
    var messageSentTime: String?
    var deleted: Int?
    var createTime: String?
    var updateTime: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case commentId = "comment_id"
        case crawlTaskId = "crawl_task_id"
        case platformCode = "platform_code"
        case contentId = "content_id"
        case authorId = "author_id"
        case authorName = "author_name"
        case authorAvatar = "author_avatar"
        case authorBio = "author_bio"
        case userFollowerCount = "user_follower_count"
        case userFollowingCount = "user_following_count"
        case userVerified = "user_verified"
        case commentText = "comment_text"
        case likeCount = "like_count"
        case replyCount = "reply_count"
        case publishTime = "publish_time"
        case extractedPhone = "extracted_phone"
        case extractedWechat = "extracted_wechat"
        case hasPhoneContact = "has_phone_contact"
        case hasWechatContact = "has_wechat_contact"
        case aiIntentScore = "ai_intent_score"
        case aiIntentLevel = "ai_intent_level"
        case isHighIntent = "is_high_intent"
        case aiAnalysisResult = "ai_analysis_result"
        case leadGenerated = "lead_generated"
        case generatedLeadId = "generated_lead_id"
        case messageSent = "message_sent"
        case messageSentTime = "message_sent_time"
        case deleted
        case createTime = "create_time"
        case updateTime = "update_time"
    }
    
    var displayIntentLevel: String {
        switch aiIntentLevel?.uppercased() {
        case "A":
            return "A级-超高意向"
        case "B":
            return "B级-高意向"
        case "C":
            return "C级-中意向"
        case "D":
            return "D级-低意向"
        default:
            return aiIntentLevel ?? "未分析"
        }
    }
}

struct MessageTemplate: Codable, Identifiable {
    let id: Int64?
    var templateName: String?
    var platformCode: String?
    var intentLevel: String?
    var templateContent: String?
    var templateVariables: String?
    var aiGenerated: Bool?
    var isDefault: Bool?
    var useCount: Int?
    var successRate: Double?
    var replyRate: Double?
    var status: Int?
    var createTime: String?
    var updateTime: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case templateName = "template_name"
        case platformCode = "platform_code"
        case intentLevel = "intent_level"
        case templateContent = "template_content"
        case templateVariables = "template_variables"
        case aiGenerated = "ai_generated"
        case isDefault = "is_default"
        case useCount = "use_count"
        case successRate = "success_rate"
        case replyRate = "reply_rate"
        case status
        case createTime = "create_time"
        case updateTime = "update_time"
    }
}

struct MessageResult: Codable {
    let success: Bool
    let commentId: Int64?
    let platformCode: String?
    let authorId: String?
    let authorName: String?
    let messageContent: String?
    let templateId: Int64?
    let templateName: String?
    let errorMessage: String?
    let sendTime: String?
    
    enum CodingKeys: String, CodingKey {
        case success
        case commentId = "comment_id"
        case platformCode = "platform_code"
        case authorId = "author_id"
        case authorName = "author_name"
        case messageContent = "message_content"
        case templateId = "template_id"
        case templateName = "template_name"
        case errorMessage = "error_message"
        case sendTime = "send_time"
    }
}

struct LeadResult: Codable {
    let success: Bool
    let generatedCount: Int?
    let skippedCount: Int?
    let errors: [String: String]?
    let generatedLeads: [Lead]?
}

struct Lead: Codable, Identifiable {
    let id: Int64?
    var title: String?
    var source: String?
    var channel: String?
    var customerName: String?
    var customerPhone: String?
    var level: String?
    var intentScore: Int?
    var requirementDesc: String?
    var status: String?
    var createTime: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case source
        case channel
        case customerName = "customer_name"
        case customerPhone = "customer_phone"
        case level
        case intentScore = "intent_score"
        case requirementDesc = "requirement_desc"
        case status
        case createTime = "create_time"
    }
}
