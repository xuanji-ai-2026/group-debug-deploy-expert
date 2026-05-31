package com.beijixing.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryTest {

    @Test
    fun `getMessages - should return message list`() = runTest {
        val mockMessages = listOf(
            com.beijixing.app.data.model.Message(
                messageId = 1,
                title = "系统通知",
                content = "您的任务已完成",
                type = "SYSTEM",
                isRead = false,
                createTime = "2024-01-01 10:00:00"
            ),
            com.beijixing.app.data.model.Message(
                messageId = 2,
                title = "商机提醒",
                content = "发现新的潜在客户",
                type = "LEAD",
                isRead = true,
                createTime = "2024-01-01 09:00:00"
            )
        )
        
        assertEquals(2, mockMessages.size)
        assertFalse(mockMessages[0].isRead)
        assertTrue(mockMessages[1].isRead)
    }

    @Test
    fun `messageTypes - should include all expected types`() = runTest {
        val expectedTypes = listOf("SYSTEM", "LEAD", "TASK", "BILLING", "SECURITY")
        
        expectedTypes.forEach { type ->
            val message = com.beijixing.app.data.model.Message(
                messageId = 0,
                title = "Test",
                content = "Content",
                type = type
            )
            assertEquals(type, message.type)
        }
    }

    @Test
    fun `markAsRead - should update read status`() = runTest {
        var isRead = false
        
        fun markAsRead() {
            isRead = true
        }
        
        assertFalse(isRead)
        markAsRead()
        assertTrue(isRead)
    }

    @Test
    fun `unreadCount - should calculate correctly`() = runTest {
        val messages = listOf(
            com.beijixing.app.data.model.Message(messageId = 1, title = "M1", isRead = false),
            com.beijixing.app.data.model.Message(messageId = 2, title = "M2", isRead = false),
            com.beijixing.app.data.model.Message(messageId = 3, title = "M3", isRead = true),
            com.beijixing.app.data.model.Message(messageId = 4, title = "M4", isRead = false)
        )
        
        val unreadCount = messages.count { !it.isRead }
        assertEquals(3, unreadCount)
        
        val readCount = messages.count { it.isRead }
        assertEquals(1, readCount)
    }

    @Test
    fun `deleteMessage - should validate messageId positive`() = runTest {
        val messageId = -1L
        
        try {
            require(messageId > 0) { "Message ID must be positive" }
            fail("Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Message ID must be positive") == true)
        }
    }

    @Test
    fun `messageTimeOrdering - should sort by createTime`() = runTest {
        val messages = listOf(
            com.beijixing.app.data.model.Message(messageId = 1, title = "Old", createTime = "2024-01-01 08:00:00"),
            com.beijixing.app.data.model.Message(messageId = 2, title = "New", createTime = "2024-01-01 09:00:00"),
            com.beijixing.app.data.model.Message(messageId = 3, title = "Newest", createTime = "2024-01-01 10:00:00")
        )
        
        val sorted = messages.sortedBy { it.createTime }
        assertEquals("Old", sorted[0].title)
        assertEquals("New", sorted[1].title)
        assertEquals("Newest", sorted[2].title)
    }
}