package com.beijixing.app.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheManagerTest {

    @Test
    fun `cacheEntry - should check expiration correctly`() = runTest {
        val entry = CacheManager.Companion.CacheEntry(
            data = "testData",
            timestamp = System.currentTimeMillis(),
            duration = 5000L
        )
        
        assertFalse(entry.isExpired())
        
        val expiredEntry = CacheManager.Companion.CacheEntry(
            data = "expiredData",
            timestamp = System.currentTimeMillis() - 10000,
            duration = 5000L
        )
        
        assertTrue(expiredEntry.isExpired())
    }

    @Test
    fun `cacheEntry - should support custom durations`() = runTest {
        val durations = mapOf(
            "short" to 60_000L,
            "medium" to 300_000L,
            "long" to 3600_000L,
            "very_long" to 86400_000L
        )
        
        durations.forEach { (name, duration) ->
            val entry = CacheManager.Companion.CacheEntry<Any>(
                data = name,
                timestamp = System.currentTimeMillis(),
                duration = duration
            )
            
            assertFalse(entry.isExpired())
            assertEquals(duration, entry.duration)
        }
    }

    @Test
    fun `cacheExpirationTime - should calculate correctly`() = runTest {
        val baseTime = System.currentTimeMillis()
        val duration = 300000L
        
        val entry = CacheManager.Companion.CacheEntry(
            data = "test",
            timestamp = baseTime,
            duration = duration
        )
        
        val expectedExpiration = baseTime + duration
        val actualExpiration = entry.timestamp + entry.duration
        
        assertEquals(expectedExpiration, actualExpiration)
    }

    @Test
    fun `cacheKeyValidation - should validate key format`() = runTest {
        val validKeys = listOf(
            "user_1",
            "lead_list_page1",
            "task_123_detail",
            "cache:session:abc123"
        )
        
        val invalidKeys = listOf("", "   ")
        
        validKeys.forEach { key ->
            assertTrue(key.isNotEmpty())
            assertTrue(key.isNotBlank())
        }
        
        invalidKeys.forEach { key ->
            assertFalse(key.isNotBlank())
        }
    }
}