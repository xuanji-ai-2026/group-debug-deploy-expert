package com.beijixing.app.data.repository

import android.content.Context
import com.beijixing.app.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

abstract class BaseRepository protected constructor(
    @ApplicationContext protected val context: Context,
    protected val apiClient: ApiClient
) {
    
    protected suspend fun <T> withIoContext(block: suspend () -> T): Result<T> {
        return try {
            Result.success(withContext(Dispatchers.IO) { block() })
        } catch (e: Exception) {
            android.util.Log.e("BaseRepository", "Error in IO operation", e)
            Result.failure(e)
        }
    }
    
    protected suspend fun <T> withDefaultContext(block: suspend () -> T): Result<T> {
        return try {
            Result.success(withContext(Dispatchers.Default) { block() })
        } catch (e: Exception) {
            android.util.Log.e("BaseRepository", "Error in computation", e)
            Result.failure(e)
        }
    }
}
