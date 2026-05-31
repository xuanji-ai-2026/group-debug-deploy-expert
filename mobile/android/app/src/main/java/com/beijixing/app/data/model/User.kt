package com.beijixing.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户数据模型
 *
 * 对应后端用户服务（bx-user）的User实体
 * 产品需求对应：第四章 多租户用户体系
 *
 * @property userId        用户ID（雪花算法生成）
 * @property tenantId      租户ID（多租户隔离字段）
 * @property phone         手机号（登录凭证）
 * @property nickname      昵称
 * @property avatar        头像URL
 * @property role          角色：ADMIN（租户管理员）/ OPERATOR（普通操作员）
 * @property points        积分余额（核心计费体系 3.1节）
 * @property status        账号状态：NORMAL / FROZEN / DELETED
 * @property deviceCount   已绑定设备数量（4.3.2节，最多5台）
 * @property createTime    注册时间
 * @property updateTime    更新时间
 */
data class User(
    @SerializedName("id")
    val userId: Long = 0L,

    @SerializedName("tenantId")
    val tenantId: Long? = null,

    @SerializedName("phone")
    val phone: String = "",

    @SerializedName("nickname")
    val nickname: String = "",

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("roleType")
    val role: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("realName")
    val realName: String? = null,

    @SerializedName("status")
    val status: Int = 1,

    @SerializedName("lastLoginTime")
    val lastLoginTime: String? = null,

    @SerializedName("createdAt")
    val createTime: String = ""
) {
    fun isAdmin(): Boolean = role == "ADMIN" || role == "SUPER_ADMIN"

    fun isNormal(): Boolean = status == 1

    fun canBindDevice(): Boolean = true
}

/**
 * 登录请求体
 * 支持手机号+验证码、手机号+密码两种登录方式
 *
 * @property phone     手机号
 * @property code      验证码（验证码登录时必填）
 * @property password  密码（密码登录时必填，使用MD5+盐值加密）
 * @property deviceId  设备指纹（4.3.2节，用于设备管理）
 * @property loginType 登录类型：CODE（验证码）/ PASSWORD（密码）/ WECHAT（微信）
 */
data class LoginRequest(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("password")
    val password: String? = null,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("login_type")
    val loginType: String = "CODE"
)

/**
 * 登录响应体
 *
 * @property token     JWT访问令牌（有效期24小时）
 * @property refreshToken 刷新令牌（有效期7天）
 * @property expiresIn 令牌过期时间（秒）
 * @property user      用户信息
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String = "",

    @SerializedName("refresh_token")
    val refreshToken: String = "",

    @SerializedName("expires_in")
    val expiresIn: Long = 0L,

    @SerializedName("user")
    val user: User = User(),

    @SerializedName("isNewUser")
    val isNewUser: Boolean = false
)

/**
 * 发送验证码请求
 *
 * @property phone     手机号
 * @property type      验证码类型：LOGIN（登录）/ REGISTER（注册）/ RESET_PWD（重置密码）
 */
data class SendCodeRequest(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("type")
    val type: String = "LOGIN"
)

/**
 * 统一API响应包装类
 * 后端所有接口统一使用此格式返回
 *
 * @param T  data字段的实际数据类型
 * @property code   状态码：0=成功，非0=失败
 * @property msg    状态消息
 * @property data   业务数据
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val msg: String,

    @SerializedName("data")
    val data: T?
) {
    fun isSuccess(): Boolean = code == CODE_SUCCESS || code == CODE_SUCCESS_ZERO

    companion object {
        const val CODE_SUCCESS = 200
        const val CODE_SUCCESS_ZERO = 0
        const val CODE_ERROR = -1
    }
}

/**
 * 分页响应包装类
 *
 * @param T  列表项数据类型
 * @property list      数据列表
 * @property total     总记录数
 * @property page      当前页码（从1开始）
 * @property pageSize  每页记录数
 * @property pages     总页数
 */
data class PageResult<T>(
    @SerializedName("list")
    val list: List<T> = emptyList(),

    @SerializedName("total")
    val total: Long = 0L,

    @SerializedName("pageNum")
    val page: Int = 1,

    @SerializedName("pageSize")
    val pageSize: Int = 20,

    @SerializedName("pages")
    val pages: Int = 0
) {
    val records: List<T> get() = list
    val isLastPage: Boolean get() = page >= pages && pages > 0
}

/**
 * 应用版本信息（用于版本检查API返回）
 */
data class AppVersionInfo(
    @SerializedName("versionCode")
    val versionCode: Int = 0,

    @SerializedName("versionName")
    val versionName: String = "",

    @SerializedName("downloadUrl")
    val downloadUrl: String = "",

    @SerializedName("updateLog")
    val updateLog: String = "",

    @SerializedName("fileSize")
    val fileSize: Long = 0L,

    @SerializedName("md5")
    val md5: String = "",

    @SerializedName("isForceUpdate")
    val isForceUpdate: Boolean = false,

    @SerializedName("releaseDate")
    val releaseDate: String = "",

    @SerializedName("minSupportVersion")
    val minSupportVersion: Int = 0
)

/**
 * APK下载信息
 */
data class DownloadInfo(
    @SerializedName("downloadUrl")
    val downloadUrl: String = "",

    @SerializedName("fileName")
    val fileName: String = "",

    @SerializedName("fileSize")
    val fileSize: Long = 0L,

    @SerializedName("md5")
    val md5: String = ""
)
