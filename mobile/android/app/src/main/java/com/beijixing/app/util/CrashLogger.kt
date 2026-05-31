package com.beijixing.app.util

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    
    private const val TAG = "CrashLogger"
    private const val CRASH_DIR = "crash_logs"
    
    private var isInitialized = false
    
    fun init(context: Context) {
        if (isInitialized) return
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                saveCrashLog(context, thread, exception)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        isInitialized = true
    }
    
    private fun saveCrashLog(context: Context, thread: Thread, exception: Throwable) {
        try {
            val crashDir = File(context.getExternalFilesDir(null), CRASH_DIR)
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.log")
            
            FileWriter(crashFile).use { writer ->
                writer.apply {
                    append("=== 北极星AI 崩溃日志 ===\n\n")
                    append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    append("设备: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                    append("Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                    append("APP版本: ${com.beijixing.app.BuildConfig.VERSION_NAME} (${com.beijixing.app.BuildConfig.VERSION_CODE})\n")
                    append("\n线程信息:\n")
                    append("  名称: ${thread.name}\n")
                    append("  ID: ${thread.id}\n")
                    append("\n异常类型: ${exception.javaClass.simpleName}\n")
                    append("异常消息: ${exception.message}\n\n")
                    append("堆栈跟踪:\n")
                    
                    PrintWriter(this).use { pw ->
                        exception.printStackTrace(pw)
                    }
                    
                    append("\n\n=== 系统信息 ===\n")
                    append("可用内存: ${Runtime.getRuntime().freeMemory() / 1024 / 1024}MB / ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB\n")
                    append("存储空间: ${Environment.getDataDirectory().freeSpace / 1024 / 1024}MB 可用\n")
                    
                    flush()
                }
            }
            
            android.util.Log.e(TAG, "崩溃日志已保存到: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "保存崩溃日志失败", e)
        }
    }
    
    fun getCrashLogs(context: Context): List<File> {
        val crashDir = File(context.getExternalFilesDir(null), CRASH_DIR)
        return if (crashDir.exists()) {
            crashDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun clearCrashLogs(context: Context) {
        val crashDir = File(context.getExternalFilesDir(null), CRASH_DIR)
        crashDir.listFiles()?.forEach { it.delete() }
    }
    
    fun getLatestCrashLogContent(context: Context): String? {
        val logs = getCrashLogs(context)
        return logs.firstOrNull()?.readText()
    }
}
