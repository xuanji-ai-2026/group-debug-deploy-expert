package com.beijixing.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.beijixing.app.R

object SocialMediaHelper {

    data class PlatformInfo(
        val name: String,
        val packageName: String,
        val deepLinkPrefix: String? = null,
        val iconResId: Int = 0
    )

    val SUPPORTED_PLATFORMS = listOf(
        PlatformInfo(
            name = "抖音",
            packageName = "com.ss.android.ugc.aweme",
            deepLinkPrefix = "snssdk1128://"
        ),
        PlatformInfo(
            name = "抖音极速版",
            packageName = "com.ss.android.ugc.aweme.lite",
            deepLinkPrefix = "aweme://"
        ),
        PlatformInfo(
            name = "快手",
            packageName = "com.smile.gifmaker",
            deepLinkPrefix = "kwai://"
        ),
        PlatformInfo(
            name = "快手极速版",
            packageName = "com.kuaishou.nebula",
            deepLinkPrefix = "ksnebula://"
        ),
        PlatformInfo(
            name = "小红书",
            packageName = "com.xingluo.all",
            deepLinkPrefix = "xhsdiscover://"
        ),
        PlatformInfo(
            name = "微信",
            packageName = "com.tencent.mm",
            deepLinkPrefix = "weixin://"
        )
    )

    fun isPlatformInstalled(context: Context, platformName: String): Boolean {
        return try {
            val platform = SUPPORTED_PLATFORMS.find { it.name == platformName } ?: return false
            context.packageManager.getPackageInfo(platform.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openPlatform(context: Context, platformName: String): Boolean {
        return try {
            val platform = SUPPORTED_PLATFORMS.find { it.name == platformName }
                ?: run {
                    Toast.makeText(context, "不支持的平台: $platformName", Toast.LENGTH_SHORT).show()
                    return false
                }

            if (!isPlatformInstalled(context, platform.name)) {
                showInstallDialog(context, platform)
                return false
            }

            val intent = context.packageManager.getLaunchIntentForPackage(platform.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                openWithDeepLink(context, platform)
            }
        } catch (e: Exception) {
            android.util.Log.e("SocialMediaHelper", "打开${platformName}失败", e)
            Toast.makeText(context, "打开${platformName}失败: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun openWithDeepLink(context: Context, platform: PlatformInfo): Boolean {
        return try {
            val deepLink = platform.deepLinkPrefix ?: return false
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("SocialMediaHelper", "Deep Link打开失败", e)
            false
        }
    }

    fun showInstallDialog(context: Context, platform: PlatformInfo) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("未安装${platform.name}")
            .setMessage("检测到您尚未安装${platform.name}，是否前往应用商店下载？")
            .setPositiveButton("去下载") { _, _ ->
                openAppStore(context, platform.packageName)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun openAppStore(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://apkpure.com/cn/search?q=$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开应用商店", Toast.LENGTH_SHORT).show()
        }
    }

    fun getInstalledPlatforms(context: Context): List<PlatformInfo> {
        return SUPPORTED_PLATFORMS.filter { isPlatformInstalled(context, it.name) }
    }

    fun openUserProfile(context: Context, platformName: String, userId: String): Boolean {
        return try {
            val platform = SUPPORTED_PLATFORMS.find { it.name == platformName }
                ?: return false

            when (platform.packageName) {
                "com.ss.android.ugc.aweme" -> openDouyinProfile(context, userId)
                "com.smile.gifmaker" -> openKuaishouProfile(context, userId)
                "com.xingluo.all" -> openXiaohongshuProfile(context, userId)
                else -> openPlatform(context, platformName)
            }
        } catch (e: Exception) {
            android.util.Log.e("SocialMediaHelper", "打开用户主页失败", e)
            false
        }
    }

    private fun openDouyinProfile(context: Context, userId: String): Boolean {
        return try {
            val uri = Uri.parse("snssdk1128://user/profile/$userId")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openPlatform(context, "抖音")
            }
        } catch (e: Exception) {
            openPlatform(context, "抖音")
        }
    }

    private fun openKuaishouProfile(context: Context, userId: String): Boolean {
        return try {
            val uri = Uri.parse("kwai://profile/$userId")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openPlatform(context, "快手")
            }
        } catch (e: Exception) {
            openPlatform(context, "快手")
        }
    }

    private fun openXiaohongshuProfile(context: Context, userId: String): Boolean {
        return try {
            val uri = Uri.parse("xhsdiscover://user/profile/$userId")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                openPlatform(context, "小红书")
            }
        } catch (e: Exception) {
            openPlatform(context, "小红书")
        }
    }
}
