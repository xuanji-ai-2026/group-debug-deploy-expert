package com.beijixing.app.util

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val cache = mutableMapOf<String, CacheEntry<Any>>()
    
    companion object {
        const val DEFAULT_CACHE_DURATION = 5 * 60 * 1000L // 5分钟
        
        data class CacheEntry<T>(
            val data: T,
            val timestamp: Long = System.currentTimeMillis(),
            val duration: Long = DEFAULT_CACHE_DURATION
        ) {
            fun isExpired(): Boolean {
                return System.currentTimeMillis() - timestamp > duration
            }
        }
    }

    suspend fun <T> getOrFetch(
        key: String,
        fetcher: suspend () -> T,
        cacheDuration: Long = DEFAULT_CACHE_DURATION
    ): T {
        return withContext(Dispatchers.IO) {
            synchronized(cache) {
                @Suppress("UNCHECKED_CAST")
                val entry = cache[key] as? CacheEntry<T>
                
                if (entry != null && !entry.isExpired()) {
                    return@withContext entry.data
                }
            }
            
            val freshData = fetcher()

            synchronized(cache) {
                @Suppress("UNCHECKED_CAST")
                cache[key] = CacheEntry(freshData as Any, duration = cacheDuration)

                if (cache.size > 100) {
                    evictExpiredEntries()
                    if (cache.size > 100) {
                        evictOldestEntries(20)
                    }
                }
            }
            
            freshData
        }
    }

    fun <T> put(key: String, data: T, duration: Long = DEFAULT_CACHE_DURATION) {
        synchronized(cache) {
            @Suppress("UNCHECKED_CAST")
            cache[key] = CacheEntry(data as Any, duration = duration)
        }
    }

    fun <T> get(key: String): T? {
        synchronized(cache) {
            @Suppress("UNCHECKED_CAST")
            val entry = cache[key] as? CacheEntry<T>
            
            return if (entry != null && !entry.isExpired()) {
                entry.data
            } else {
                if (entry != null) {
                    cache.remove(key)
                }
                null
            }
        }
    }

    fun remove(key: String) {
        synchronized(cache) {
            cache.remove(key)
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }

    fun clearByPrefix(prefix: String) {
        synchronized(cache) {
            cache.keys.filter { it.startsWith(prefix) }.forEach { key ->
                cache.remove(key)
            }
        }
    }

    private fun evictExpiredEntries() {
        cache.entries.removeAll { (_, value) ->
            value.isExpired()
        }
    }

    private fun evictOldestEntries(count: Int) {
        val sorted = cache.entries.sortedBy { it.value.timestamp }
        sorted.take(count).forEach { (key, _) ->
            cache.remove(key)
        }
    }

    fun getCacheStats(): Map<String, Any> {
        synchronized(cache) {
            return mapOf(
                "total_entries" to cache.size,
                "expired_count" to cache.values.count { it.isExpired() },
                "valid_count" to cache.values.count { !it.isExpired() }
            )
        }
    }
}
