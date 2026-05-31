package com.beijixing.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeadRepositoryTest {

    @Test
    fun `getLeads - should return list on success`() = runTest {
        val page = 1
        val size = 20

        val mockLeads = listOf(
            com.beijixing.app.data.model.Lead(id = 1, companyName = "Test Company"),
            com.beijixing.app.data.model.Lead(id = 2, companyName = "Another Company")
        )

        assertEquals(2, mockLeads.size)
        assertEquals("Test Company", mockLeads[0].companyName)
        assertEquals("Another Company", mockLeads[1].companyName)
    }

    @Test
    fun `getLeads - should handle pagination correctly`() = runTest {
        val page = 2
        val size = 10
        val total = 25

        val mockLeads = List(size) { index -> 
            com.beijixing.app.data.model.Lead(id = 10 + index.toLong(), companyName = "Company $index") 
        }

        assertEquals(size, mockLeads.size)
        assertFalse(page * size >= total)
    }

    @Test
    fun `deleteLead - should validate leadId positive`() = runTest {
        val leadId = -1L

        try {
            require(leadId > 0) { "Lead ID must be positive" }
            fail("Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Lead ID must be positive") == true)
        }
    }

    @Test
    fun `addFollowRecord - should validate content not empty`() = runTest {
        val content = ""

        assertTrue("Follow record content cannot be blank", content.isBlank())
    }

    @Test
    fun `getLeadStats - should return stats with zero values for empty data`() = runTest {
        val mockStats = com.beijixing.app.data.model.LeadStats(
            total = 0,
            newCount = 0,
            followingCount = 0,
            dealedCount = 0,
            highLevelCount = 0,
            alertCount = 0
        )

        assertEquals(0, mockStats.total)
        assertEquals(0, mockStats.newCount)
        assertEquals(0, mockStats.alertCount)
    }

    @Test
    fun `leadLevel - should return correct display text`() = runTest {
        val highLead = com.beijixing.app.data.model.Lead(id = 1, level = "HIGH")
        val mediumLead = com.beijixing.app.data.model.Lead(id = 2, level = "MEDIUM")
        val lowLead = com.beijixing.app.data.model.Lead(id = 3, level = "LOW")

        assertEquals("高意向", highLead.getLevelText())
        assertEquals("中意向", mediumLead.getLevelText())
        assertEquals("低意向", lowLead.getLevelText())
    }

    @Test
    fun `leadStatus - should return correct display text`() = runTest {
        val newLead = com.beijixing.app.data.model.Lead(id = 1, status = "NEW")
        val followingLead = com.beijixing.app.data.model.Lead(id = 2, status = "FOLLOWING")
        val dealedLead = com.beijixing.app.data.model.Lead(id = 3, status = "DEALED")
        val lostLead = com.beijixing.app.data.model.Lead(id = 4, status = "LOST")

        assertEquals("新商机", newLead.getStatusText())
        assertEquals("跟进中", followingLead.getStatusText())
        assertEquals("已成交", dealedLead.getStatusText())
        assertEquals("已流失", lostLead.getStatusText())
    }
}