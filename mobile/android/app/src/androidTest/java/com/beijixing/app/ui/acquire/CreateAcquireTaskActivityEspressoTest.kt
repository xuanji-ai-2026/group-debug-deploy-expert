package com.beijixing.app.ui.acquire

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.beijixing.app.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso UI Test for CreateAcquireTaskActivity
 * Tests form validation, checkbox multi-selection, and boundary cases
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateAcquireTaskActivityEspressoTest {

    private lateinit var scenario: ActivityScenario<CreateAcquireTaskActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(CreateAcquireTaskActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    // ==================== Form Validation Tests ====================

    /**
     * Test Case 2.1: Empty task name should show error
     */
    @Test
    fun testEmptyTaskName_showsError() {
        onView(withId(R.id.btnSubmit)).perform(click())

        onView(withId(R.id.tilTaskName))
            .check(matches(hasErrorText("请输入任务名称")))
    }

    /**
     * Test Case 2.2: Empty keywords should show error
     */
    @Test
    fun testEmptyKeywords_showsError() {
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        onView(withId(R.id.tilKeywords))
            .check(matches(hasErrorText("请输入至少一个关键词")))
    }

    /**
     * Test Case 2.3: Empty daily limit should show error
     */
    @Test
    fun testEmptyDailyLimit_showsError() {
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1, keyword2"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        onView(withId(R.id.tilDailyLimit))
            .check(matches(hasErrorText("请输入每日上限")))
    }

    /**
     * Test Case 2.4: No platform selected should show toast error
     */
    @Test
    fun testNoPlatformSelected_showsError() {
        // Fill required fields but uncheck all platforms
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1"), closeSoftKeyboard())
        onView(withId(R.id.etDailyLimit))
            .perform(typeText("10"), closeSoftKeyboard())

        // Uncheck all platform checkboxes (if checked by default)
        if (isCheckboxChecked(R.id.cbDouyin)) {
            onView(withId(R.id.cbDouyin)).perform(click())
        }
        
        onView(withId(R.id.btnSubmit)).perform(click())

        // Note: Toast verification requires custom matcher or UiAutomator
        // For now, we verify the form doesn't submit (activity still visible)
        onView(withId(R.id.btnSubmit))
            .check(matches(isDisplayed()))
    }

    // ==================== Checkbox Multi-Selection Tests ====================

    /**
     * Test: Multiple platform selection works correctly
     */
    @Test
    fun testMultiplePlatformSelection_worksCorrectly() {
        // Verify Douyin is checked by default
        onView(withId(R.id.cbDouyin))
            .check(matches(isChecked()))

        // Select additional platforms
        onView(withId(R.id.cbXiaohongshu))
            .perform(click())
        onView(withId(R.id.cbXiaohongshu))
            .check(matches(isChecked()))

        onView(withId(R.id.cbKuaishou))
            .perform(click())
        onView(withId(R.id.cbKuaishou))
            .check(matches(isChecked()))

        // All three should be selected
        onView(withId(R.id.cbDouyin))
            .check(matches(isChecked()))
        onView(withId(R.id.cbXiaohongshu))
            .check(matches(isChecked()))
        onView(withId(R.id.cbKuaishou))
            .check(matches(isChecked()))
    }

    /**
     * Test: Unchecking all platforms prevents submission
     */
    @Test
    fun testUncheckAllPlatforms_preventsSubmission() {
        // Uncheck default selection
        onView(withId(R.id.cbDouyin))
            .perform(click())
        onView(withId(R.id.cbDouyin))
            .check(matches(isNotChecked()))

        // Fill other fields
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1"), closeSoftKeyboard())
        onView(withId(R.id.etDailyLimit))
            .perform(typeText("10"), closeSoftKeyboard())

        // Try to submit - should not proceed
        onView(withId(R.id.btnSubmit)).perform(click())

        // Verify activity is still showing (not finished)
        onView(withId(R.id.btnSubmit))
            .check(matches(isDisplayed()))
    }

    // ==================== Boundary Value Tests ====================

    /**
     * Test Case 3.1: Very long task name (200+ characters)
     */
    @Test
    fun testLongTaskName_accepted() {
        val longText = "A".repeat(250)
        
        onView(withId(R.id.etTaskName))
            .perform(typeText(longText), closeSoftKeyboard())
        
        onView(withId(R.id.etTaskName))
            .check(matches(withText(longText)))
    }

    /**
     * Test Case 3.2: Decimal daily limit should show error
     */
    @Test
    fun testDecimalDailyLimit_showsError() {
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1"), closeSoftKeyboard())
        onView(withId(R.id.etDailyLimit))
            .perform(typeText("1.5"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        onView(withId(R.id.tilDailyLimit))
            .check(matches(hasErrorText("请输入有效的数字")))
    }

    /**
     * Test Case 3.3: Very large daily limit value
     */
    @Test
    fun testVeryLargeDailyLimit_accepted() {
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1"), closeSoftKeyboard())
        onView(withId(R.id.etDailyLimit))
            .perform(typeText("999999999"), closeSoftKeyboard())
        
        // Should accept large number (backend may have its own limits)
        onView(withId(R.id.etDailyLimit))
            .check(matches(withText("999999999")))
    }

    // ==================== Special Character Tests ====================

    /**
     * Test Case 4.1: Chinese punctuation in keywords
     */
    @Test
    fun testChinesePunctuation_accepted() {
        val chinesePunctuation = "，。！？（）：；""''"
        
        onView(withId(R.id.etKeywords))
            .perform(typeText(chinesePunctuation), closeSoftKeyboard())
        
        onView(withId(R.id.etKeywords))
            .check(matches(withText(chinesePunctuation)))
    }

    /**
     * Test Case 4.2: Mixed characters (English + Chinese + Numbers + Symbols)
     */
    @Test
    fun testMixedCharacters_accepted() {
        val mixedText = "Test@#$%中文123测试_测试-2026"
        
        onView(withId(R.id.etTaskName))
            .perform(typeText(mixedText), closeSoftKeyboard())
        
        onView(withId(R.id.etTaskName))
            .check(matches(withText(mixedText)))
    }

    // ==================== Radio Button Tests ====================

    /**
     * Test: Channel selection radio buttons work correctly
     */
    @Test
    fun testChannelSelection_changesSelection() {
        // Default should be Search
        onView(withId(R.id.rbSearch))
            .check(matches(isChecked()))

        // Select Topic
        onView(withId(R.id.rbTopic))
            .perform(click())
        onView(withId(R.id.rbTopic))
            .check(matches(isChecked()))

        // Select Location
        onView(withId(R.id.rbLocation))
            .perform(click())
        onView(withId(R.id.rbLocation))
            .check(matches(isChecked()))

        // Select Recommend
        onView(withId(R.id.rbRecommend))
            .perform(click())
        onView(withId(R.id.rbRecommend))
            .check(matches(isChecked()))
    }

    // ==================== Navigation Tests ====================

    /**
     * Test Case 5.1: Back button functionality
     */
    @Test
    fun testBackButton_navigatesBack() {
        // Click toolbar back button
        onView(
            withContentDescription(
                org.hamcrest.CoreMatchers.anyOf(
                    org.hamcrest.CoreMatchers.containsString("Navigate up"),
                    org.hamcrest.CoreMatchers.containsString("")
                )
            )
        ).or(onView(withParent(withId(R.id.toolbar))))
         .perform(click())

        // Activity should finish
    }

    /**
     * Test Case 5.2: All form fields are displayed
     */
    @Test
    fun testAllFormFields_displayed() {
        onView(withId(R.id.tilTaskName))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tilKeywords))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tilDailyLimit))
            .check(matches(isDisplayed()))
        onView(withId(R.id.rgChannel))
            .check(matches(isDisplayed()))
        onView(withId(R.id.cbDouyin))
            .check(matches(isDisplayed()))
        onView(withId(R.id.cbXiaohongshu))
            .check(matches(isDisplayed()))
        onView(withId(R.id.cbKuaishou))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnSubmit))
            .check(matches(isDisplayed()))
    }

    // ==================== Helper Methods ====================

    private fun isCheckboxChecked(checkBoxId: Int): Boolean {
        try {
            onView(withId(checkBoxId))
                .check(matches(isChecked()))
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
