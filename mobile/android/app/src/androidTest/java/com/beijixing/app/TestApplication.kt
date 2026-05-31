package com.beijixing.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Test Application class for Hilt instrumentation testing
 * Uses a separate Application class to avoid conflicts with production
 */
@HiltAndroidApp
class TestApplication : Application()
