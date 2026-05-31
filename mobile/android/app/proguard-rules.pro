# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFileTable,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==================== 北极星AI - ProGuard Rules ====================
# 保持所有类不被混淆或移除（确保功能完整）
-keep class com.beijixing.app.** { *; }
-keep interface com.beijixing.app.** { *; }
-keep enum com.beijixing.app.** { *; }

# 特别保持Activity
-keep class com.beijixing.app.ui.** extends android.app.Activity { *; }

# 保持ViewModel
-keep class com.beijixing.app.*ViewModel { *; }
-keep class com.beijixing.app.*.*ViewModel { *; }

# 保持Repository
-keep class com.beijixing.app.data.repository.** { *; }

# 保持模型类的序列化
-keep class com.beijixing.app.data.model.** { *; }

# 保持API接口（Retrofit需要）
-keep interface com.beijixing.app.data.remote.** { *; }
-keep @interface retrofit2.http.* { *; }

# 保持Hilt注入相关类
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *** getContext(); }
-keep class dagger.hilt.android.internal.managers.* { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltComponents_Singleton { *; }

# 保持Gson序列化
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保持Kotlin元数据（协程、data class等）
-keepnames class kotlinx.serialization.json.** { *; }
-dontnote kotlinx.serialization.json.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin协程保持
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 保持Compose相关
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 保持OkHttp和Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Exceptions,InnerClasses,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations

# 保持DataStore
-keep class androidx.datastore.** { *; }

# 保持Coil图片加载库
-dontwarn coil.**
-keep class coil.** { *; }

# 保持Glide图片加载库
-dontwarn com.bumptech.glide.**
-keep public class com.bumptech.glide.** {*;}
-keep class com.bumptech.glide.load.engine.bitmap_recycle.** {*;}
-keep class com.bumptech.glide.module.** {*;}

# 保持Material Design组件
-dontwarn com.google.android.material.**

# 保持AndroidX组件
-keep class androidx.recyclerview.** { *; }
-keep class androidx.swiperefreshlayout.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.lifecycle.** { *; }

# 保持Activity Result API
-keep class androidx.activity.result.** { *; }

# 保留日志调用（调试需要，生产环境可通过其他方式优化）
# -assumenosideeffects class android.util.Log {
#     public static boolean isLoggable(java.lang.String, int);
#     public static int v(...);
#     public static int d(...);
#     public static int i(...);
#     public static int w(...);
#     public static int e(...);
# }
