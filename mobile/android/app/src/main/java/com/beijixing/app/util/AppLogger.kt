package com.beijixing.app.util

import android.content.Context
import android.util.Log
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object AppLogger {
    
    private const val TAG = "BeiJiXing_App"
    private const val MAX_LOG_ENTRIES = 500
    
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String,
        val threadName: String,
        val deviceInfo: String? = null
    ) {
        override fun toString(): String = "[$timestamp] [$level/$threadName] $tag: $message"
    }
    
    enum class LogLevel(val priority: Int, val prefix: String) {
        VERBOSE(2, "V"),
        DEBUG(3, "D"),
        INFO(4, "I"),
        WARN(5, "W"),
        ERROR(6, "E")
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun v(tag: String, message: String) {
        log(LogLevel.VERBOSE, tag, message)
    }
    
    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }
    
    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }
    
    fun w(tag: String, message: String) {
        log(LogLevel.WARN, tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level.prefix,
            tag = tag,
            message = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message,
            threadName = Thread.currentThread().name
        )
        
        logQueue.offer(entry)
        
        while (logQueue.size > MAX_LOG_ENTRIES) {
            logQueue.poll()
        }
        
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, entry.toString())
            LogLevel.DEBUG -> Log.d(tag, entry.toString())
            LogLevel.INFO -> Log.i(tag, entry.toString())
            LogLevel.WARN -> Log.w(tag, entry.toString())
            LogLevel.ERROR -> Log.e(tag, entry.toString())
        }
    }
    
    fun logDeviceInfo(context: Context) {
        try {
            val deviceInfo = DeviceInfoManager.getDeviceInfo(context)
            i(TAG, "\n${deviceInfo.toLogString()}")
            
            addDeviceMetadata(deviceInfo)
        } catch (e: Exception) {
            e(TAG, "记录设备信息失败", e)
        }
    }
    
    private fun addDeviceMetadata(deviceInfo: DeviceInfoManager.DeviceInfo) {
        val metadata = "${deviceInfo.deviceName} | ${deviceInfo.screenWidthPx}x${deviceInfo.screenHeightPx} | API${deviceInfo.androidVersion} | v${deviceInfo.appVersionName}"
        
        logQueue.forEach { entry ->
            if (entry.deviceInfo == null) {
                entry.copy(deviceInfo = metadata)
            }
        }
    }
    
    fun logUIEvent(component: String, action: String, details: String = "") {
        d("UI_Event", "$component → $action ${if (details.isNotEmpty()) "| $details" else ""}")
    }
    
    fun logPerformance(operation: String, startTimeMs: Long, endTimeMs: Long = System.currentTimeMillis()) {
        val duration = endTimeMs - startTimeMs
        val level = if (duration > 1000) LogLevel.WARN else LogLevel.DEBUG
        log(level, "Performance", "⏱ $operation: ${duration}ms")
    }
    
    fun logApiCall(method: String, url: String, durationMs: Long, success: Boolean, statusCode: Int? = null) {
        val status = if (success) "✅" else "❌"
        val statusInfo = statusCode?.let { "($it)" } ?: ""
        i("API_Call", "$status $method $url $statusInfo [${durationMs}ms]")
    }
    
    fun logUserAction(action: String, target: String, result: String = "success") {
        i("UserAction", "👤 $action → $target [$result]")
    }
    
    fun logErrorWithContext(context: String, error: Throwable, additionalInfo: Map<String, Any?> = emptyMap()) {
        val infoStr = if (additionalInfo.isNotEmpty()) {
            "\n附加信息:\n${additionalInfo.map { "  ${it.key}: ${it.value}" }.joinToString("\n")}"
        } else {
            ""
        }
        e("Error", "❌ $context$infoStr", error)
    }
    
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logQueue.toList().takeLast(count)
    }
    
    fun getAllLogs(): List<LogEntry> {
        return logQueue.toList()
    }
    
    fun exportLogsToFile(context: Context): String? {
        return try {
            val fileName = "beijixing_log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = context.getExternalFilesDir(null)?.resolve(fileName)
            
            file?.let { f ->
                PrintWriter(FileWriter(f)).use { writer ->
                    writer.println("═══════════════════════════════════════")
                    writer.println("北极星AI应用日志")
                    writer.println("导出时间: ${dateFormat.format(Date())}")
                    writer.println("日志条数: ${logQueue.size}")
                    writer.println("═══════════════════════════════════════\n")
                    
                    logQueue.forEach { entry ->
                        writer.println(entry.toString())
                    }
                }
                
                i(TAG, "日志已导出到: ${f.absolutePath}")
                f.absolutePath
            }
        } catch (e: Exception) {
            e(TAG, "导出日志失败", e)
            null
        }
    }
    
    fun clearLogs() {
        logQueue.clear()
        i(TAG, "日志已清除")
    }
    
    fun generateDiagnosticReport(context: Context): String {
        val sb = StringBuilder()
        
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("🔍 北极星AI诊断报告")
        sb.appendLine("生成时间: ${dateFormat.format(Date())}")
        sb.appendLine("═══════════════════════════════════════\n")
        
        try {
            sb.appendLine("📱 设备信息:")
            sb.appendLine(DeviceInfoManager.getDeviceInfo(context).toLogString())
            sb.appendLine()
        } catch (e: Exception) {
            sb.appendLine("❌ 获取设备信息失败: ${e.message}\n")
        }
        
        val recentErrors = logQueue.filter { it.level == LogLevel.ERROR.prefix }.takeLast(20)
        if (recentErrors.isNotEmpty()) {
            sb.appendLine("❌ 最近错误 (${recentErrors.size}条):")
            recentErrors.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }
        
        val recentWarnings = logQueue.filter { it.level == LogLevel.WARN.prefix }.takeLast(10)
        if (recentWarnings.isNotEmpty()) {
            sb.appendLine("⚠️ 最近警告 (${recentWarnings.size}条):")
            recentWarnings.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }
        
        sb.appendLine("📊 统计信息:")
        sb.appendLine("  总日志数: ${logQueue.size}")
        sb.appendLine("  错误数: ${logQueue.count { it.level == LogLevel.ERROR.prefix }}")
        sb.appendLine("  警告数: ${logQueue.count { it.level == LogLevel.WARN.prefix }}")
        sb.appendLine("═══════════════════════════════════════")
        
        return sb.toString()
    }
}
