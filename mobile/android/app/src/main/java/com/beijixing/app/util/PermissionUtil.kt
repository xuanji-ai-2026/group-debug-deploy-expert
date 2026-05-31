package com.beijixing.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {

    val PERMISSION_MICROPHONE = arrayOf(Manifest.permission.RECORD_AUDIO)

    val PERMISSION_NOTIFICATION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    val PERMISSION_STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val PERMISSION_CAMERA = arrayOf(Manifest.permission.CAMERA)

    val PERMISSION_LOCATION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val PERMISSION_PHONE = arrayOf(Manifest.permission.READ_PHONE_STATE)

    val PERMISSION_CONTACTS = arrayOf(Manifest.permission.READ_CONTACTS)

    val ALL_REQUIRED_PERMISSIONS = mutableListOf<String>().apply {
        addAll(PERMISSION_STORAGE)
        addAll(PERMISSION_CAMERA)
        addAll(PERMISSION_LOCATION)
        addAll(PERMISSION_MICROPHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addAll(PERMISSION_NOTIFICATION)
        }
    }.toTypedArray()

    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasStoragePermission(context: Context): Boolean =
        hasPermissions(context, PERMISSION_STORAGE)

    fun hasCameraPermission(context: Context): Boolean =
        hasPermissions(context, PERMISSION_CAMERA)

    fun hasLocationPermission(context: Context): Boolean =
        hasPermissions(context, PERMISSION_LOCATION)

    fun hasMicrophonePermission(context: Context): Boolean =
        hasPermissions(context, PERMISSION_MICROPHONE)

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(context, PERMISSION_NOTIFICATION)
        } else {
            true
        }

    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ): Boolean {
        if (hasPermissions(activity, permissions)) {
            return true
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
        return false
    }

    fun requestStoragePermission(activity: Activity, requestCode: Int): Boolean =
        requestPermissions(activity, PERMISSION_STORAGE, requestCode)

    fun requestCameraPermission(activity: Activity, requestCode: Int): Boolean =
        requestPermissions(activity, PERMISSION_CAMERA, requestCode)

    fun requestLocationPermission(activity: Activity, requestCode: Int): Boolean =
        requestPermissions(activity, PERMISSION_LOCATION, requestCode)

    fun requestMicrophonePermission(activity: Activity, requestCode: Int): Boolean =
        requestPermissions(activity, PERMISSION_MICROPHONE, requestCode)

    fun requestNotificationPermission(activity: Activity, requestCode: Int): Boolean {
        if (!hasNotificationPermission(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(activity, PERMISSION_NOTIFICATION, requestCode)
            }
            return false
        }
        return true
    }

    fun requestAllRequiredPermissions(activity: Activity, requestCode: Int): Boolean {
        return requestPermissions(activity, ALL_REQUIRED_PERMISSIONS, requestCode)
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PermissionUtil", "无法打开应用设置", e)
        }
    }

    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "相机权限：用于扫描二维码、拍摄截图"
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO -> "存储权限：用于保存爬取数据、导出报表"
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "位置权限：用于基于位置的内容推荐"
            Manifest.permission.RECORD_AUDIO -> "麦克风权限：用于语音输入、语音搜索"
            Manifest.permission.POST_NOTIFICATIONS -> "通知权限：用于接收任务完成提醒"
            Manifest.permission.READ_PHONE_STATE -> "电话权限：用于设备识别"
            Manifest.permission.READ_CONTACTS -> "通讯录权限：用于导入联系人"
            else -> "必要权限"
        }
    }
}

object PermissionCodes {
    const val REQUEST_CODE_STORAGE = 1001
    const val REQUEST_CODE_CAMERA = 1002
    const val REQUEST_CODE_LOCATION = 1003
    const val REQUEST_CODE_MICROPHONE = 1004
    const val REQUEST_CODE_NOTIFICATION = 1005
    const val REQUEST_CODE_ALL = 1999
}
