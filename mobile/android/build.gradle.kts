// Top-level build file for 北极星AI商机获客系统 Android App
// Android minimum SDK: API 26 (Android 8.0)
// Target SDK: API 34 (Android 14)
// Kotlin version: 1.9.x
// Compose version: 1.5.x

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}

// Definition:
// - API 26 (Android 8.0): Minimum supported version per product requirements
// - API 34 (Android 14): Target SDK for latest features and Google Play requirements
// - Kotlin 1.9.x: Stable Kotlin with excellent Compose support
// - Hilt 2.48.x: Dependency injection with Android lifecycle awareness
