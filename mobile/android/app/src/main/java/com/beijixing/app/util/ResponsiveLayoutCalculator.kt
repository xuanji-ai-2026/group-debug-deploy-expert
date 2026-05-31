package com.beijixing.app.util

import android.content.Context
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.roundToInt

object ResponsiveLayoutCalculator {
    
    private const val TAG = "BeiJiXing_Layout"
    
    data class LayoutConfig(
        val topBgHeightPx: Int,
        val topBgHeightDp: Float,
        
        val logoMarginTopPx: Int,
        val logoMarginTopDp: Float,
        val logoSizePx: Int,
        val logoSizeDp: Float,
        
        val appNameMarginTopDp: Float,
        val appNameTextSizeSp: Float,
        
        val cardMarginTopPx: Int,
        val cardMarginTopDp: Float,
        val cardPaddingPx: Int,
        val cardPaddingDp: Float,
        
        val tabHeightPx: Int,
        val tabHeightDp: Float,
        val tabTextSizeSp: Float,
        
        val inputTextSizeSp: Float,
        val buttonTextSizeSp: Float,
        
        val bottomMarginPx: Int,
        
        val isCompactMode: Boolean,
        val adjustmentReason: String
    ) {
        fun toLogString(): String {
            return """
            ══════════════════════════════
            📐 响应式布局配置
            ══════════════════════════════
            顶部背景: ${topBgHeightDp.toInt()}dp (${topBgHeightPx}px)
            Logo间距: ${logoMarginTopDp.toInt()}dp · 尺寸: ${logoSizeDp.toInt()}dp
            卡片边距: ${cardMarginTopDp.toInt()}dp · 内边距: ${cardPaddingDp.toInt()}dp
            Tab高度: ${tabHeightDp.toInt()}dp · 字体: ${tabTextSizeSp}sp
            紧凑模式: $isCompactMode
            调整原因: $adjustmentReason
            ══════════════════════════════
            """.trimIndent()
        }
    }
    
    fun calculateLayout(context: Context, deviceInfo: DeviceInfoManager.DeviceInfo): LayoutConfig {
        Log.i(TAG, "开始计算响应式布局...")
        Log.d(TAG, """
            |输入参数:
            |  屏幕尺寸: ${deviceInfo.screenWidthPx}×${deviceInfo.screenHeightPx}px (${deviceInfo.screenWidthDp.toInt()}×${deviceInfo.screenHeightDp.toInt()}dp)
            |  密度: ${deviceInfo.screenDensityDpi}dpi (${deviceInfo.screenDensity}x)
            |  安全区域: top=${deviceInfo.safeAreaTopPx}px, bottom=${deviceInfo.safeAreaBottomPx}px
            |  挖孔: ${if (deviceInfo.hasCutout) "${deviceInfo.cutoutHeightPx}px" else "无"}
            |  状态栏: ${deviceInfo.statusBarHeightPx}px
        """.trimMargin())
        
        val density = deviceInfo.screenDensity
        val screenWidthDp = deviceInfo.screenWidthDp
        val screenHeightDp = deviceInfo.screenHeightDp
        val safeAreaTop = deviceInfo.safeAreaTopPx
        val hasNotch = deviceInfo.isNotchDevice
        
        val isCompactMode = screenHeightDp < 700 || hasNotch
        val isSmallScreen = screenWidthDp < 360
        
        Log.d(TAG, "设备特征: compact=$isCompactMode, smallScreen=$isSmallScreen, notch=$hasNotch")
        
        val topBgHeightDp = when {
            hasNotch && safeAreaTop > 100 -> {
                val minBgHeight = (safeAreaTop / density) + 140f
                Log.d(TAG, "挖孔屏模式: safeAreaTop=${safeAreaTop}px → topBg=${minBgHeight.toInt()}dp")
                minBgHeight.coerceIn(200f, 320f)
            }
            isCompactMode -> {
                Log.d(TAG, "紧凑模式: 减少背景高度")
                200f
            }
            else -> {
                Log.d(TAG, "标准模式")
                260f
            }
        }
        
        val logoMarginTopDp = when {
            hasNotch && safeAreaTop > 0 -> {
                val margin = (safeAreaTop / density) + 15f
                Log.d(TAG, "挖孔屏Logo间距: ${margin.toInt()}dp (based on safeAreaTop=${safeAreaTop}px)")
                margin.coerceIn(40f, 120f)
            }
            isCompactMode -> 35f
            else -> 45f
        }
        
        val logoSizeDp = if (isSmallScreen) 60f else 70f
        
        val appNameMarginTopDp = if (isCompactMode) 8f else 12f
        val appNameTextSizeSp = if (isSmallScreen) 20f else 24f
        
        val cardMarginTopDp = when {
            hasNotch && safeAreaTop > 100 -> {
                val margin = (safeAreaTop / density) - 30f
                Log.d(TAG, "挖孔屏卡片间距: ${margin.toInt()}dp")
                margin.coerceIn(-50f, 20f)
            }
            isCompactMode -> -35f
            else -> -45f
        }
        
        val cardPaddingDp = if (isCompactMode) 22f else 28f
        
        val tabHeightDp = if (isCompactMode) 40f else 44f
        val tabTextSizeSp = if (isSmallScreen) 13f else 14f
        
        val inputTextSizeSp = if (isSmallScreen) 15f else 16f
        val buttonTextSizeSp = if (isSmallScreen) 15f else 16f
        
        val bottomMarginDp = if (isCompactMode) 16f else 24f
        
        val config = LayoutConfig(
            topBgHeightPx = (topBgHeightDp * density.toDouble()).toInt(),
            topBgHeightDp = topBgHeightDp,
            
            logoMarginTopPx = (logoMarginTopDp * density.toDouble()).toInt(),
            logoMarginTopDp = logoMarginTopDp,
            logoSizePx = (logoSizeDp * density.toDouble()).toInt(),
            logoSizeDp = logoSizeDp,
            
            appNameMarginTopDp = appNameMarginTopDp,
            appNameTextSizeSp = appNameTextSizeSp,
            
            cardMarginTopPx = (cardMarginTopDp * density.toDouble()).toInt(),
            cardMarginTopDp = cardMarginTopDp,
            cardPaddingPx = (cardPaddingDp * density.toDouble()).toInt(),
            cardPaddingDp = cardPaddingDp,
            
            tabHeightPx = (tabHeightDp * density.toDouble()).toInt(),
            tabHeightDp = tabHeightDp,
            tabTextSizeSp = tabTextSizeSp,
            
            inputTextSizeSp = inputTextSizeSp,
            buttonTextSizeSp = buttonTextSizeSp,
            
            bottomMarginPx = (bottomMarginDp * density.toDouble()).toInt(),
            
            isCompactMode = isCompactMode,
            adjustmentReason = buildAdjustmentReason(hasNotch, isCompactMode, isSmallScreen, safeAreaTop)
        )
        
        Log.i(TAG, "\n${config.toLogString()}")
        
        return config
    }
    
    private fun buildAdjustmentReason(
        hasNotch: Boolean,
        isCompact: Boolean,
        isSmall: Boolean,
        safeAreaTop: Int
    ): String {
        val reasons = mutableListOf<String>()
        if (hasNotch) reasons.add("挖孔屏适配(safeAreaTop=${safeAreaTop}px)")
        if (isCompact) reasons.add("紧凑屏幕(<700dp高)")
        if (isSmall) reasons.add("小屏宽度(<360dp宽)")
        return if (reasons.isEmpty()) "标准布局" else reasons.joinToString(" + ")
    }
    
    fun applyLayoutConfig(
        context: Context,
        config: LayoutConfig,
        views: Map<String, View?>
    ) {
        Log.i(TAG, "应用响应式布局配置...")
        var appliedCount = 0
        var errorCount = 0
        
        try {
            views["topBg"]?.let { view ->
                val layoutParams = view.layoutParams
                layoutParams.height = config.topBgHeightPx
                view.layoutParams = layoutParams
                Log.d(TAG, "✅ topBg高度: ${config.topBgHeightPx}px (${config.topBgHeightDp.toInt()}dp)")
                appliedCount++
            } ?: run { Log.w(TAG, "⚠️ topBg为null"); errorCount++ }
            
            views["ivLogo"]?.let { view ->
                val params = view.layoutParams as? ConstraintLayout.LayoutParams
                params?.let { p ->
                    p.topMargin = config.logoMarginTopPx
                    p.width = config.logoSizePx
                    p.height = config.logoSizePx
                    view.layoutParams = p
                    Log.d(TAG, "✅ Logo: margin=${config.logoMarginTopPx}px, size=${config.logoSizePx}px")
                    appliedCount++
                } ?: run { Log.w(TAG, "⚠️ ivLogo参数类型错误"); errorCount++ }
            } ?: run { Log.w(TAG, "⚠️ ivLogo为null"); errorCount++ }
            
            views["tvAppName"]?.let { view ->
                (view as? android.widget.TextView)?.let { tv ->
                    val params = tv.layoutParams as? ConstraintLayout.LayoutParams
                    params?.let { p ->
                        p.topMargin = (config.appNameMarginTopDp * config.topBgHeightDp / config.topBgHeightDp).toInt()
                        tv.layoutParams = p
                        tv.textSize = config.appNameTextSizeSp
                        Log.d(TAG, "✅ AppName: marginTop=${config.appNameMarginTopDp.toInt()}dp, textSize=${config.appNameTextSizeSp}sp")
                        appliedCount++
                    } ?: run { Log.w(TAG, "⚠️ tvAppName参数类型错误"); errorCount++ }
                } ?: run { Log.w(TAG, "⚠️ tvAppName不是TextView"); errorCount++ }
            } ?: run { Log.w(TAG, "⚠️ tvAppName为null"); errorCount++ }
            
            views["cardLogin"]?.let { view ->
                val params = view.layoutParams as? ConstraintLayout.LayoutParams
                params?.let { p ->
                    p.topMargin = config.cardMarginTopPx
                    view.layoutParams = p
                    Log.d(TAG, "✅ CardLogin: marginTop=${config.cardMarginTopPx}px (${config.cardMarginTopDp.toInt()}dp)")
                    appliedCount++
                } ?: run { Log.w(TAG, "⚠️ cardLogin参数类型错误"); errorCount++ }
            } ?: run { Log.w(TAG, "⚠️ cardLogin为null"); errorCount++ }
            
            views["tabContainer"]?.let { view ->
                val layoutParams = view.layoutParams
                layoutParams.height = config.tabHeightPx
                view.layoutParams = layoutParams
                Log.d(TAG, "✅ TabContainer高度: ${config.tabHeightPx}px (${config.tabHeightDp.toInt()}dp)")
                appliedCount++
            } ?: run { Log.w(TAG, "⚠️ tabContainer为null"); errorCount++ }
            
            listOf("tabSms", "tabPassword", "tabEmail").forEach { tabId ->
                views[tabId]?.let { view ->
                    (view as? android.widget.TextView)?.let { tv ->
                        tv.textSize = config.tabTextSizeSp
                        appliedCount++
                    }
                }
            }
            
            Log.i(TAG, "✅ 布局配置应用完成: 成功${appliedCount}项, 失错${errorCount}项")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 应用布局配置失败", e)
            throw e
        }
    }
}
