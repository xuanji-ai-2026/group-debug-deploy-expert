package com.beijixing.app

import android.app.Application
import android.util.Log
import com.beijixing.app.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * 北极星AI商机获客系统 - Application入口
 *
 * 功能说明：
 * - 初始化 Hilt 依赖注入容器
 * - 全局Application级别的配置
 * - 进程级别的生命周期管理
 *
 * 产品需求对应：
 * - 5.2节 多模态交互系统 - 语音输入初始化
 * - 5.1节 权限申请 - 自启动权限申请入口
 * - 6.5节 自动发布与运营 - 后台任务服务注册
 */
@HiltAndroidApp
class BxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        CrashLogger.init(this)
        
        initGlobalConfig()
    }

    /**
     * 初始化全局配置
     * 可在此处配置：
     * - Crash监控
     * - 性能监控
     * - 推送服务
     * - 语音识别SDK
     */
    private fun initGlobalConfig() {
        Log.d("BxApplication", "北极星SDK初始化(预留)")
        // 1. 初始化极光推送/JPush（用于消息推送、商机预警）
        // 2. 初始化语音识别SDK（支持普通话、粤语、四川话等方言）
        // 3. 初始化设备指纹（用于账号安全 4.3.2节）
    }

    companion object {
        @Volatile
        private lateinit var instance: BxApplication

        /**
         * 获取Application单例
         * 用于全局访问Application上下文
         */
        fun getInstance(): BxApplication = instance
    }
}
