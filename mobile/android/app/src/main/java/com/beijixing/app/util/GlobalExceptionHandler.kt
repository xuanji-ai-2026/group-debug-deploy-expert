package com.beijixing.app.util

import android.content.Context
import android.widget.Toast
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

object GlobalExceptionHandler {

    fun handle(error: Throwable, context: Context) {
        if (error is CancellationException) return

        val message = when (error) {
            is UnknownHostException, is ConnectException -> "Network unavailable. Please check your connection."
            is SocketTimeoutException -> "Connection timeout. Please try again."
            is IOException -> "Network error occurred."
            is HttpException -> {
                when (error.code()) {
                    401 -> "Unauthorized. Please login again."
                    403 -> "Access forbidden."
                    404 -> "Resource not found."
                    in 500..599 -> "Server error. Please try again."
                    else -> "HTTP Error: ${error.code()}"
                }
            }
            else -> error.message ?: "Unknown error occurred."
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        android.util.Log.e("GlobalException", "Error", error)
    }
}
