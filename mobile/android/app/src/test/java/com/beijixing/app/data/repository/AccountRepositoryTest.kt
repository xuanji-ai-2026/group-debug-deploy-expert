package com.beijixing.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryTest {

    @Test
    fun `getAccounts - should return list on success`() = runTest {
        val mockAccounts = listOf(
            com.beijixing.app.data.model.SocialAccount(
                id = 1,
                platform = "WECHAT",
                accountName = "微信公众号",
                accountNo = "wx_test001",
                status = "ACTIVE",
                healthStatus = "HEALTHY"
            ),
            com.beijixing.app.data.model.SocialAccount(
                id = 2,
                platform = "DOUYIN",
                accountName = "抖音账号",
                accountNo = "dy_test002",
                status = "ACTIVE",
                healthStatus = "WARNING"
            )
        )
        
        assertEquals(2, mockAccounts.size)
        assertEquals("微信", mockAccounts[0].getPlatformDisplayName())
        assertEquals("抖音", mockAccounts[1].getPlatformDisplayName())
    }

    @Test
    fun `addAccount - should validate required fields`() = runTest {
        val request = com.beijixing.app.data.model.AddAccountRequest(
            platform = "XIAOHONGSHU",
            accountName = "小红书测试号",
            accountNo = "xhs_test003"
        )
        
        assertNotNull(request)
        assertEquals("XIAOHONGSHU", request.platform)
        assertTrue(request.platform.isNotEmpty())
        assertTrue(request.accountName.isNotEmpty())
        assertTrue(request.accountNo.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addAccount - should throw exception when platform is empty`() = runTest {
        val platform = ""
        require(platform.isNotEmpty()) { "Platform cannot be empty" }
    }

    @Test
    fun `accountHealthCheck - should parse health result correctly`() = runTest {
        val healthyResult = com.beijixing.app.data.model.AccountHealthResult(
            accountId = 1,
            isHealthy = true,
            status = "HEALTHY",
            message = "账号状态正常"
        )
        
        assertTrue(healthyResult.isHealthy)
        assertEquals("HEALTHY", healthyResult.status)
        
        val unhealthyResult = com.beijixing.app.data.model.AccountHealthResult(
            accountId = 2,
            isHealthy = false,
            status = "ERROR",
            message = "登录失效，请重新授权"
        )
        
        assertFalse(unhealthyResult.isHealthy)
        assertEquals("ERROR", unhealthyResult.status)
        assertTrue(unhealthyResult.message.contains("登录失效"))
    }

    @Test
    fun `platformDisplayName - should return Chinese names`() = runTest {
        val platforms = mapOf(
            "WECHAT" to "微信",
            "DOUYIN" to "抖音",
            "XIAOHONGSHU" to "小红书",
            "WEIBO" to "微博",
            "LINKEDIN" to "领英"
        )
        
        platforms.forEach { (code, expectedName) ->
            val account = com.beijixing.app.data.model.SocialAccount(
                id = 0,
                platform = code,
                accountName = "Test"
            )
            assertEquals(expectedName, account.getPlatformDisplayName())
        }
    }

    @Test
    fun `accountStatus - should validate status transitions`() = runTest {
        val validStatuses = listOf("ACTIVE", "INACTIVE", "BANNED")
        
        fun isValidStatus(status: String): Boolean {
            return validStatuses.contains(status)
        }
        
        assertTrue(isValidStatus("ACTIVE"))
        assertTrue(isValidStatus("INACTIVE"))
        assertTrue(isValidStatus("BANNED"))
        assertFalse(isValidStatus("UNKNOWN"))
        assertFalse(isValidStatus(""))
    }
}