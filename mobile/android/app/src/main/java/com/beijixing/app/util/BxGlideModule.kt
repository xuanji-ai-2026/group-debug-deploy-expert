package com.beijixing.app.util

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@com.bumptech.glide.annotation.GlideModule
class BxGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        val requestOptions = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig()

        builder.setDefaultRequestOptions(requestOptions)
    }
}
