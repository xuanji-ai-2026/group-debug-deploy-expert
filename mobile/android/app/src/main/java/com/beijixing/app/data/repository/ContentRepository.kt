package com.beijixing.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generateContent(@Suppress("UNUSED_PARAMETER") params: Any): Result<String> {
        return Result.success("内容生成中...")
    }

    suspend fun publishContent(@Suppress("UNUSED_PARAMETER") content: Any): Result<Boolean> {
        return Result.success(true)
    }
}
