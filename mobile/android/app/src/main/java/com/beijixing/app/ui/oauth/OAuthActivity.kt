package com.beijixing.app.ui.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.data.remote.ApiClient
import com.beijixing.app.data.remote.ApiService
import com.beijixing.app.data.remote.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class OAuthActivity : AppCompatActivity() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "OAuthActivity"

        fun start(context: Context, platform: String) {
            Log.i(TAG, "启动OAuth授权流程: platform=$platform")
            val intent = Intent(context, OAuthActivity::class.java)
            intent.putExtra("platform", platform)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            handleOAuthCallback()
        } catch (e: Exception) {
            Log.e(TAG, "处理OAuth回调异常", e)
            showError(this, "授权回调处理失败")
            finish()
        }
    }

    private fun handleOAuthCallback() {
        val uri = intent?.data

        if (uri == null) {
            Log.w(TAG, "未收到有效的Deep Link URI")
            showError(this, "无效的授权回调")
            finish()
            return
        }

        Log.i(TAG, "收到OAuth回调")

        val pathSegments = uri.pathSegments
        if (pathSegments.size < 3 || pathSegments[0] != "oauth" || pathSegments[1] != "callback") {
            Log.w(TAG, "无效的回调路径: ${uri.path}")
            showError(this, "无效的回调地址")
            finish()
            return
        }

        val platform = pathSegments[2].uppercase()
        Log.i(TAG, "识别平台: $platform")

        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")

        if (code.isNullOrEmpty()) {
            showError(this, "授权码缺失（可能用户取消授权）")
            finish()
            return
        }

        if (state.isNullOrEmpty()) {
            showError(this, "状态参数缺失")
            finish()
            return
        }

        Log.d(TAG, "回调参数: platform=$platform, code=***, state=***")
        exchangeCodeForTokens(platform, code, state, null)
    }

    private fun exchangeCodeForTokens(platform: String, code: String,
                                      state: String, codeVerifier: String?) {
        Log.i(TAG, "正在交换Token: platform=$platform")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(this@OAuthActivity, "正在完成授权...", Toast.LENGTH_SHORT).show()

                val response = withContext(Dispatchers.IO) {
                    apiClient.apiService.handleOAuthCallback(
                        platform = platform,
                        code = code,
                        state = state,
                        codeVerifier = codeVerifier
                    )
                }

                if (response.isSuccessful && response.body()?.isSuccess() == true) {
                    val oauthResult = response.body()!!.data!!
                    Log.i(TAG, "OAuth绑定成功: platform=$platform")

                    Toast.makeText(this@OAuthActivity, "绑定${getPlatformName(platform)}成功!", Toast.LENGTH_LONG).show()

                    val resultIntent = Intent().apply {
                        putExtra("account_id", oauthResult.accountId)
                        putExtra("platform", platform)
                        putExtra("nickname", oauthResult.nickname ?: "")
                        putExtra("avatar", oauthResult.avatar ?: "")
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    delay(1500)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Token交换失败"
                    Log.e(TAG, "OAuth token exchange failed")
                    showError(this@OAuthActivity, errorMsg)
                    delay(2000)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "网络请求异常", e)
                showError(this@OAuthActivity, "网络异常: ${e.message}")
                delay(2000)
                finish()
            }
        }
    }

    private fun showError(context: Context, message: String) {
        Log.e(TAG, "错误: $message")
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun getPlatformName(platform: String): String {
        return when (platform.uppercase()) {
            "DOUYIN" -> "抖音"
            "XIAOHONGSHU" -> "小红书"
            "KUAISHOU" -> "快手"
            "WECHAT" -> "微信"
            else -> platform
        }
    }
}
