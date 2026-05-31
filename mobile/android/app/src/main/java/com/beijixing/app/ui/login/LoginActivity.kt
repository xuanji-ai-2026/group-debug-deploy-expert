package com.beijixing.app.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.BuildConfig
import com.beijixing.app.R
import com.beijixing.app.util.AppLogger
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    
    private var etPhone: EditText? = null
    private var etCode: EditText? = null
    private var etPassword: EditText? = null
    private var btnLogin: Button? = null
    private var btnSendCode: Button? = null
    private var btnWechat: Button? = null
    
    private var tabSms: TextView? = null
    private var tabPassword: TextView? = null
    private var tabEmail: TextView? = null
    private var tilCode: View? = null
    private var tilPassword: View? = null
    private var tilEmail: View? = null
    private var tilPhone: View? = null
    private var tabContainer: View? = null
    private var tvVersion: TextView? = null
    
    companion object {
        private const val TAG = "BeiJiXing_Login"
        
        const val TEST_ACCOUNTS = """
        ══════════════════════════════
           测试账号 (v1.0.18)
        ══════════════════════════════
        1️⃣ 19577227393 / Admin@123 (普通用户)
        2️⃣ 13812345678 / Admin@123 (普通管理员)
        3️⃣ 13800138000 / Admin@123 (超级管理员)
        ══════════════════════════════
        """
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_login)
            
            initViews()
            setupListeners()
            switchToMode("sms")
            observeState()
            
            AppLogger.i(TAG, "✅ 初始化完成")
            AppLogger.i(TAG, TEST_ACCOUNTS)
            
            // 检查是否需要显示隐私政策弹窗
            val privacyAgreed = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("privacy_agreed", false)
            
            if (!privacyAgreed) {
                AppLogger.i(TAG, "📋 用户未同意隐私政策，显示协议弹窗")
                
                window.decorView.post {
                    showPrivacyPolicyDialog()
                }
            } else {
                AppLogger.i(TAG, "✅ 用户已同意隐私政策")
            }
            
            // 动态检查版本更新（调用真实API）
            checkForAppUpdate()
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "初始化失败", e)
            Toast.makeText(this, "初始化失败", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun initViews() {
        etPhone = findViewById(R.id.etPhone)
        etCode = findViewById(R.id.etCode)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnWechat = findViewById(R.id.btnWechat)
        
        tabSms = findViewById(R.id.tabSms)
        tabPassword = findViewById(R.id.tabPassword)
        tabEmail = findViewById(R.id.tabEmail)
        tilCode = findViewById(R.id.tilCode)
        tilPassword = findViewById(R.id.tilPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        tabContainer = findViewById(R.id.tabContainer)
        tvVersion = findViewById(R.id.tvVersion)
        
        tvVersion?.text = "v${BuildConfig.VERSION_NAME}"
        android.util.Log.i(TAG, "✅ 版本号: v${BuildConfig.VERSION_NAME}")
    }
    
    private fun setupListeners() {
        btnLogin?.setOnClickListener { performLogin() }
        btnSendCode?.setOnClickListener { handleSendCode() }
        
        btnWechat?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击微信登录")
            handleWechatLogin()
        }
        
        tabSms?.setOnClickListener {
            android.util.Log.i(TAG, "→ 点击了验证码Tab")
            switchToMode("sms")
        }
        
        tabPassword?.setOnClickListener {
            android.util.Log.i(TAG, "→ 点击了密码Tab")
            switchToMode("password")
        }
        
        tabEmail?.setOnClickListener {
            android.util.Log.i(TAG, "→ 点击了邮箱Tab")
            switchToMode("email")
        }
    }
    
    private fun switchToMode(mode: String) {
        val currentMode = viewModel.uiState.value.loginMode
        
        if (currentMode == mode) {
            return
        }
        
        AppLogger.i(TAG, "切换模式: $currentMode -> $mode")
        
        viewModel.setLoginMode(mode)
        
        resetTabs()
        
        when (mode) {
            "sms" -> {
                selectTab(tabSms)
                tilPhone?.visibility = View.VISIBLE
                tilCode?.visibility = View.VISIBLE
                tilPassword?.visibility = View.GONE
                tilEmail?.visibility = View.GONE
                btnSendCode?.visibility = View.VISIBLE
            }
            "password" -> {
                selectTab(tabPassword)
                tilPhone?.visibility = View.VISIBLE
                tilCode?.visibility = View.GONE
                tilPassword?.visibility = View.VISIBLE
                tilEmail?.visibility = View.GONE
                btnSendCode?.visibility = View.GONE
            }
            "email" -> {
                selectTab(tabEmail)
                tilPhone?.visibility = View.GONE
                tilCode?.visibility = View.GONE
                tilPassword?.visibility = View.GONE
                tilEmail?.visibility = View.VISIBLE
                btnSendCode?.visibility = View.GONE
            }
        }
        
        findViewById<View>(R.id.cardLogin)?.requestLayout()
        
        AppLogger.i(TAG, "✅ 已切换到 $mode 模式")
    }
    
    private fun resetTabs() {
        listOf(tabSms, tabPassword, tabEmail).forEach { tab ->
            tab?.isSelected = false
            tab?.invalidate()
        }
    }

    private fun selectTab(tab: TextView?) {
        tab?.isSelected = true
        tab?.invalidate()
    }
    
    private fun performLogin() {
        val phone = etPhone?.text?.toString()?.trim() ?: ""
        
        if (phone.isEmpty()) {
            showError("请输入手机号/用户名")
            return
        }
        
        val mode = viewModel.uiState.value.loginMode
        
        when (mode) {
            "sms" -> {
                val code = etCode?.text?.toString()?.trim() ?: ""
                if (code.isEmpty()) {
                    showError("请输入验证码")
                    return
                }
                viewModel.loginWithSmsCode(phone, code)
            }
            "password" -> {
                val password = etPassword?.text?.toString() ?: ""
                if (password.isEmpty()) {
                    showError("请输入密码")
                    return
                }
                viewModel.login(phone, password)
            }
            "email" -> {
                val emailView = tilEmail?.findViewById<EditText>(R.id.etEmail)
                val email = emailView?.text?.toString()?.trim() ?: ""
                if (email.isEmpty()) {
                    showError("请输入邮箱")
                    return
                }
                viewModel.loginWithEmail(email, "")
            }
        }
        
        AppLogger.i(TAG, "执行登录: 模式=$mode, 账号=${if (phone.length > 3) "***" + phone.takeLast(4) else phone}")
    }
    
    private fun handleSendCode() {
        val phone = etPhone?.text?.toString()?.trim() ?: ""
        
        if (phone.isEmpty()) {
            showError("请输入手机号")
            return
        }
        
        if (phone.length != 11) {
            showError("请输入正确的手机号")
            return
        }
        
        AppLogger.i(TAG, "发送验证码到: ${phone.take(3)}****${phone.takeLast(4)}")
        viewModel.sendSmsCode(phone)
        
        startCountdown()
    }
    
    private fun startCountdown() {
        btnSendCode?.isEnabled = false
        var count = 60
        
        android.os.Handler(android.os.Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                if (count > 0) {
                    btnSendCode?.text = "${count}s后重发"
                    count--
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 1000)
                } else {
                    btnSendCode?.text = "获取验证码"
                    btnSendCode?.isEnabled = true
                }
            }
        })
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        showLoading(true)
                    } else {
                        showLoading(false)
                        
                        state.errorMsg?.let { error ->
                            showError(error)
                            viewModel.clearError()
                        }
                        
                        if (state.isLoggedIn) {
                            AppLogger.i(TAG, "登录成功！跳转主页...")
                            navigateToMain()
                        }
                        
                        if (state.isSendingCode) {
                            AppLogger.i(TAG, "验证码已发送")
                            showToast("验证码已发送")
                        }
                    }
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.loadingView)?.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin?.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        AppLogger.w(TAG, "错误: $message")
    }
    
    private fun handleWechatLogin() {
        AppLogger.i(TAG, "📱 处理微信登录请求")
        
        try {
            val packageManager = packageManager
            val intent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
            
            if (intent != null) {
                AppLogger.i(TAG, "✅ 检测到微信已安装，启动微信授权")
                
                showToast("正在启动微信授权...")
                
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                
            } else {
                AppLogger.w(TAG, "❌ 未检测到微信应用")
                showWechatNotInstalledDialog()
            }
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "微信登录失败", e)
            showError("微信登录功能暂未开放，敬请期待")
        }
    }
    
    private fun showWechatNotInstalledDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("检测到您尚未安装微信，是否前往应用商店下载？")
            .setPositiveButton("去下载") { _, _ ->
                try {
                    val storeIntent = Intent(Intent.ACTION_VIEW)
                    storeIntent.data = Uri.parse("market://details?id=com.tencent.mm")
                    startActivity(storeIntent)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "打开应用商店失败", e)
                    showError("无法打开应用商店，请手动搜索微信")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToMain() {
        AppLogger.i(TAG, "🚀 登录成功！正在跳转到主页...")
        showToast("登录成功！即将进入主页...")
        
        try {
            val intent = Intent(this, com.beijixing.app.ui.main.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            
            AppLogger.i(TAG, "✅ 已成功跳转到主页")
        } catch (e: Exception) {
            AppLogger.e(TAG, "❌ 跳转主页失败", e)
            showError("跳转失败：" + (e.message ?: "未知错误"))
        }
    }
    
    fun onTabSmsClick(view: View) {
        android.util.Log.i(TAG, "✅ [XML onClick] 验证码Tab被点击")
        switchToMode("sms")
    }
    
    fun onTabPasswordClick(view: View) {
        android.util.Log.i(TAG, "✅ [XML onClick] 密码Tab被点击")
        switchToMode("password")
    }
    
    fun onTabEmailClick(view: View) {
        android.util.Log.i(TAG, "✅ [XML onClick] 邮箱Tab被点击")
        switchToMode("email")
    }
    
    /**
     * 动态检查应用更新（使用真实API）
     * 只有当服务器返回的版本号大于当前版本时才显示更新提示
     */
    private fun checkForAppUpdate() {
        AppLogger.i(TAG, "🔍 开始动态检查版本更新...")
        
        com.beijixing.app.util.AppUpdateManager.checkForUpdate(this, object : com.beijixing.app.util.AppUpdateManager.UpdateCallback {
            override fun onCheckComplete(updateInfo: com.beijixing.app.util.AppUpdateManager.UpdateInfo) {
                if (updateInfo.hasUpdate) {
                    AppLogger.i(TAG, "✅ 发现新版本: ${updateInfo.versionName}，显示更新对话框")
                    showVersionUpdateDialog(updateInfo)
                } else {
                    AppLogger.i(TAG, "✅ 当前已是最新版本或无法获取版本信息")
                }
            }
            override fun onDownloadProgress(progress: Int) {}
            override fun onDownloadSuccess(apkFile: java.io.File) {}
            override fun onError(error: String) {
                AppLogger.e(TAG, "检查更新失败: $error")
            }
        })
    }
    
    private fun showVersionUpdateDialog(versionInfo: com.beijixing.app.util.AppUpdateManager.UpdateInfo? = null) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_version_update, null)
            
            val builder = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)  // 允许用户关闭
            
            val dialog = builder.create()
            
            if (dialog.window != null) {
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            
            // 使用动态版本信息或默认值
            val currentVersion = "v${BuildConfig.VERSION_NAME}"
            val newVersion = versionInfo?.let { "v${it.versionName}" } ?: currentVersion
            val updateData = versionInfo ?: com.beijixing.app.util.AppUpdateManager.UpdateInfo(
                hasUpdate = false,
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
                downloadUrl = "https://www.beijixing-ai.com/api/app/download",
                changelog = "版本更新",
                isForceUpdate = false,
                apkSize = 0
            )
            
            dialogView.findViewById<android.widget.TextView>(R.id.tvCurrentVersion)?.text = currentVersion
            dialogView.findViewById<android.widget.TextView>(R.id.tvNewVersion)?.text = newVersion
            dialogView.findViewById<android.widget.TextView>(R.id.tvUpdateTitle)?.text = "🎉 发现新版本 $newVersion"
            
            // 立即更新按钮
            dialogView.findViewById<Button>(R.id.btnConfirmUpdate)?.setOnClickListener {
                AppLogger.i(TAG, "→ 用户点击了'立即更新'按钮")
                
                try {
                    dialog.dismiss()
                    
                    // 显示下载进度对话框
                    val progressDialog = android.app.ProgressDialog(this@LoginActivity).apply {
                        setTitle("🚀 正在下载新版本")
                        setMessage("准备下载 ${updateData.versionName}...")
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
                    
                    // 使用新的回调机制
                    val callback = object : com.beijixing.app.util.AppUpdateManager.UpdateCallback {
                        override fun onCheckComplete(info: com.beijixing.app.util.AppUpdateManager.UpdateInfo) {}
                        override fun onDownloadProgress(progress: Int) {
                            runOnUiThread {
                                if (progressDialog.isShowing) {
                                    progressDialog.progress = progress
                                    when (progress) {
                                        0 -> progressDialog.setMessage("正在连接服务器...")
                                        in 1..30 -> progressDialog.setMessage("正在下载 ${updateData.versionName}... ($progress%)")
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
                                com.beijixing.app.util.AppUpdateManager.installApk(this@LoginActivity, apkFile)
                            }
                        }
                        override fun onError(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "更新失败：$error", Toast.LENGTH_LONG).show()
                                if (progressDialog.isShowing) {
                                    progressDialog.dismiss()
                                }
                            }
                        }
                    }
                    
                    // 调用AppUpdateManager开始下载和安装
                    com.beijixing.app.util.AppUpdateManager.startDownload(
                        context = this@LoginActivity,
                        updateInfo = updateData,
                        callback = callback
                    )
                    
                } catch (e: Exception) {
                    AppLogger.e(TAG, "❌ 启动下载更新失败", e)
                    Toast.makeText(this, "启动更新失败：" + (e.message ?: "未知错误"), Toast.LENGTH_LONG).show()
                }
            }
            
            // 跳过版本
            dialogView.findViewById<android.widget.TextView>(R.id.tvSkipVersion)?.setOnClickListener {
                AppLogger.i(TAG, "→ 用户选择跳过版本更新")
                dialog.dismiss()
            }
            
            dialog.show()
            
            AppLogger.i(TAG, "✅ 版本迭代弹窗已显示 - 当前: $currentVersion, 最新: $newVersion")
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "显示版本迭代弹窗失败", e)
        }
    }
    
    private fun openUpdatePage() {
        try {
            // 打开应用商店或下载页面
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://beijixing.ai/download")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            
            AppLogger.i(TAG, "✅ 已打开更新页面")
        } catch (e: Exception) {
            AppLogger.e(TAG, "打开更新页面失败", e)
            Toast.makeText(this, "请访问 beijixing.ai 下载最新版本", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showPrivacyPolicyDialog() {
        if (isFinishing || isDestroyed) {
            AppLogger.w(TAG, "Activity已销毁或正在结束，跳过显示隐私政策弹窗")
            return
        }

        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_privacy_policy, null)

            val btnDisagree = dialogView.findViewById<Button>(R.id.btnDisagree)
            val btnAgree = dialogView.findViewById<Button>(R.id.btnAgree)
            val tvLinkAgreement = dialogView.findViewById<android.widget.TextView>(R.id.tvLinkAgreement)
            val tvLinkPrivacy = dialogView.findViewById<android.widget.TextView>(R.id.tvLinkPrivacy)
            val tvLinkCollection = dialogView.findViewById<android.widget.TextView>(R.id.tvLinkCollection)

            if (btnAgree == null || btnDisagree == null) {
                AppLogger.e(TAG, "隐私政策弹窗关键按钮为null，btnAgree=${btnAgree != null}, btnDisagree=${btnDisagree != null}")
                return
            }

            val builder = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)

            val dialog = builder.create()

            btnDisagree.setOnClickListener {
                AppLogger.w(TAG, "用户拒绝了隐私政策")
                Toast.makeText(this, "需要同意协议才能使用本应用", Toast.LENGTH_LONG).show()

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    finishAffinity()
                    System.exit(0)
                }, 1500)
            }

            btnAgree.setOnClickListener {
                AppLogger.i(TAG, "✅ 用户同意了隐私政策")

                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("privacy_agreed", true)
                    .apply()

                dialog.dismiss()

                Toast.makeText(this, "感谢您的信任！", Toast.LENGTH_SHORT).show()
            }

            tvLinkAgreement?.setOnClickListener {
                openWebPage("https://beijixing.ai/agreement")
            }

            tvLinkPrivacy?.setOnClickListener {
                openWebPage("https://beijixing.ai/privacy")
            }

            tvLinkCollection?.setOnClickListener {
                openWebPage("https://beijixing.ai/data-collection")
            }

            if (isFinishing || isDestroyed) {
                AppLogger.w(TAG, "Activity已销毁，取消显示弹窗")
                return
            }

            dialog.show()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            AppLogger.i(TAG, "✅ 隐私政策弹窗已显示")

        } catch (e: Exception) {
            AppLogger.e(TAG, "显示隐私政策弹窗失败", e)
        }
    }
    
    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            
            AppLogger.i(TAG, "✅ 已打开网页: $url")
        } catch (e: Exception) {
            AppLogger.e(TAG, "打开网页失败: $url", e)
            Toast.makeText(this, "无法打开链接，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun onPrivacyClick(view: View) {
        android.util.Log.i(TAG, "→ 点击了隐私政策链接")
        showPrivacyPolicyDialog()
    }
    
    fun onAgreementClick(view: View) {
        android.util.Log.i(TAG, "→ 点击了用户协议链接")
        showPrivacyPolicyDialog()
    }
}