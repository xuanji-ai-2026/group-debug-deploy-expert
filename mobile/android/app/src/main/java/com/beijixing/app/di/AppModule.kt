package com.beijixing.app.di

import android.content.Context
import com.beijixing.app.data.remote.ApiClient
import com.beijixing.app.data.remote.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt依赖注入模块
 * 提供全局单例对象
 *
 * @see com.beijixing.app.BxApplication 使用@HiltAndroidApp启用Hilt
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供Context
     * 使用@ApplicationContext确保使用Application级别的Context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    /**
     * 提供PreferencesManager单例
     * 用于Token和用户信息的持久化存储
     */
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    /**
     * 提供ApiClient单例
     * Retrofit客户端配置（直接使用PreferencesManager）
     */
    @Provides
    @Singleton
    fun provideApiClient(preferencesManager: PreferencesManager): ApiClient {
        return ApiClient(preferencesManager)
    }
}
