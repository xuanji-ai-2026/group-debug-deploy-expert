package com.beijixing.app.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.beijixing.app.BuildConfig
import com.beijixing.app.R
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val UPDATE_CHECK_URL = "https://www.beijixing-ai.com/api/app/version-check"
    private const val DOWNLOAD_TIMEOUT = 60L
    
    data class UpdateInfo(
        val hasUpdate: Boolean,
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String?,
        val changelog: String?,
        val isForceUpdate: Boolean = false,
        val apkSize: Long = 0
    )

    interface UpdateCallback {
        fun onCheckComplete(updateInfo: UpdateInfo)
        fun onDownloadProgress(progress: Int)
        fun onDownloadSuccess(apkFile: File)
        fun onError(error: String)
    }

    private var downloadJob: Job? = null
    private var isDownloading = false

    fun checkForUpdate(context: Context, callback: UpdateCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("$UPDATE_CHECK_URL?currentVersion=${BuildConfig.VERSION_CODE}&platform=android")
                    .build()

                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val code = json.optInt("code", -1)
                        
                        if (code == 0 || code == 200) {
                            val data = json.optJSONObject("data")
                            if (data != null) {
                                val latestVersionCode = data.optInt("versionCode", 0)
                                val hasUpdate = latestVersionCode > BuildConfig.VERSION_CODE
                                
                                val updateInfo = UpdateInfo(
                                    hasUpdate = hasUpdate,
                                    versionCode = latestVersionCode,
                                    versionName = data.optString("versionName", ""),
                                    downloadUrl = data.optString("downloadUrl", null),
                                    changelog = data.optString("changelog", null),
                                    isForceUpdate = data.optBoolean("forceUpdate", false),
                                    apkSize = data.optLong("apkSize", 0)
                                )
                                
                                withContext(Dispatchers.Main) {
                                    callback.onCheckComplete(updateInfo)
                                }
                                return@launch
                            }
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    callback.onCheckComplete(UpdateInfo(
                        hasUpdate = false,
                        versionCode = BuildConfig.VERSION_CODE,
                        versionName = BuildConfig.VERSION_NAME,
                        downloadUrl = null,
                        changelog = null
                    ))
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "检查更新失败", e)
                withContext(Dispatchers.Main) {
                    callback.onError("检查更新失败: ${e.message}")
                }
            }
        }
    }

    fun showUpdateDialog(activity: Activity, updateInfo: UpdateInfo, callback: UpdateCallback) {
        val builder = AlertDialog.Builder(activity)
            .setTitle("发现新版本 v${updateInfo.versionName}")
            .setMessage(
                """
                更新内容：
                ${updateInfo.changelog ?: "性能优化和Bug修复"}
                
                文件大小：${formatFileSize(updateInfo.apkSize)}
                """.trimIndent()
            )
        
        if (updateInfo.isForceUpdate) {
            builder.setCancelable(false)
            builder.setPositiveButton("立即更新") { _, _ ->
                startDownload(activity, updateInfo, callback)
            }
        } else {
            builder.setPositiveButton("立即更新") { _, _ ->
                startDownload(activity, updateInfo, callback)
            }
            builder.setNegativeButton("稍后再说") { dialog, _ ->
                dialog.dismiss()
            }
        }
        
        builder.show()
    }

    fun startDownload(context: Context, updateInfo: UpdateInfo, callback: UpdateCallback) {
        if (isDownloading) return
        
        val downloadUrl = updateInfo.downloadUrl
        if (downloadUrl.isNullOrEmpty()) {
            callback.onError("下载链接无效")
            return
        }
        
        isDownloading = true
        
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(downloadUrl).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        callback.onError("下载失败: HTTP ${response.code}")
                    }
                    return@launch
                }
                
                val body = response.body
                if (body == null) {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        callback.onError("下载响应为空")
                    }
                    return@launch
                }
                
                val totalSize = body.contentLength()
                val apkFile = getApkFile(context, updateInfo.versionName)
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    
                    if (totalSize > 0) {
                        val progress = ((totalRead * 100) / totalSize).toInt()
                        withContext(Dispatchers.Main) {
                            callback.onDownloadProgress(progress)
                        }
                    }
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    callback.onDownloadSuccess(apkFile)
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "下载APK失败", e)
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    callback.onError("下载失败: ${e.message}")
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "安装APK失败", e)
            installApkFallback(context, apkFile)
        }
    }

    private fun installApkFallback(context: Context, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "备用安装方式也失败", e)
        }
    }

    private fun getApkFile(context: Context, versionName: String): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return File(downloadDir, "BeijiXingAI_v${versionName}.apk")
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        isDownloading = false
    }

    fun clearCachedApks(context: Context) {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { it.delete() }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "清理缓存APK失败", e)
        }
    }
}

object UpdateConstants {
    const val REQUEST_CODE_INSTALL_PERMISSION = 1001
    
    fun isUpdateAvailable(currentVersionCode: Int, latestVersionCode: Int): Boolean {
        return latestVersionCode > currentVersionCode
    }
}
