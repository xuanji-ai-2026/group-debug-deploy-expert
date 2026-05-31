package com.beijixing.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object ThirdPartyAppUtil {

    private const val TAG = "ThirdPartyApp"

    object PackageName {
        const val DOUYIN = "com.ss.android.ugc.aweme"
        const val DOUYIN_LITE = "com.zhiliaoapp.musically"
        const val KUAISHOU = "com.smile.gifmaker"
        const val KUAISHOU_NEBULA = "com.kuaishou.nebula"
        const val KUAISHOU_INTERNATIONAL = "com.yxcorp.gifshow"
        const val XIAOHONGSHU = "com.xingin.xhs"
        const val WECHAT = "com.tencent.mm"
        const val QQ = "com.tencent.mobileqq"
        const val WEIBO = "com.sina.weibo"
    }

    object Platform {
        const val DOUYIN = "DOUYIN"
        const val KUAISHOU = "KUAISHOU"
        const val XIAOHONGSHU = "XIAOHONGSHU"
        const val WECHAT = "WECHAT"
        const val WEIBO = "WEIBO"
        const val QQ = "QQ"
    }

    fun getPackageName(platform: String): String {
        return when (platform) {
            Platform.DOUYIN -> PackageName.DOUYIN
            Platform.KUAISHOU -> PackageName.KUAISHOU
            Platform.XIAOHONGSHU -> PackageName.XIAOHONGSHU
            Platform.WECHAT -> PackageName.WECHAT
            Platform.WEIBO -> PackageName.WEIBO
            Platform.QQ -> PackageName.QQ
            else -> ""
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isPlatformInstalled(context: Context, platform: String): Boolean {
        val packageName = getPackageName(platform)
        if (packageName.isEmpty()) return false

        if (isAppInstalled(context, packageName)) return true

        return when (platform) {
            Platform.DOUYIN -> isAppInstalled(context, PackageName.DOUYIN_LITE)
            Platform.KUAISHOU -> isAppInstalled(context, PackageName.KUAISHOU_NEBULA) ||
                    isAppInstalled(context, PackageName.KUAISHOU_INTERNATIONAL)
            else -> false
        }
    }

    fun openApp(context: Context, platform: String): Boolean {
        val mainPackageName = getPackageName(platform)
        if (mainPackageName.isEmpty()) {
            Toast.makeText(context, "不支持的平台: $platform", Toast.LENGTH_SHORT).show()
            return false
        }

        var targetPackage = mainPackageName

        when (platform) {
            Platform.DOUYIN -> {
                if (!isAppInstalled(context, mainPackageName)) {
                    targetPackage = PackageName.DOUYIN_LITE
                }
            }
            Platform.KUAISHOU -> {
                if (!isAppInstalled(context, mainPackageName)) {
                    targetPackage = PackageName.KUAISHOU_NEBULA
                }
                if (!isAppInstalled(context, targetPackage)) {
                    targetPackage = PackageName.KUAISHOU_INTERNATIONAL
                }
            }
        }

        if (!isAppInstalled(context, targetPackage)) {
            showInstallPrompt(context, platform)
            return false
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(targetPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "打开应用失败: $targetPackage", e)
            Toast.makeText(context, "打开${getDisplayName(platform)}失败", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openAppWithContent(context: Context, platform: String, content: String): Boolean {
        return when (platform) {
            Platform.DOUYIN -> openDouyinWithContent(context, content)
            Platform.KUAISHOU -> openKuaishouWithContent(context, content)
            Platform.XIAOHONGSHU -> openXiaohongshuWithContent(context, content)
            Platform.WECHAT -> openWechatWithContent(context, content)
            Platform.WEIBO -> openWeiboWithContent(context, content)
            else -> openApp(context, platform)
        }
    }

    private fun openDouyinWithContent(context: Context, content: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage(PackageName.DOUYIN)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openApp(context, Platform.DOUYIN)
            }
        } catch (e: Exception) {
            openApp(context, Platform.DOUYIN)
        }
    }

    private fun openKuaishouWithContent(context: Context, content: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage(PackageName.KUAISHOU)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openApp(context, Platform.KUAISHOU)
            }
        } catch (e: Exception) {
            openApp(context, Platform.KUAISHOU)
        }
    }

    private fun openXiaohongshuWithContent(context: Context, content: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage(PackageName.XIAOHONGSHU)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openApp(context, Platform.XIAOHONGSHU)
            }
        } catch (e: Exception) {
            openApp(context, Platform.XIAOHONGSHU)
        }
    }

    private fun openWechatWithContent(context: Context, content: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage(PackageName.WECHAT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openApp(context, Platform.WECHAT)
            }
        } catch (e: Exception) {
            openApp(context, Platform.WECHAT)
        }
    }

    private fun openWeiboWithContent(context: Context, content: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage(PackageName.WEIBO)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openApp(context, Platform.WEIBO)
            }
        } catch (e: Exception) {
            openApp(context, Platform.WEIBO)
        }
    }

    fun openUrlInBrowser(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "打开URL失败", e)
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun goToDownloadPage(context: Context, platform: String) {
        try {
            val packageName = getPackageName(platform)
            val uri = Uri.parse("market://details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openUrlInBrowser(context, "https://www.coolapk.com/apk/${getPackageName(platform)}")
        }
    }

    private fun showInstallPrompt(context: Context, platform: String) {
        val displayName = getDisplayName(platform)
        android.util.Log.i(TAG, "$displayName 未安装")
        
        try {
            goToDownloadPage(context, platform)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "请先安装$displayName",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun getDisplayName(platform: String): String {
        return when (platform) {
            Platform.DOUYIN -> "抖音"
            Platform.KUAISHOU -> "快手"
            Platform.XIAOHONGSHU -> "小红书"
            Platform.WECHAT -> "微信"
            Platform.WEIBO -> "微博"
            Platform.QQ -> "QQ"
            else -> platform
        }
    }

    fun getAllInstalledPlatforms(context: Context): List<String> {
        val platforms = mutableListOf<String>()
        listOf(Platform.DOUYIN, Platform.KUAISHOU, Platform.XIAOHONGSHU,
              Platform.WECHAT, Platform.WEIBO, Platform.QQ).forEach { platform ->
            if (isPlatformInstalled(context, platform)) {
                platforms.add(platform)
            }
        }
        return platforms
    }
}