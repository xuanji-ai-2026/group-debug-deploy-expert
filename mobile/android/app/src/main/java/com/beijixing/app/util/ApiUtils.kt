package com.beijixing.app.util

import com.beijixing.app.data.model.ApiResponse
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

object ApiUtils {

    fun <T> safeExtractData(response: Response<ApiResponse<T>>): T? {
        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess() == true) body.data else null
            } else null
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    }

    suspend inline fun <T> safeApiCall(crossinline block: suspend () -> Response<ApiResponse<T>>): Result<T?> {
        return try { Result.success(safeExtractData(block())) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    fun <T> isSuccess(response: Response<ApiResponse<T>>): Boolean =
        try { response.isSuccessful && response.body()?.isSuccess() == true }
        catch (_: Exception) { false }

    fun <T> getErrorMessage(response: Response<ApiResponse<T>>, default: String = "Error"): String =
        try { if (!response.isSuccessful) "HTTP ${response.code()}" else response.body()?.msg ?: default }
        catch (_: Exception) { default }
}
