package com.beijixing.app.util

import android.util.Log
import android.view.MotionEvent
import android.view.View

object DebugLogger {
    
    private const val TAG = "BeiJiXing_Debug"
    private const val ENABLED = true
    
    private var currentActivity: String = "Unknown"
    
    fun setContext(activityName: String) {
        currentActivity = activityName
        i("=== $activityName 开始 ===")
    }
    
    fun v(message: String) { if (ENABLED) Log.v(TAG, formatMessage(message)) }
    fun d(message: String) { if (ENABLED) Log.d(TAG, formatMessage(message)) }
    fun i(message: String) { if (ENABLED) Log.i(TAG, formatMessage(message)) }
    fun w(message: String) { if (ENABLED) Log.w(TAG, formatMessage(message)) }
    fun e(message: String) { if (ENABLED) Log.e(TAG, formatMessage(message)) }
    fun e(message: String, throwable: Throwable) { 
        if (ENABLED) Log.e(TAG, formatMessage(message), throwable) 
    }
    
    private fun formatMessage(message: String): String {
        val threadName = Thread.currentThread().name
        return "[$currentActivity][$threadName] $message"
    }
    
    object UI {
        
        fun logViewInfo(view: View?, viewName: String) {
            if (!ENABLED || view == null) return
            
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            
            d("""
            📐 View信息: $viewName
               - 类型: ${view.javaClass.simpleName}
               - ID: ${if (view.id > 0) "${resourcesName(view.id)}" else "无ID"}
               - 位置: (${location[0]}, ${location[1]})
               - 尺寸: ${view.width}×${view.height}px
               - 可见性: ${visibilityToString(view.visibility)}
               - 可点击: ${view.isClickable}
               - 可聚焦: ${view.isFocusable}
               - 已启用: ${view.isEnabled}
            """.trimIndent())
        }
        
        fun logTouchEvent(view: View?, event: MotionEvent?, viewName: String) {
            if (!ENABLED || view == null || event == null) return
            
            val actionString = when (event.action) {
                MotionEvent.ACTION_DOWN -> "按下(ACTION_DOWN)"
                MotionEvent.ACTION_UP -> "抬起(ACTION_UP)"
                MotionEvent.ACTION_MOVE -> "移动(ACTION_MOVE)"
                MotionEvent.ACTION_CANCEL -> "取消(ACTION_CANCEL)"
                else -> "其他(${event.action})"
            }
            
            d("""
            👆 触摸事件: $viewName
               - 动作: $actionString
               - 坐标: (${event.x.toInt()}, ${event.y.toInt()})
               - 原始坐标: (${event.rawX.toInt()}, ${event.rawY.toInt()})
               - 压力: ${event.pressure}
               - 触摸面积: ${(event.size * 100).toInt()}%
            """.trimIndent())
        }
        
        fun logTabSwitch(fromMode: String, toMode: String) {
            d("🔄 Tab切换: $fromMode → $toMode")
        }
        
        fun logVisibilityChange(viewName: String, visibility: Int) {
            d("👁️ 可见性变化: $viewName → ${visibilityToString(visibility)}")
        }
        
        private fun visibilityToString(visibility: Int): String {
            return when (visibility) {
                View.VISIBLE -> "VISIBLE(可见)"
                View.INVISIBLE -> "INVISIBLE(不可见但占位)"
                View.GONE -> "GONE(完全隐藏)"
                else -> "UNKNOWN($visibility)"
            }
        }
        
        private fun resourcesName(resId: Int): String {
            try {
                return "R.id.${Integer.toHexString(resId)}"
            } catch (e: Exception) {
                return "未知"
            }
        }
    }
    
    object Network {
        fun requestStart(url: String) { d("🌐 网络请求开始: $url") }
        fun requestSuccess(url: String, code: Int) { i("✅ 网络请求成功: $url ($code)") }
        fun requestError(url: String, error: String) { e("❌ 网络请求失败: $url - $error") }
        fun responseTime(url: String, timeMs: Long) { d("⏱️ 响应时间: $url - ${timeMs}ms") }
    }
    
    object Lifecycle {
        fun onCreate(activity: String) { i("🎬 onCreate: $activity") }
        fun onStart(activity: String) { d("▶️ onStart: $activity") }
        fun onResume(activity: String) { d("▶️ onResume: $activity") }
        fun onPause(activity: String) { d("⏸️ onPause: $activity") }
        fun onStop(activity: String) { d("⏹️ onStop: $activity") }
        fun onDestroy(activity: String) { w("💥 onDestroy: $activity") }
    }
    
    object Performance {
        private val startTimeMap = mutableMapOf<String, Long>()
        
        fun startOperation(operation: String) {
            startTimeMap[operation] = System.currentTimeMillis()
            d("⏳ 操作开始: $operation")
        }
        
        fun endOperation(operation: String) {
            val startTime = startTimeMap[operation] ?: return
            val duration = System.currentTimeMillis() - startTime
            i("✅ 操作完成: $operation (${duration}ms)")
            startTimeMap.remove(operation)
        }
        
        fun measure(tag: String, block: () -> Unit) {
            val start = System.currentTimeMillis()
            block()
            val duration = System.currentTimeMillis() - start
            d("⏱️ 性能测量: $tag = ${duration}ms")
        }
    }
}