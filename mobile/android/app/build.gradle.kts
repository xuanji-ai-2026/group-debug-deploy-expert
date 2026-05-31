plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.beijixing.app"
    compileSdk = 34

    // 签名配置 (使用 debug keystore 用于 Release 构建)
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("USERPROFILE") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.beijixing.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 25
        versionName = "1.0.25"

        testInstrumentationRunner = "com.beijixing.app.HiltTestRunner"

        // 北极星API基础地址
        buildConfigField("String", "BASE_URL", "\"https://www.beijixing-ai.com/api/\"")

        // 矢量Drawable支持
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // 自定义APK输出名称
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "BeijiXingAI-${versionName}-${buildType.name}.apk"
        }
    }

    compileOptions {
        // Java 17 兼容性，确保与现代 JDK 兼容
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        getByName("test") {
            java.srcDirs("src/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }
}

dependencies {
    // ==================== AndroidX Core ====================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ==================== Material Design 3 (XML + Compose) ====================
    // Material library provides Material3 widgets for XML layouts
    implementation("com.google.android.material:material:1.11.0")

    // ==================== Jetpack Compose ====================
    // Compose BOM manages all Compose library versions for consistency
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Navigation for screen routing
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ==================== Lifecycle & ViewModel ====================
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // ==================== Hilt Dependency Injection ====================
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")

    // ==================== Retrofit + OkHttp ====================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ==================== Coroutines ====================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ==================== DataStore (Preferences) ====================
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ==================== Splash Screen ====================
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ==================== Accompanist (Permissions) ====================
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // ==================== Coil (Image Loading for Compose) ====================
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ==================== Glide (Image Loading for XML Views) ====================
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ==================== CircleImageView ====================
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // ==================== Testing - Unit Tests (Robolectric + MockWebServer) ====================
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48.1")

    // ==================== Testing - Instrumented Tests (Espresso) ====================
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48.1")

    // Compose Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Hilt: Allow references to custom Dagger components
kapt {
    correctErrorTypes = true
}
