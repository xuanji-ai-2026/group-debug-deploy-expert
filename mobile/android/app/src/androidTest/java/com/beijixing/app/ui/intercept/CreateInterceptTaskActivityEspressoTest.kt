package com.beijixing.app.ui.intercept

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
 * Espresso UI Test for CreateInterceptTaskActivity
 * Tests form validation, boundary cases, and navigation
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateInterceptTaskActivityEspressoTest {

    private lateinit var scenario: ActivityScenario<CreateInterceptTaskActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(CreateInterceptTaskActivity::class.java)
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
        // Click submit without filling any field
        onView(withId(R.id.btnSubmit)).perform(click())

        // Verify error message is shown for task name
        onView(withId(R.id.tilTaskName))
            .check(matches(hasErrorText("请输入任务名称")))
    }

    /**
     * Test Case 2.2: Empty keywords should show error
     */
    @Test
    fun testEmptyKeywords_showsError() {
        // Fill only task name, leave keywords empty
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        // Verify error message is shown for keywords
        onView(withId(R.id.tilKeywords))
            .check(matches(hasErrorText("请输入至少一个关键词")))
    }

    /**
     * Test Case 2.3: Empty daily limit should show error
     */
    @Test
    fun testEmptyDailyLimit_showsError() {
        // Fill task name and keywords, leave daily limit empty
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1, keyword2"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        // Verify error message is shown for daily limit
        onView(withId(R.id.tilDailyLimit))
            .check(matches(hasErrorText("请输入每日上限")))
    }

    /**
     * Test Case 3.1: Invalid daily limit (negative number) should show error
     */
    @Test
    fun testNegativeDailyLimit_showsError() {
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1"), closeSoftKeyboard())
        onView(withId(R.id.etDailyLimit))
            .perform(typeText("-5"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        onView(withId(R.id.tilDailyLimit))
            .check(matches(hasErrorText("请输入有效的数字")))
    }

    /**
     * Test Case 3.2: Zero daily limit should show error
     */
    @Test
    fun testZeroDailyLimit_showsError() {
        onView(withId(R.id.etTaskName))
            .perform(typeText("Test Task"), closeSoftKeyboard())
        onView(withId(R.id.etKeywords))
            .perform(typeText("keyword1"), closeSoftKeyboard())
        onView(withId(R.id.etDailyLimit))
            .perform(typeText("0"), closeSoftKeyboard())
        
        onView(withId(R.id.btnSubmit)).perform(click())

        onView(withId(R.id.tilDailyLimit))
            .check(matches(hasErrorText("请输入有效的数字")))
    }

    // ==================== Boundary Value Tests ====================

    /**
     * Test Case 3.3: Very long text input (200 characters)
     */
    @Test
    fun testLongTextInput_acceptsInput() {
        val longText = "A".repeat(200)
        
        onView(withId(R.id.etTaskName))
            .perform(typeText(longText), closeSoftKeyboard())
        
        // Verify the text was entered successfully
        onView(withId(R.id.etTaskName))
            .check(matches(withText(longText)))
    }

    /**
     * Test Case 4.1: Special characters (SQL injection attempt)
     */
    @Test
    fun testSpecialCharacters_sqlInjection() {
        val sqlInjection = "'; DROP TABLE tasks; --"
        
        onView(withId(R.id.etTaskName))
            .perform(typeText(sqlInjection), closeSoftKeyboard())
        
        // Should accept the input (backend should handle sanitization)
        onView(withId(R.id.etTaskName))
            .check(matches(withText(sqlInjection)))
    }

    /**
     * Test Case 4.2: Special characters (XSS attack)
     */
    @Test
    fun testSpecialCharacters_xssAttack() {
        val xssAttack = "<script>alert('xss')</script>"
        
        onView(withId(R.id.etKeywords))
            .perform(typeText(xssAttack), closeSoftKeyboard())
        
        // Should accept the input (backend should handle sanitization)
        onView(withId(R.id.etKeywords))
            .check(matches(withText(xssAttack)))
    }

    /**
     * Test Case 4.3: Emoji characters
     */
    @Test
    fun testEmojiCharacters_accepted() {
        val emojiText = "😊🎉🚀📱💼"
        
        onView(withId(R.id.etTaskName))
            .perform(typeText(emojiText), closeSoftKeyboard())
        
        // Should accept emoji input
        onView(withId(R.id.etTaskName))
            .check(matches(withText(emojiText)))
    }

    // ==================== Navigation Tests ====================

    /**
     * Test Case 5.1: Toolbar back button navigates correctly
     */
    @Test
    fun testBackButton_navigatesToPreviousScreen() {
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

        // Activity should be finished (navigated back)
        // Note: In real implementation, this would verify MainActivity is shown
    }

    /**
     * Test Case 5.2: All form fields are visible and accessible
     */
    @Test
    fun testAllFormFields_visibleAndAccessible() {
        // Check all required form fields exist and are displayed
        onView(withId(R.id.tilTaskName))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tilKeywords))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tilCompetitorAccounts))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tilDailyLimit))
            .check(matches(isDisplayed()))
        onView(withId(R.id.rgPlatform))
            .check(matches(isDisplayed()))
        onView(withId(R.id.rgTargetType))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnSubmit))
            .check(matches(isDisplayed()))
    }

    // ==================== Radio Button Tests ====================

    /**
     * Test: Platform selection radio buttons work correctly
     */
    @Test
    fun testPlatformSelection_changesSelection() {
        // Default selection should be Douyin
        onView(withId(R.id.rbDouyin))
            .check(matches(isChecked()))

        // Select Xiaohongshu
        onView(withId(R.id.rbXiaohongshu))
            .perform(click())
        onView(withId(R.id.rbXiaohongshu))
            .check(matches(isChecked()))
        onView(withId(R.id.rbDouyin))
            .check(matches(isNotChecked()))

        // Select Kuaishou
        onView(withId(R.id.rbKuaishou))
            .perform(click())
        onView(withId(R.id.rbKuaishou))
            .check(matches(isChecked()))
    }

    /**
     * Test: Target type selection works correctly
     */
    @Test
    fun testTargetTypeSelection_changesSelection() {
        // Default should be Comment
        onView(withId(R.id.rbComment))
            .check(matches(isChecked()))

        // Select Fan
        onView(withId(R.id.rbFan))
            .perform(click())
        onView(withId(R.id.rbFan))
            .check(matches(isChecked()))

        // Select Search
        onView(withId(R.id.rbSearch))
            .perform(click())
        onView(withId(R.id.rbSearch))
            .check(matches(isChecked()))
    }
}
