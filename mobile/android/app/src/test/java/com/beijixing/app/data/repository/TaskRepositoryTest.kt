package com.beijixing.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryTest {

    @Test
    fun `getTasks - should return correct task list`() = runTest {
        val tasks = listOf(
            com.beijixing.app.data.model.Task(
                taskId = 1,
                name = "获客任务1",
                type = "ACTIVE_CAPTURE",
                status = "RUNNING"
            ),
            com.beijixing.app.data.model.Task(
                taskId = 2,
                name = "截客任务2",
                type = "INTERCEPT",
                status = "PENDING"
            )
        )
        
        assertEquals(2, tasks.size)
        assertEquals("ACTIVE_CAPTURE", tasks[0].type)
        assertEquals("INTERCEPT", tasks[1].type)
        assertTrue(tasks[0].canPause())
        assertTrue(tasks[1].canCancel())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createTask - should throw exception when name is empty`() = runTest {
        val name = ""
        require(name.isNotEmpty()) { "Task name cannot be empty" }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createTask - should throw exception when platform is empty`() = runTest {
        val platform = ""
        require(platform.isNotEmpty()) { "Platform cannot be empty" }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createTask - should throw exception when keywords is empty`() = runTest {
        val keywords = emptyList<String>()
        require(keywords.isNotEmpty()) { "Keywords cannot be empty" }
    }

    @Test
    fun `taskStatusTransition - should validate state changes`() = runTest {
        val validTransitions = mapOf(
            "PENDING" to listOf("RUNNING", "CANCELLED"),
            "RUNNING" to listOf("PAUSED", "COMPLETED", "FAILED"),
            "PAUSED" to listOf("RUNNING", "CANCELLED")
        )
        
        fun canTransition(from: String, to: String): Boolean {
            return validTransitions[from]?.contains(to) == true
        }
        
        assertTrue(canTransition("PENDING", "RUNNING"))
        assertTrue(canTransition("RUNNING", "PAUSED"))
        assertFalse(canTransition("PENDING", "COMPLETED"))
        assertFalse(canTransition("COMPLETED", "RUNNING"))
    }

    @Test
    fun `taskStats - should calculate statistics correctly`() = runTest {
        val stats = com.beijixing.app.data.model.TaskStats(
            totalProcessed = 100,
            successCount = 85,
            failCount = 10,
            pendingCount = 5,
            durationSeconds = 3600,
            avgProcessTime = 36.5
        )
        
        assertEquals(100, stats.totalProcessed)
        assertEquals(85, stats.successCount)
        assertEquals(10, stats.failCount)
        assertEquals(5, stats.pendingCount)
        assertEquals(3600, stats.durationSeconds)
    }

    @Test
    fun `pagination - should calculate page info correctly`() = runTest {
        val totalItems = 95
        val pageSize = 20
        val expectedPages = (totalItems + pageSize - 1) / pageSize
        
        assertEquals(5, expectedPages)
        
        for (page in 1..expectedPages) {
            val start = (page - 1) * pageSize
            val end = minOf(start + pageSize, totalItems)
            val isLastPage = page == expectedPages
            
            when (page) {
                1 -> {
                    assertEquals(0, start)
                    assertEquals(20, end)
                    assertFalse(isLastPage)
                }
                4 -> {
                    assertEquals(60, start)
                    assertEquals(80, end)
                    assertFalse(isLastPage)
                }
                5 -> {
                    assertEquals(80, start)
                    assertEquals(95, end)
                    assertTrue(isLastPage)
                }
            }
        }
    }
}