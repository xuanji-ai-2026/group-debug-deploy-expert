package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * OAuth授权相关数据模型
 *
 * @author 北极星AI团队
 * @version 2.0 (2026-05-20)
 */

/**
 * 生成授权URL响应（后端返回）
 */
data class AuthorizationUrlResponse(
    @SerializedName("platform")
    val platform: String,           // 平台代码: DOUYIN/XIAOHONGSHU
    
    @SerializedName("authUrl")
    val authUrl: String,            // 完整的授权URL（移动端直接打开）
    
    @SerializedName("state")
    val state: String,              // 状态参数（回调时原样返回）
    
    @SerializedName("codeVerifier")
    val codeVerifier: String,       // PKCE验证码（⚠️ 必须安全保存）
    
    @SerializedName("expiresIn")
    val expiresIn: Int              // 授权URL有效期（秒）
)

/**
 * OAuth授权成功响应（后端返回）
 */
data class OAuthSuccessResult(
    @SerializedName("accountId")
    val accountId: Long,            // 社交账号ID（北极星系统内）
    
    @SerializedName("platform")
    val platform: String,           // 平台代码
    
    @SerializedName("nickname")
    val nickname: String?,          // 社交平台用户昵称
    
    @SerializedName("avatar")
    val avatar: String?,            // 头像URL
    
    @SerializedName("openId")
    val openId: String?,            // 社交平台OpenID
    
    @SerializedName("scopes")
    val scopes: List<String>?,      // 已授权的权限列表
    
    @SerializedName("bindTime")
    val bindTime: String?           // 绑定时间（ISO 8601格式）
)

/**
 * Token刷新响应
 */
data class RefreshTokenResponse(
    @SerializedName("accountId")
    val accountId: Long,
    
    @SerializedName("refreshedAt")
    val refreshedAt: String?,
    
    @SerializedName("message")
    val message: String?
)

/**
 * 已绑定的社交账号信息（用于展示在账号管理页面）
 */
data class BoundSocialAccount(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("platformCode")
    val platformCode: String,       // 平台代码
    
    @SerializedName("platformName")
    val platformName: String?,      // 平台名称（中文）
    
    @SerializedName("nickname")
    val nickname: String?,          // 社交平台昵称
    
    @SerializedName("avatar")
    val avatar: String?,            // 头像URL
    
    @SerializedName("openId")
    val openId: String?,
    
    @SerializedName("status")
    val status: Int,                // 状态: 1=正常, 0=已解绑, -1=Token过期
    
    @SerializedName("tokenExpireTime")
    val tokenExpireTime: String?,   // Token过期时间
    
    @SerializedName("boundAt")
    val boundAt: String?            // 绑定时间
) {
    /**
     * 获取平台显示名称
     */
    fun getPlatformDisplayName(): String {
        return when (platformCode.uppercase()) {
            "DOUYIN" -> "抖音"
            "XIAOHONGSHU" -> "小红书"
            "KUAISHOU" -> "快手"
            "WECHAT" -> "微信"
            "WEIBO" -> "微博"
            else -> platformCode
        }
    }
    
    /**
     * 判断账号是否有效
     */
    fun isActive(): Boolean = status == 1
    
    /**
     * 获取状态描述文字
     */
    fun getStatusDescription(): String {
        return when (status) {
            1 -> "✅ 正常"
            0 -> "❌ 已解绑"
            -1 -> "⚠️ Token已过期"
            else -> "未知状态"
        }
    }
}
