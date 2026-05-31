package com.beijixing.app.util

import android.content.Context
import android.widget.Toast

object ToastUtil {

    fun showShort(context: Context?, message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun showLong(context: Context?, message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }

    fun show(context: Context?, message: String) {
        showShort(context, message)
    }
}
