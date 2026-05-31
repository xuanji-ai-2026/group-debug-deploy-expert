package com.beijixing.app.ui.splash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.beijixing.app.BuildConfig
import com.beijixing.app.R
import com.beijixing.app.data.model.AppVersionInfo
import com.beijixing.app.data.remote.ApiClient
import com.beijixing.app.ui.login.LoginActivity
import com.beijixing.app.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    private companion object {
        private const val TAG = "SplashActivity"
    }
    
    private lateinit var texts: Array<TextViewInfo>
    
    @javax.inject.Inject
    lateinit var apiClient: ApiClient
    
    data class TextViewInfo(
        val viewId: Int,
        val delay: Long,
        val text: String
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            
            initTexts()
            startAnimations()
            setupEnterButton()
            
            android.util.Log.i(TAG, "✅ 启动页初始化完成 - 版本 v${BuildConfig.VERSION_NAME}")
            
            // 🚀 启动时自动检查新版本
            checkForUpdateInBackground()
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "启动页初始化失败", e)
            navigateToMain()
        }
    }
    
    /**
     * 后台静默检查新版本（不阻塞UI）
     */
    private fun checkForUpdateInBackground() {
        AppLogger.i(TAG, "🔍 开始后台检查新版本...")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentVersionCode = BuildConfig.VERSION_CODE
                val currentVersionName = BuildConfig.VERSION_NAME
                
                AppLogger.i(TAG, "当前版本信息：$currentVersionName ($currentVersionCode)")
                
                // 调用API检查新版本
                val response = apiClient.apiService.checkAppVersion(
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName,
                    platform = "android"
                )
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.isSuccess() && apiResponse.data != null) {
                            val versionInfo = apiResponse.data!!
                            
                            AppLogger.i(TAG, "✅ 版本检查完成 - 最新版: ${versionInfo.versionName}")
                            
                            if (versionInfo.versionCode > currentVersionCode) {
                                AppLogger.i(TAG, "🎉 发现新版本！准备显示更新提示")
                                
                                showUpdateDialog(versionInfo)
                            } else {
                                AppLogger.i(TAG, "ℹ️ 当前已是最新版本")
                            }
                        } else {
                            AppLogger.w(TAG, "⚠️ 版本检查返回异常：${apiResponse.msg}")
                            AppLogger.w(TAG, "ℹ️ 生产环境：不显示更新弹窗，直接进入应用")
                        }
                    } else if (response.code() == 401) {
                        AppLogger.e(TAG, "❌ Token已过期或无效 (HTTP 401)，需要重新登录")
                        apiClient.clearToken()
                        
                        val intent = Intent(this@SplashActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        AppLogger.w(TAG, "⚠️ 版本检查请求失败：HTTP ${response.code()}")
                        AppLogger.w(TAG, "ℹ️ 非认证错误，跳过版本检查，直接进入应用")
                    }
                }
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "❌ 版本检查异常", e)
                AppLogger.e(TAG, "ℹ️ 生产环境：版本检查失败，直接进入应用")
            }
        }
    }
    
    /**
     * 显示更新提示对话框
     */
    private fun showUpdateDialog(versionInfo: AppVersionInfo) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_version_update, null)
            
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(!versionInfo.isForceUpdate)
            
            val dialog = builder.create()
            
            if (dialog.window != null) {
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            
            // 填充版本信息
            dialogView.findViewById<TextView>(R.id.tvUpdateTitle)?.text = 
                "发现新版本 v${versionInfo.versionName}"
            
            dialogView.findViewById<TextView>(R.id.tvCurrentVersion)?.text = 
                "v${BuildConfig.VERSION_NAME}"
            
            dialogView.findViewById<TextView>(R.id.tvNewVersion)?.text = 
                "v${versionInfo.versionName}"
            
            dialogView.findViewById<TextView>(R.id.tvUpdateSize)?.text = 
                "📦 更新大小：${formatFileSize(versionInfo.fileSize)}"
            
            dialogView.findViewById<TextView>(R.id.tvReleaseDate)?.text = 
                "📅 ${versionInfo.releaseDate}"
            
            // 填充更新日志
            dialogView.findViewById<TextView>(R.id.tvMajorUpdates)?.text = 
                extractSection(versionInfo.updateLog, "重大更新") ?: "• 全新UI设计\n• 性能优化"
            
            dialogView.findViewById<TextView>(R.id.tvFixes)?.text = 
                extractSection(versionInfo.updateLog, "问题修复") ?: "• 修复已知问题"
            
            dialogView.findViewById<TextView>(R.id.tvFeatures)?.text = 
                extractSection(versionInfo.updateLog, "新增功能") ?: "• 新增功能特性"
            
            // 强制更新提示
            if (versionInfo.isForceUpdate) {
                dialogView.findViewById<TextView>(R.id.tvForceUpdateHint)?.visibility = 
                    android.view.View.VISIBLE
                
                dialogView.findViewById<TextView>(R.id.tvSkipVersion)?.visibility = 
                    android.view.View.GONE
            }
            
            // 立即更新按钮
            dialogView.findViewById<Button>(R.id.btnConfirmUpdate)?.setOnClickListener {
                AppLogger.i(TAG, "→ 用户点击了'立即更新'按钮")
                
                try {
                    dialog.dismiss()
                    
                    // 显示下载进度对话框
                    val progressDialog = android.app.ProgressDialog(this@SplashActivity).apply {
                        setTitle("🚀 正在下载新版本")
                        setMessage("准备下载 ${versionInfo.versionName}...")
                        setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
                        setMax(100)
                        setProgress(0)
                        setCancelable(false)
                        setButton(android.content.DialogInterface.BUTTON_NEGATIVE, "后台运行") { _, _ ->
                            AppLogger.i(TAG, "用户选择后台下载")
                            dismiss()
                        }
                        show()
                    }
                    
                    // 转换为AppUpdateManager所需的格式
                    val updateManagerInfo = com.beijixing.app.util.AppUpdateManager.UpdateInfo(
                        hasUpdate = true,
                        versionCode = versionInfo.versionCode,
                        versionName = versionInfo.versionName,
                        downloadUrl = versionInfo.downloadUrl.ifEmpty { "https://www.beijixing-ai.com/api/app/download" },
                        changelog = versionInfo.updateLog,
                        isForceUpdate = versionInfo.isForceUpdate,
                        apkSize = versionInfo.fileSize
                    )
                    
                    // 启动下载和安装（带进度UI）
                    val callback = object : com.beijixing.app.util.AppUpdateManager.UpdateCallback {
                        override fun onCheckComplete(updateInfo: com.beijixing.app.util.AppUpdateManager.UpdateInfo) {}
                        override fun onDownloadProgress(progress: Int) {
                            runOnUiThread {
                                if (progressDialog.isShowing) {
                                    progressDialog.progress = progress
                                    when (progress) {
                                        0 -> progressDialog.setMessage("正在连接服务器...")
                                        in 1..30 -> progressDialog.setMessage("正在下载 ${versionInfo.versionName}... ($progress%)")
                                        in 31..70 -> progressDialog.setMessage("下载中... ($progress%)\n请保持网络畅通")
                                        in 71..99 -> progressDialog.setMessage("即将完成... ($progress%)")
                                        100 -> {
                                            progressDialog.setMessage("✅ 下载完成！准备安装...")
                                        }
                                    }
                                }
                            }
                        }
                        override fun onDownloadSuccess(apkFile: java.io.File) {
                            runOnUiThread {
                                if (progressDialog.isShowing) {
                                    progressDialog.dismiss()
                                }
                                com.beijixing.app.util.AppUpdateManager.installApk(this@SplashActivity, apkFile)
                            }
                        }
                        override fun onError(error: String) {
                            runOnUiThread {
                                android.widget.Toast.makeText(
                                    this@SplashActivity,
                                    "更新失败：$error",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                if (progressDialog.isShowing) {
                                    progressDialog.dismiss()
                                }
                            }
                        }
                    }
                    
                    com.beijixing.app.util.AppUpdateManager.startDownload(
                        context = this@SplashActivity,
                        updateInfo = updateManagerInfo,
                        callback = callback
                    )
                    
                } catch (e: Exception) {
                    AppLogger.e(TAG, "❌ 启动下载失败", e)
                    android.widget.Toast.makeText(
                        this@SplashActivity,
                        "启动更新失败：" + (e.message ?: "未知错误"),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            // 暂时跳过按钮
            dialogView.findViewById<TextView>(R.id.tvSkipVersion)?.setOnClickListener {
                AppLogger.i(TAG, "→ 用户选择了'暂时跳过'")
                dialog.dismiss()
            }
            
            dialog.show()
            
            AppLogger.i(TAG, "✅ 版本更新弹窗已显示")
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "❌ 显示更新弹窗失败", e)
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    /**
     * 从更新日志中提取指定部分内容
     */
    private fun extractSection(updateLog: String, sectionName: String): String? {
        val regex = Regex("""【$sectionName】\s*\n([\s\S]*?)(?=【|$)""")
        return regex.find(updateLog)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
    
    /**
     * 装饰圆浮动动画（增强视觉效果）
     */
    private fun startDecorCircleAnimations() {
        val handler = Handler(Looper.getMainLooper())
        
        // 装饰圆1 - 右上角大圆：缓慢上下浮动
        findViewById<View>(R.id.decorCircle1)?.let { circle ->
            val floatAnim = TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.03f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, -0.03f
            ).apply {
                duration = 3500
                repeatCount = AlphaAnimation.INFINITE
                repeatMode = TranslateAnimation.REVERSE
                fillAfter = true
            }
            
            handler.postDelayed({ circle.startAnimation(floatAnim) }, 300)
        }
        
        // 装饰圆2 - 左侧中圆：左右轻微摆动
        findViewById<View>(R.id.decorCircle2)?.let { circle ->
            val swayAnim = TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, -0.02f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.02f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f
            ).apply {
                duration = 4000
                repeatCount = AlphaAnimation.INFINITE
                repeatMode = TranslateAnimation.REVERSE
                fillAfter = true
            }
            
            handler.postDelayed({ circle.startAnimation(swayAnim) }, 600)
        }
        
        // 装饰圆3 - 右下角小圆：旋转+缩放组合动画
        findViewById<View>(R.id.decorCircle3)?.let { circle ->
            val rotateAnim = RotateAnimation(
                0f, 360f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 15000
                repeatCount = AlphaAnimation.INFINITE
                fillAfter = true
            }
            
            val scalePulse = ScaleAnimation(
                0.9f, 1.15f, 0.9f, 1.15f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 2500
                repeatCount = AlphaAnimation.INFINITE
                repeatMode = ScaleAnimation.REVERSE
                fillAfter = true
            }
            
            handler.postDelayed({
                circle.startAnimation(rotateAnim)
                circle.startAnimation(scalePulse)
            }, 900)
        }
        
        AppLogger.d(TAG, "✨ 装饰圆动画已启动")
    }
    
    private fun initTexts() {
        texts = arrayOf(
            TextViewInfo(R.id.tvLine1, 0L, "🚀 智能获客，高效转化；"),
            TextViewInfo(R.id.tvLine2, 1L, "🎯 洞察全网潜在客源"),
            TextViewInfo(R.id.tvLine3, 2L, "🔍 锁定行业优质商机"),
            TextViewInfo(R.id.tvLine4, 3L, "📨 智能分发精准触达"),
            TextViewInfo(R.id.tvLine5, 4L, "⚡ 自动运营高效流转"),
            TextViewInfo(R.id.tvLine6, 5L, "💡 极简赋能商业拓客"),
            TextViewInfo(R.id.tvLine7, 6L, "🌟 全域驱动业绩倍增")
        )
        
        findViewById<TextView>(R.id.tvMainTitle)?.text = "北极星 AI 商机获客系统"
        findViewById<TextView>(R.id.tvVersion)?.text = "Version ${BuildConfig.VERSION_NAME}"
    }
    
    private fun startAnimations() {
        val handler = Handler(Looper.getMainLooper())
        
        // 0. 装饰圆浮动动画（增强视觉效果）
        startDecorCircleAnimations()
        
        // 1. Logo光晕脉冲效果
        findViewById<ImageView>(R.id.ivLogoGlow)?.let { glow ->
            val pulseAnim = ScaleAnimation(
                1f, 1.2f, 1f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1800
                repeatCount = AlphaAnimation.INFINITE
                repeatMode = ScaleAnimation.REVERSE
                fillAfter = true
            }
            
            glow.startAnimation(pulseAnim)
            
            // 光晕透明度呼吸效果
            val alphaBreath = AlphaAnimation(0.2f, 0.5f).apply {
                duration = 2000
                repeatCount = AlphaAnimation.INFINITE
                repeatMode = AlphaAnimation.REVERSE
                fillAfter = true
            }
            glow.startAnimation(alphaBreath)
        }
        
        // 2. Logo缩放淡入 + 轻微旋转
        findViewById<ImageView>(R.id.ivLogo)?.let { logo ->
            logo.alpha = 1f
            
            val fadeIn = AlphaAnimation(0f, 1f).apply { 
                duration = 1000; 
                fillAfter = true 
            }
            
            val scaleUp = ScaleAnimation(
                0.2f, 1f, 0.2f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply { 
                duration = 1200; 
                fillAfter = true 
            }
            
            val rotateIn = RotateAnimation(
                -15f, 0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply { 
                duration = 1000; 
                fillAfter = true 
            }
            
            logo.startAnimation(fadeIn)
            logo.startAnimation(scaleUp)
            logo.startAnimation(rotateIn)
        }
        
        // 3. 主标题打字机效果 + 上浮
        handler.postDelayed({
            findViewById<TextView>(R.id.tvMainTitle)?.let { title ->
                title.alpha = 1f
                
                val fadeIn = AlphaAnimation(0f, 1f).apply { 
                    duration = 800; 
                    fillAfter = true 
                }
                
                val slideUp = TranslateAnimation(
                    TranslateAnimation.RELATIVE_TO_SELF, 0f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0.08f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0f
                ).apply { 
                    duration = 900; 
                    fillAfter = true 
                }
                
                title.startAnimation(fadeIn)
                title.startAnimation(slideUp)
            }
        }, 600)
        
        // 4. 副标题淡入
        handler.postDelayed({
            findViewById<TextView>(R.id.tvLine1)?.let { subtitle ->
                subtitle.alpha = 1f
                
                val fadeIn = AlphaAnimation(0f, 1f).apply { 
                    duration = 700; 
                    fillAfter = true 
                }
                
                val slideUp = TranslateAnimation(
                    TranslateAnimation.RELATIVE_TO_SELF, 0f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0.04f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0f
                ).apply { 
                    duration = 600; 
                    fillAfter = true 
                }
                
                subtitle.startAnimation(fadeIn)
                subtitle.startAnimation(slideUp)
            }
        }, 1100)
        
        // 5. 卡片容器弹性缩放淡入
        handler.postDelayed({
            findViewById<CardView>(R.id.cardContent)?.let { card ->
                card.alpha = 1f
                
                val fadeIn = AlphaAnimation(0f, 1f).apply { 
                    duration = 1000; 
                    fillAfter = true 
                }
                
                val scaleBounce = ScaleAnimation(
                    0.8f, 1.05f, 0.8f, 1.05f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                ).apply { 
                    duration = 800; 
                    fillAfter = true 
                }
                
                card.startAnimation(fadeIn)
                card.startAnimation(scaleBounce)
                
                // 卡片内文字逐行显示
                showCardContent()
            }
        }, 1400)
        
        // 6. 进入按钮渐显+弹性缩放+呼吸灯效果
        handler.postDelayed({
            findViewById<Button>(R.id.btnEnter)?.let { btn ->
                btn.alpha = 1f
                
                val fadeIn = AlphaAnimation(0f, 1f).apply { 
                    duration = 700; 
                    fillAfter = true 
                }
                
                val scaleBounce = ScaleAnimation(
                    0.85f, 1.02f, 0.85f, 1.02f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                ).apply { 
                    duration = 600; 
                    fillAfter = true 
                }
                
                btn.startAnimation(fadeIn)
                btn.startAnimation(scaleBounce)
                
                // 按钮呼吸灯效果（引导用户点击）
                handler.postDelayed({
                    val breathScale = ScaleAnimation(
                        1f, 1.04f, 1f, 1.04f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                    ).apply {
                        duration = 1200
                        repeatCount = AlphaAnimation.INFINITE
                        repeatMode = ScaleAnimation.REVERSE
                        fillAfter = true
                    }
                    btn.startAnimation(breathScale)
                    
                    android.util.Log.i(TAG, "✅ 进入按钮已显示（带呼吸灯效果）- 等待用户点击")
                }, 1000)
            }
        }, 2800)
        
        // 7. 版本号标签淡入
        handler.postDelayed({
            findViewById<android.widget.LinearLayout>(R.id.layoutVersion)?.let { layout ->
                layout.alpha = 1f
                
                val fadeIn = AlphaAnimation(0f, 1f).apply { 
                    duration = 600; 
                    fillAfter = true 
                }
                
                val slideUp = TranslateAnimation(
                    TranslateAnimation.RELATIVE_TO_SELF, 0f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0.03f,
                    TranslateAnimation.RELATIVE_TO_SELF, 0f
                ).apply { 
                    duration = 500; 
                    fillAfter = true 
                }
                
                layout.startAnimation(fadeIn)
                layout.startAnimation(slideUp)
                
                android.util.Log.i(TAG, "✅ 版本号已显示: v${BuildConfig.VERSION_NAME}")
            }
        }, 3200)
    }
    
    private fun showCardContent() {
        val handler = Handler(Looper.getMainLooper())
        
        texts.filter { it.viewId != R.id.tvLine1 }.forEachIndexed { index, textInfo ->
            handler.postDelayed({
                findViewById<TextView>(textInfo.viewId)?.let { textView ->
                    textView.text = textInfo.text
                    textView.alpha = 1f
                    
                    val fadeIn = AlphaAnimation(0f, 1f).apply { 
                        duration = 500; 
                        fillAfter = true 
                    }
                    
                    val slideUp = TranslateAnimation(
                        TranslateAnimation.RELATIVE_TO_SELF, 0f,
                        TranslateAnimation.RELATIVE_TO_SELF, 0f,
                        TranslateAnimation.RELATIVE_TO_SELF, 0.03f,
                        TranslateAnimation.RELATIVE_TO_SELF, 0f
                    ).apply { 
                        duration = 600; 
                        fillAfter = true 
                    }
                    
                    textView.startAnimation(fadeIn)
                    textView.startAnimation(slideUp)
                }
            }, index * 180L)
        }
    }
    
    private fun setupEnterButton() {
        findViewById<Button>(R.id.btnEnter)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击了'进入应用'按钮")
            navigateToMain()
        }
    }
    
    private fun navigateToMain() {
        android.util.Log.i(TAG, "🚀 正在跳转到登录页...")
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    
    override fun onBackPressed() {
        android.util.Log.w(TAG, "用户按下了返回键（已禁用）")
    }
}
