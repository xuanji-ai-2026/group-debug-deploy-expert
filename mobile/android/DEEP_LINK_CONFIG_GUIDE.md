# Android Deep Link配置指南 (OAuth授权专用)
# 文件位置: mobile/android/app/src/main/AndroidManifest.xml
# 更新时间: 2026-05-20
# 作者: 北极星AI团队

## 📋 配置说明

### 1. 在AndroidManifest.xml中注册OAuthActivity

```xml
<!-- OAuthActivity - 处理社交平台Deep Link回调 -->
<activity
    android:name=".ui.oauth.OAuthActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:theme="@style/Theme.AppCompat.Translucent.NoTitleBar">
    
    <!-- HTTPS Universal Links（推荐方式） -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- 抖音回调: https://yourapp.com/oauth/callback/douyin?code=xxx&state=yyy -->
        <data
            android:scheme="https"
            android:host="${applicationId}.yourdomain.com"  <!-- 替换为您的实际域名 -->
            android:pathPrefix="/oauth/callback/douyin" />
        
        <!-- 小红书回调: https://yourapp.com/oauth/callback/xiaohongshu?code=xxx&state=yyy -->
        <data
            android:scheme="https"
            android:host="${applicationId}.yourdomain.com"
            android:pathPrefix="/oauth/callback/xiaohongshu" />
        
        <!-- 快手回调: https://yourapp.com/oauth/callback/kuaishou?code=xxx&state=yyy -->
        <data
            android:scheme="https"
            android:host="${applicationId}.yourdomain.com"
            android:pathPrefix="/oauth/callback/kuaishou" />
            
        <!-- 微信回调: https://yourapp.com/oauth/callback/wechat?code=xxx&state=yyy -->
        <data
            android:scheme="https"
            android:host="${applicationId}.yourdomain.com"
            android:pathPrefix="/oauth/callback/wechat" />
    </intent-filter>
    
    <!-- 自定义Scheme（备用方案，用于测试） -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- 北极星自定义scheme -->
        <data android:scheme="beijixing" android:pathPrefix="/oauth/callback" />
    </intent-filter>
</activity>
```

### 2. 替换域名占位符

**重要**: 请将 `${applicationId}.yourdomain.com` 替换为您实际拥有的HTTPS域名！

示例:
- 开发环境: `dev.beijixing.com`
- 测试环境: `test.beijixing.com`
- 生产环境: `app.beijixing.com` 或 `www.beijixing.com`

### 3. 域名验证配置

Android App Links要求在服务器上部署`assetlinks.json`文件:

**文件路径**: `https://yourdomain.com/.well-known/assetlinks.json`

**文件内容**:
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.beijixing.app",
    "sha256_cert_fingerprints":
    [
      "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
      "YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY"
    ]
  }
}]
```

**获取SHA256指纹的方法**:
```bash
# Debug版本
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
  -storepass android -keypass android 2>/dev/null | grep SHA256

# Release版本（使用您的签名密钥）
keytool -list -v -keystore your-release-key.jks -alias your-alias \
  -storepass your-password 2>/dev/null | grep SHA256
```

### 4. 测试Deep Link

#### 方法1: 使用adb命令行测试
```bash
# 测试抖音回调
adb shell am start -a android.intent.action-VIEW \
  -d "https://app.beijixing.com/oauth/callback/douyin?code=test_code_12345&state=test_state_67890"

# 测试小红书回调
adb shell am start -a android.intent.action-VIEW \
  -d "https://app.beijixing.com/oauth/callback/xiaohongshu?code=test_code_abcde&state=test_state_fghij"

# 使用自定义scheme测试
adb shell am start -a android.intent.action-VIEW \
  -d "beijixing:/oauth/callback/douyin?code=test_code&state=test_state"
```

#### 方法2: 在浏览器地址栏输入
```
https://app.beijixing.com/oauth/callback/douyin?code=test_code_12345&state=test_state_67890
```

### 5. 处理已安装APP的冲突

如果用户手机上同时安装了北极星APP和社交APP（如抖音），需要处理以下场景:

**方案A**: 先跳转到社交APP授权，再通过Deep Link返回北极星APP
```
用户点击"绑定抖音" → 打开抖音授权页 → 用户确认 → 
抖音通过snssdk1128:// 回调 → Android系统弹出选择器 → 
用户选择"北极星APP" → OAuthActivity接收回调
```

**方案B**: 使用Chrome Custom Tabs（推荐）
```kotlin
// 在OAuthHelper.kt中使用Custom Tabs打开授权URL
val customTabsIntent = CustomTabsIntent.Builder().build()
customTabsIntent.launchUrl(this, Uri.parse(authUrl))
```

### 6. 安全注意事项

⚠️ **必须遵守的安全规范**:

1. **State参数验证**: 每次OAuth流程必须生成唯一的state参数，并在回调时严格验证
   ```kotlin
   // ✅ 正确：生成随机state
   val state = UUID.randomUUID().toString()
   
   // ❌ 错误：使用固定或可预测的state
   val state = "fixed_state_123"
   ```

2. **PKCE Code Verifier保护**: code_verifier必须安全存储，不能明文保存到SharedPreferences
   ```kotlin
   // ✅ 正确：使用EncryptedSharedPreferences
   val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
   val prefs = EncryptedSharedPreferences.create(
       "oauth_secure_prefs",
       masterKey,
       this,
       EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
       EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
   )
   prefs.edit().putString("code_verifier", codeVerifier).apply()
   
   // ❌ 错误：明文存储到普通SharedPreferences
   getSharedPreferences("prefs", MODE_PRIVATE)
       .edit()
       .putString("code_verifier", codeVerifier)  // 危险！
       .apply()
   ```

3. **清理敏感数据**: 授权完成后立即删除临时存储的敏感数据
   ```kotlin
   override fun onDestroy() {
       super.onDestroy()
       clearOAuthTempData(platform)
   }
   ```

4. **HTTPS强制**: 所有回调URL必须使用HTTPS，禁止HTTP
   ```xml
   <!-- ✅ 正确：使用HTTPS scheme -->
   <data android:scheme="https" ... />
   
   <!-- ❌ 错误：使用HTTP scheme -->
   <data android:scheme="http" ... />  <!-- Android 9+会拦截 -->
   ```

5. **防重放攻击**: 授权码(code)只能使用一次，后端必须校验
   ```java
   // MobileOAuthController.java 中实现
   if (redisTemplate.hasKey("oauth:used_code:" + code)) {
       throw new RuntimeException("授权码已被使用");
   }
   redisTemplate.opsForValue().set("oauth:used_code:" + code, "1", Duration.ofMinutes(10));
   ```

### 7. 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 点击授权链接后没有跳转到APP | Intent Filter未正确配置 | 检查scheme/host/path是否匹配 |
| 弹出"请选择应用"对话框 | 多个APP匹配了相同的URI | 确保域名唯一性 |
| OAuthActivity未收到回调 | exported=false | 设置android:exported="true" |
| State验证失败 | SharedPreferences数据丢失 | 使用EncryptedSharedPreferences |
| PKCE验证失败 | code_verifier存储时被截断 | 确保长度≥43字符 |

---

**文档版本**: v2.0  
**最后更新**: 2026-05-20  
**适用范围**: Android API 23+ (Android 6.0 Marshmallow及以上)
