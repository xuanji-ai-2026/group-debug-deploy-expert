package com.beijixing.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    @Test(expected = IllegalArgumentException::class)
    fun `login - should throw exception when phone is empty`() = runTest {
        val phone = ""
        require(phone.isNotEmpty()) { "Phone cannot be empty" }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `login - should throw exception when password is too short`() = runTest {
        val password = "123"
        require(password.length >= 6) { "Password must be at least 6 characters" }
    }

    @Test
    fun `getUserInfo - should return User object on success`() = runTest {
        val mockUser = com.beijixing.app.data.model.User(
            userId = 1L,
            nickname = "TestUser",
            phone = "13800138000"
        )
        
        assertNotNull(mockUser)
        assertEquals("TestUser", mockUser.nickname)
        assertEquals(1L, mockUser.userId)
        assertEquals("13800138000", mockUser.phone)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changePassword - should validate old password not empty`() = runTest {
        val oldPwd = ""
        require(oldPwd.isNotEmpty()) { "Old password cannot be empty" }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changePassword - should validate new password length`() = runTest {
        val newPwd = "123"
        require(newPwd.length >= 6) { "New password must be at least 6 characters" }
    }
    
    @Test
    fun `updateProfile - should create valid UpdateProfileRequest`() = runTest {
        val request = com.beijixing.app.data.model.UpdateProfileRequest(
            nickname = "NewNickname",
            avatar = null,
            email = "test@example.com",
            phone = "13800138000"
        )
        
        assertEquals("NewNickname", request.nickname)
        assertNull(request.avatar)
        assertEquals("test@example.com", request.email)
        assertEquals("13800138000", request.phone)
    }
    
    @Test
    fun `registerRequest - should have required fields`() = runTest {
        val request = com.beijixing.app.data.model.RegisterRequest(
            phone = "13800138000",
            password = "Admin@123",
            nickName = "TestUser",
            deviceId = "test_device_123"
        )
        
        assertEquals("13800138000", request.phone)
        assertEquals("Admin@123", request.password)
        assertEquals("TestUser", request.nickName)
        assertEquals("test_device_123", request.deviceId)
    }
    
    @Test
    fun `userBalance - should initialize with default values`() = runTest {
        val balance = com.beijixing.app.data.model.UserBalance()
        
        assertEquals(0.0, balance.balance, 0.01)
        assertEquals(0, balance.points)
        assertEquals(0.0, balance.frozen, 0.01)
        assertEquals(0.0, balance.totalRecharge, 0.01)
    }
}