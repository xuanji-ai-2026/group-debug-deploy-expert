package com.beijixing.app.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.WindowInsets
import android.view.WindowInsets.Type
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale
import java.util.UUID

object DeviceInfoManager {
    
    private const val TAG = "BeiJiXing_Device"
    
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val deviceName: String,
        val hardwareId: String,
        val androidVersion: Int,
        val androidVersionName: String,
        val sdkVersion: Int,
        val securityPatch: String,
        val screenWidthPx: Int,
        val screenHeightPx: Int,
        val screenDensityDpi: Int,
        val screenDensity: Float,
        val scaledDensity: Float,
        val screenWidthDp: Float,
        val screenHeightDp: Float,
        val screenAspectRatio: String,
        val statusBarHeightPx: Int,
        val navigationBarHeightPx: Int,
        val hasCutout: Boolean,
        val cutoutHeightPx: Int,
        val safeAreaTopPx: Int,
        val safeAreaBottomPx: Int,
        val appVersionCode: Int,
        val appVersionName: String,
        val packageInfo: String,
        val recommendedTopMarginDp: Float,
        val recommendedCardMarginTopDp: Float,
        val isNotchDevice: Boolean,
        val needsLayoutAdjustment: Boolean
    ) {
        fun toLogString(): String {
            return """
            ════════════════════════════════════════
            📱 北极星 - 设备信息报告 v2.0
            ════════════════════════════════════════
            设备: $deviceName ($manufacturer $model)
            分辨率: ${screenWidthPx}x${screenHeightPx}px
            密度: ${screenDensityDpi}dpi
            Android: $androidVersion ($androidVersionName)
            挖孔屏: ${if (hasCutout) "是 (${cutoutHeightPx}px)" else "否"}
            安全区: top=${safeAreaTopPx}px, bottom=${safeAreaBottomPx}px
            版本: v$appVersionName ($appVersionCode)
            ════════════════════════════════════════
            """.trimIndent()
        }
    }
    
    private var cachedDeviceInfo: DeviceInfo? = null
    
    fun getDeviceInfo(context: Context): DeviceInfo {
        cachedDeviceInfo?.let { return it }
        
        Log.i(TAG, "开始收集设备信息...")
        
        val packageManager = context.packageManager
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取包信息失败", e)
            null
        }
        
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        @Suppress("DEPRECATION")
        val defaultDisplay: Display = windowManager.defaultDisplay
        defaultDisplay.getRealMetrics(displayMetrics)
        
        val point = Point()
        defaultDisplay.getRealSize(point)
        
        val statusBarHeight = getStatusBarHeight(context)
        val navigationBarHeight = getNavigationBarHeight(context)
        val cutoutInfo = getCutoutInfo(windowManager)
        val safeAreaInfo = getSafeArea(windowManager)
        
        val isNotchDevice = cutoutInfo.first && cutoutInfo.second > 0
        val needsLayoutAdjustment = isNotchDevice || statusBarHeight > 100
        
        val recommendedTopMargin = calculateRecommendedTopMargin(
            cutoutInfo.second,
            statusBarHeight,
            displayMetrics.density
        )
        
        val recommendedCardMargin = calculateRecommendedCardMargin(
            isNotchDevice,
            cutoutInfo.second,
            displayMetrics.density
        )
        
        val info = DeviceInfo(
            manufacturer = Build.MANUFACTURER.uppercase(Locale.getDefault()),
            model = Build.MODEL,
            deviceName = "${Build.BRAND} ${Build.MODEL}",
            hardwareId = getHardwareIdentifier(),
            
            androidVersion = Build.VERSION.SDK_INT,
            androidVersionName = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            
            screenWidthPx = point.x,
            screenHeightPx = point.y,
            screenDensityDpi = displayMetrics.densityDpi,
            screenDensity = displayMetrics.density,
            scaledDensity = displayMetrics.scaledDensity,
            screenWidthDp = point.x / displayMetrics.density,
            screenHeightDp = point.y / displayMetrics.density,
            screenAspectRatio = calculateAspectRatio(point.x, point.y),
            
            statusBarHeightPx = statusBarHeight,
            navigationBarHeightPx = navigationBarHeight,
            hasCutout = cutoutInfo.first,
            cutoutHeightPx = cutoutInfo.second,
            safeAreaTopPx = safeAreaInfo.first,
            safeAreaBottomPx = safeAreaInfo.second,
            
            appVersionCode = packageInfo?.versionCode ?: 0,
            appVersionName = packageInfo?.versionName ?: "unknown",
            packageInfo = "${context.packageName}",
            
            recommendedTopMarginDp = recommendedTopMargin,
            recommendedCardMarginTopDp = recommendedCardMargin,
            isNotchDevice = isNotchDevice,
            needsLayoutAdjustment = needsLayoutAdjustment
        )
        
        cachedDeviceInfo = info
        Log.i(TAG, "\n${info.toLogString()}")
        
        return info
    }
    
    private fun getHardwareIdentifier(): String {
        return try {
            "${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD}_${Build.SERIAL}".replace(" ", "_").uppercase(Locale.getDefault())
        } catch (e: Exception) {
            UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.getDefault())
        }
    }
    
    private fun calculateRecommendedTopMargin(cutoutHeight: Int, statusBarHeight: Int, density: Float): Float {
        val baseMargin = if (cutoutHeight > 0) cutoutHeight else statusBarHeight
        return (baseMargin / density) + 20f
    }
    
    private fun calculateRecommendedCardMargin(isNotch: Boolean, cutoutHeight: Int, density: Float): Float {
        return if (isNotch && cutoutHeight > 0) {
            (cutoutHeight / density) - 20f
        } else {
            -40f
        }
    }
    
    private fun getSafeArea(windowManager: WindowManager): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val insets = windowManager.maximumWindowMetrics.windowInsets
                val systemBars = insets.getInsets(Type.systemBars())
                Pair(systemBars.top, systemBars.bottom)
            } catch (e: Exception) {
                Log.w(TAG, "获取安全区域失败(R+)", e)
                Pair(0, 0)
            }
        } else {
            Pair(0, 0)
        }
    }
    
    fun calculateAspectRatio(width: Int, height: Int): String {
        val gcdValue = gcd(width, height)
        val w = width / gcdValue
        val h = height / gcdValue
        return "$w:$h"
    }
    
    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }
    
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    
    private fun getCutoutInfo(windowManager: WindowManager): Pair<Boolean, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val insets = windowManager.maximumWindowMetrics.windowInsets
                val cutoutInset = insets.getInsets(Type.displayCutout())
                val hasCutout = cutoutInset.top > 0 || cutoutInset.bottom > 0
                return Pair(hasCutout, cutoutInset.top)
            } catch (e: Exception) {
                Log.w(TAG, "获取挖孔信息失败(R+)", e)
                Pair(false, 0)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            val cutout = windowManager.defaultDisplay?.cutout
            val hasCutout = cutout != null && cutout.boundingRects.isNotEmpty()
            var maxHeight = 0
            cutout?.boundingRects?.forEach { rect ->
                maxHeight = maxOf(maxOf(maxHeight, rect.height()))
            }
            Pair(hasCutout, maxHeight)
        } else {
            Pair(false, 0)
        }
    }
    
    fun setupEdgeToEdge(activity: Activity) {
        Log.i(TAG, "设置Edge-to-Edge显示模式...")
        
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = true
        
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT
        
        Log.i(TAG, "Edge-to-Edge模式已启用")
    }
    
    fun clearCache() {
        cachedDeviceInfo = null
        Log.i(TAG, "设备信息缓存已清除")
    }
}
