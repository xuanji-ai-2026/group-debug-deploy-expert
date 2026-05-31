package com.beijixing.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object PerformanceOptimizer {
    
    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    private val compressedImageCache = mutableMapOf<String, ByteArray>()
    
    suspend fun compressImage(
        @Suppress("UNUSED_PARAMETER") context: Context,
        imagePath: String,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920,
        quality: Int = 85
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(imagePath, targetWidth, quality)
            
            compressedImageCache[cacheKey]?.let { return@withContext it }
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            BitmapFactory.decodeFile(imagePath, options)
            
            if (options.outWidth == -1 || options.outHeight == -1) {
                return@withContext null
            }
            
            var sampleSize = 1
            while (options.outWidth / sampleSize > targetWidth ||
                   options.outHeight / sampleSize > targetHeight) {
                sampleSize *= 2
            }
            
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            
            val bitmap = BitmapFactory.decodeFile(imagePath, decodeOptions) ?: return@withContext null
            
            val scaledBitmap = if (bitmap.width > targetWidth || bitmap.height > targetHeight) {
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }
            
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val compressedData = stream.toByteArray()
            
            scaledBitmap.recycle()
            stream.close()
            
            synchronized(compressedImageCache) {
                if (compressedImageCache.size > 50) {
                    val keysToRemove = compressedImageCache.keys.take(10)
                    keysToRemove.forEach { compressedImageCache.remove(it) }
                }
                compressedImageCache[cacheKey] = compressedData
            }
            
            compressedData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    fun addBitmapToCache(key: String, bitmap: Bitmap?) {
        if (bitmap == null || key.isEmpty()) return
        
        memoryCache.put(key, bitmap)
    }

    fun clearImageCache() {
        memoryCache.evictAll()
        synchronized(compressedImageCache) {
            compressedImageCache.clear()
        }
    }

    fun getCacheSize(): Int {
        return memoryCache.size()
    }

    fun getMaxCacheSize(): Int {
        return memoryCache.maxSize()
    }

    private fun generateCacheKey(vararg params: Any): String {
        val input = params.joinToString("_")
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(input.toByteArray())
        
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight &&
                   halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }

    suspend fun optimizeMemoryUsage() = withContext(Dispatchers.Default) {
        System.gc()
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usagePercent = (usedMemory.toDouble() / maxMemory) * 100
        
        if (usagePercent > 80) {
            clearImageCache()
            System.gc()
        }
        
        mapOf(
            "usedMB" to usedMemory / (1024 * 1024),
            "maxMB" to maxMemory / (1024 * 1024),
            "usagePercent" to usagePercent.toInt()
        )
    }

    fun isLowMemoryDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return activityManager.isLowRamDevice
    }

    fun getOptimalThreadPoolSize(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return when {
            availableProcessors <= 2 -> 2
            availableProcessors <= 4 -> 3
            else -> minOf(availableProcessors - 1, 6)
        }
    }
}
