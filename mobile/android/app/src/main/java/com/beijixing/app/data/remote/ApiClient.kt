package com.beijixing.app.data.remote

import android.util.Log
import com.beijixing.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    companion object {
        private const val TAG = "ApiClient"

        const val BASE_URL = "https://www.beijixing-ai.com/api/"
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(createAuthInterceptor())
            .addInterceptor(createTokenRefreshInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .addInterceptor(createNetworkDiagnosticInterceptor())
            .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
    }

    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        if (BuildConfig.DEBUG) Log.d(TAG, "Initializing ApiService...")
        createRetrofit().create(ApiService::class.java)
    }

    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            try {
                val token = preferencesManager.getToken()

                if (!token.isNullOrEmpty()) {
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .header("Content-Type", "application/json")
                        .method(originalRequest.method, originalRequest.body)
                        .build()

                    if (BuildConfig.DEBUG) Log.d(TAG, "Adding auth header to request")
                    chain.proceed(authenticatedRequest)
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "No token available, proceeding without auth")
                    chain.proceed(originalRequest)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Failed to get token for auth header: ${e.message}")
                chain.proceed(originalRequest)
            }
        }
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return try {
            HttpLoggingInterceptor { message ->
                if (!BuildConfig.DEBUG) return@HttpLoggingInterceptor
                when {
                    message.startsWith("{") || message.startsWith("[") -> {
                        Log.d(TAG, message)
                    }
                    message.contains("token", ignoreCase = true) -> {
                        Log.d(TAG, "*** SENSITIVE DATA ***")
                    }
                    else -> {
                        Log.d(TAG, message)
                    }
                }
            }.apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to create logging interceptor: ${e.message}")
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    fun cleanup() {
        if (BuildConfig.DEBUG) Log.i(TAG, "Cleaning up ApiClient resources...")
    }

    private fun createTokenRefreshInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (response.code == 401 && request.header("Authorization") != null) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Received 401, attempting token refresh...")

                synchronized(this) {
                    val currentToken = preferencesManager.getToken()
                    if (currentToken.isNullOrEmpty()) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "No token available for refresh, login required")
                        onAuthFailed()
                        return@Interceptor response
                    }

                    val refreshToken = preferencesManager.getRefreshToken()
                    if (refreshToken.isNullOrEmpty()) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "No refresh token, login required")
                        clearToken()
                        onAuthFailed()
                        return@Interceptor response
                    }

                    try {
                        val refreshResponse = apiService.refreshTokenSync(
                            com.beijixing.app.data.model.RefreshTokenRequest(refreshToken)
                        )

                        if (refreshResponse.isSuccessful && refreshResponse.body()?.isSuccess() == true) {
                            val loginData = refreshResponse.body()!!.data!!
                            saveToken(loginData.token)
                            preferencesManager.saveRefreshToken(loginData.refreshToken)

                            if (BuildConfig.DEBUG) Log.i(TAG, "Token refresh succeeded, retrying request...")

                            val newRequest = request.newBuilder()
                                .header("Authorization", "Bearer ${loginData.token}")
                                .build()

                            response.close()
                            return@Interceptor chain.proceed(newRequest)
                        } else {
                            if (BuildConfig.DEBUG) Log.e(TAG, "Token refresh failed")
                            clearToken()
                            preferencesManager.clearRefreshToken()
                            onAuthFailed()
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Token refresh exception: ${e.javaClass.simpleName}")
                        clearToken()
                        preferencesManager.clearRefreshToken()
                        onAuthFailed()
                    }
                }
            }

            response
        }
    }

    private fun onAuthFailed() {
        if (BuildConfig.DEBUG) Log.e(TAG, "Auth failed, redirecting to login")
    }

    private fun createNetworkDiagnosticInterceptor(): Interceptor {
        return Interceptor { chain ->
            if (!BuildConfig.DEBUG) {
                return@Interceptor chain.proceed(chain.request())
            }

            val request = chain.request()
            val url = request.url.toString()
            val method = request.method

            Log.d(TAG, "╔══════════════════════════════════════╗")
            Log.d(TAG, "║ 🌐 网络请求开始                        ║")
            Log.d(TAG, "╠══════════════════════════════════════╣")
            Log.d(TAG, "║ 方法: $method")
            Log.d(TAG, "║ URL: $url")
            Log.d(TAG, "║ 请求头: ${request.headers.names().joinToString(", ")}")

            val requestBody = request.body
            if (requestBody != null) {
                val contentType = requestBody.contentType()
                val contentLength = requestBody.contentLength()
                Log.d(TAG, "║ Content-Type: $contentType")
                Log.d(TAG, "║ Content-Length: ${contentLength} bytes")
            }
            Log.d(TAG, "╚══════════════════════════════════════╝")

            val startTime = System.currentTimeMillis()

            try {
                val response = chain.proceed(request)
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                Log.d(TAG, "╔══════════════════════════════════════╗")
                Log.d(TAG, "║ ✅ 网络响应成功                       ║")
                Log.d(TAG, "╠══════════════════════════════════════╣")
                Log.d(TAG, "║ 状态码: ${response.code} (${response.message})")
                Log.d(TAG, "║ 耗时: ${duration}ms")
                Log.d(TAG, "║ 响应头: ${response.headers.names().joinToString(", ")}")

                val responseBody = response.body
                if (responseBody != null) {
                    val contentLength = responseBody.contentLength()
                    Log.d(TAG, "║ 响应体大小: ${if (contentLength > 0) "$contentLength bytes" else "未知"}")
                }

                when (response.code) {
                    in 200..299 -> Log.d(TAG, "║ 状态: ✅ 请求成功")
                    in 300..399 -> Log.w(TAG, "║ 状态: ⚠️ 重定向 (${response.code})")
                    401 -> Log.e(TAG, "║ 状态: ❌ 未授权")
                    403 -> Log.e(TAG, "║ 状态: ❌ 禁止访问")
                    404 -> Log.e(TAG, "║ 状态: ❌ 资源不存在")
                    in 400..499 -> Log.e(TAG, "║ 状态: ❌ 客户端错误 (${response.code})")
                    500 -> Log.e(TAG, "║ 状态: ❌ 服务器内部错误")
                    in 500..599 -> Log.e(TAG, "║ 状态: ❌ 服务器错误 (${response.code})")
                    else -> Log.w(TAG, "║ 状态: ❓ 未知状态码 (${response.code})")
                }

                Log.d(TAG, "╚══════════════════════════════════════╝")

                response

            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                Log.e(TAG, "╔══════════════════════════════════════╗")
                Log.e(TAG, "║ ❌ 网络请求失败                       ║")
                Log.e(TAG, "╠══════════════════════════════════════╣")
                Log.e(TAG, "║ 耗时: ${duration}ms")
                Log.e(TAG, "║ 请求URL: $url")
                Log.e(TAG, "║ 错误类型: ${e.javaClass.simpleName}")
                Log.e(TAG, "║ 错误消息: ${e.message}")
                Log.e(TAG, "║ 异常堆栈: ${e.stackTraceToString().take(500)}")
                Log.e(TAG, "╠══════════════════════════════════════╣")
                Log.e(TAG, "║ 🔍 诊断信息：                         ║")

                when (e) {
                    is java.net.SocketTimeoutException -> {
                        Log.e(TAG, "║ 类型: 连接超时/读取超时               ║")
                        Log.e(TAG, "║ 可能原因:                           ║")
                        Log.e(TAG, "║   1. 服务器响应缓慢                  ║")
                        Log.e(TAG, "║   2. 网络不稳定（信号弱/丢包）        ║")
                        Log.e(TAG, "║   3. 防火墙或代理限制                ║")
                        Log.e(TAG, "║   4. 服务器负载过高                  ║")
                        Log.e(TAG, "║ 建议: 检查网络连接，稍后重试          ║")
                    }
                    is java.net.UnknownHostException -> {
                        Log.e(TAG, "║ 类型: DNS解析失败                     ║")
                        Log.e(TAG, "║ 可能原因:                           ║")
                        Log.e(TAG, "║   1. 域名/IP地址配置错误             ║")
                        Log.e(TAG, "║   2. DNS服务器不可用                 ║")
                        Log.e(TAG, "║   3. 设备未连接网络                  ║")
                        Log.e(TAG, "║   4. Hosts文件配置问题               ║")
                        Log.e(TAG, "║ 建议: 检查BASE_URL配置和网络连接      ║")
                    }
                    is java.net.ConnectException -> {
                        Log.e(TAG, "║ 类型: 连接被拒绝                      ║")
                        Log.e(TAG, "║ 可能原因:                           ║")
                        Log.e(TAG, "║   1. 服务未启动或端口错误             ║")
                        Log.e(TAG, "║   2. IP地址错误或服务器宕机           ║")
                        Log.e(TAG, "║   3. 防火墙阻止连接                  ║")
                        Log.e(TAG, "║   4. 服务器达到最大连接数             ║")
                        Log.e(TAG, "║ 建议: 检查服务端状态和端口配置        ║")
                    }
                    is javax.net.ssl.SSLException -> {
                        Log.e(TAG, "║ 类型: SSL/TLS握手失败                 ║")
                        Log.e(TAG, "║ 可能原因:                           ║")
                        Log.e(TAG, "║   1. 证书过期或不受信任               ║")
                        Log.e(TAG, "║   2. 协议版本不匹配                  ║")
                        Log.e(TAG, "║   3. 中间人攻击（证书被篡改）         ║")
                        Log.e(TAG, "║ 建议: 检查网络安全配置和证书有效性    ║")
                    }
                    is java.io.IOException -> {
                        Log.e(TAG, "║ 类型: IO异常                          ║")
                        Log.e(TAG, "║ 可能原因:                           ║")
                        Log.e(TAG, "║   1. 网络中断                         ║")
                        Log.e(TAG, "║   2. 缓冲区溢出                       ║")
                        Log.e(TAG, "║   3. 连接被重置                       ║")
                        Log.e(TAG, "║ 建议: 检查网络稳定性和内存使用情况    ║")
                    }
                    else -> {
                        Log.e(TAG, "║ 类型: 未知异常                        ║")
                        Log.e(TAG, "║ 建议: 查看完整日志并联系技术支持       ║")
                    }
                }

                Log.e(TAG, "╚══════════════════════════════════════╝")

                throw e
            }
        }
    }

    fun saveToken(token: String) {
        preferencesManager.saveToken(token)
        if (BuildConfig.DEBUG) Log.d(TAG, "Token saved successfully")
    }

    fun saveRefreshToken(refreshToken: String) {
        preferencesManager.saveRefreshToken(refreshToken)
        if (BuildConfig.DEBUG) Log.d(TAG, "RefreshToken saved successfully")
    }

    fun clearToken() {
        preferencesManager.clearToken()
        if (BuildConfig.DEBUG) Log.d(TAG, "Token cleared")
    }
}
