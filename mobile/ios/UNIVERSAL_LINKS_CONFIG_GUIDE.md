# iOS Universal Links配置指南 (OAuth授权专用)
# 文件位置: mobile/ios/BxApp/
# 更新时间: 2026-05-20
# 作者: 北极星AI团队

## 📋 配置说明

### 1. 在Xcode中配置Associated Domains

1. 打开Xcode项目 → 选择Target → Signing & Capabilities
2. 点击 "+ Capability" → 搜索 "Associated Domains"
3. 添加以下域名（**注意**: 必须以 `applinks:` 开头）:

```
applinks:app.beijixing.com
```

**重要**: 
- 不要包含 `https://` 或路径部分
- 域名必须是您实际拥有的HTTPS域名
- 开发环境和生产环境可以使用不同的域名

### 2. 部署apple-app-site-association (AASA) 文件

AASA文件必须部署到您的HTTPS服务器的根目录或 `.well-known` 子目录:

**方式A - 根目录（推荐）**:
```
https://app.beijixing.com/apple-app-site-association
```

**方式B - .well-known目录**:
```
https://app.beijixing.com/.well-known/apple-app-site-association
```

**AASA文件内容示例**:
```json
{
  "applinks": {
    "details": [
      {
        "appIDs": [ "TEAMID.com.beijixing.BxApp" ],
        "components": [
          { "/": "/oauth/callback/douyin", "comment": "抖音OAuth回调" },
          { "/": "/oauth/callback/xiaohongshu", "comment": "小红书OAuth回调" },
          { "/": "/oauth/callback/kuaishou", "comment": "快手OAuth回调" },
          { "/": "/oauth/callback/wechat", "comment": "微信OAuth回调" }
        ],
        "paths": [ "/oauth/*" ]
      }
    ]
  }
}
```

**获取Team ID的方法**:
```bash
# 方式1: 从Apple Developer Portal查看
# 登录 https://developer.apple.com/account/#/membership

# 方式2: 从Xcode自动获取
# Xcode → Preferences → Account → 选择您的团队 → Team ID

# 方式3: 命令行工具
security find-identity -v -p codesigning
```

### 3. AASA文件要求

⚠️ **必须严格遵守以下规范**:

| 要求 | 说明 |
|------|------|
| **Content-Type** | `application/json` （禁止其他类型） |
| **文件大小** | ≤ 128KB |
| **编码格式** | UTF-8 无BOM |
| **访问权限** | 公开可访问（无需认证） |
| **HTTPS证书** | 必须是有效且受信任的SSL证书 |
| **不支持重定向** | AASA URL不能返回301/302重定向 |

**验证AASA文件是否可访问**:
```bash
curl -I https://app.beijixing.com/apple-app-site-association

# 期望响应:
# HTTP/2 200
# content-type: application/json
```

### 4. SceneDelegate配置（iOS 13+）

在您的 `SceneDelegate.swift` 中添加Universal Links回调处理:

```swift
// SceneDelegate+OAuth.swift (已创建)

func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
          let url = userActivity.webpageURL else {
        return
    }
    
    handleOAuthCallback(url: url)
}
```

### 5. AppDelegate配置（iOS 12及更早版本）

如果您需要支持iOS 12及更早版本:

```swift
// AppDelegate.swift

func application(_ application: UIApplication,
                 continue userActivity: NSUserActivity,
                 restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
    
    guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
          let url = userActivity.webpageURL else {
        return false
    }
    
    handleOAuthCallback(url: url)
    return true
}
```

### 6. Info.plist配置（可选）

如果使用自定义scheme作为备用方案:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>beijixing</string>
        </array>
        <key>CFBundleURLName</key>
        <string>com.beijixing.app.oauth</string>
    </dict>
</array>
```

### 7. 测试Universal Links

#### 方法1: 使用模拟器测试

```bash
# 构建并运行到模拟器
xcodebuild -workspace BxApp.xcworkspace \
  -scheme BxApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  build
```

#### 方法2: 使用真机测试

1. 将AASA文件部署到服务器
2. 在iPhone Safari浏览器中输入:
   ```
   https://app.beijixing.com/oauth/callback/douyin?code=test_code_12345&state=test_state_67890
   ```
3. 如果配置正确，Safari会直接跳转到北极星APP

#### 方法3: 使用Xcode控制台验证

```swift
// 在SceneDelegate中添加日志
print("🔗 收到Universal Link: \(url.absoluteString)")
```

#### 方法4: 使用Apple验证工具

访问: https://search.developer.apple.com/appsearch-validation-tool/

输入您的域名和Bundle ID，检查AASA文件是否被正确识别。

### 8. 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| Universal Links不生效 | AASA文件无法访问 | 检查HTTPS证书、Content-Type、文件大小 |
| 弹出Safari而非打开APP | Associated Domains未正确配置 | 确认域名前缀为`applinks:`（无http/路径） |
| 回调参数丢失 | URL编码问题 | 对code和state进行URL编码 |
| State验证失败 | Keychain数据未保存 | 检查Keychain Access Groups配置 |
| PKCE验证失败 | Code Verifier存储失败 | 使用SecItemAdd而非UserDefaults |

### 9. 安全最佳实践

✅ **必须遵守的安全规范**:

1. **使用Keychain存储敏感数据**:
   ```swift
   // ✅ 正确：使用Keychain
   SecItemAdd(query as CFDictionary, nil)
   
   // ❌ 错误：使用UserDefaults（不安全）
   UserDefaults.standard.set(codeVerifier, forKey: "verifier")
   ```

2. **启用App Transport Security (ATS)**:
   ```xml
   <!-- Info.plist -->
   <key>NSAppTransportSecurity</key>
   <dict>
       <key>NSAllowsArbitraryLoads</key>
       <false/>
   </dict>
   ```

3. **清理临时数据**:
   ```swift
   defer {
       removeFromKeychain(key: "code_verifier")
       removeFromKeychain(key: "state")
   }
   ```

4. **防止重放攻击**:
   ```swift
   // 后端必须校验code只能使用一次
   if redis.exists("oauth:used_code:\(code)") {
       throw OAuthError.securityViolation("Code已被使用")
   }
   ```

---

## 📚 参考资料

- [Apple官方文档: Supporting Universal Links](https://developer.apple.com/library/archive/documentation/General/Conceptual/AppSearch/UniversalLinks.html)
- [Apple开发者论坛: Associated Domains FAQ](https://developer.apple.com/forums/thread/663189)
- [WWDC 2022: What's new in App Store Connect](https://developer.apple.com/videos/play/wwdc2022/10143/)
- [RFC 3986: Uniform Resource Identifier (URI): Generic Syntax](https://tools.ietf.org/html/rfc3986)

---

**文档版本**: v2.0  
**最后更新**: 2026-05-20  
**适用范围**: iOS 13+ / iPadOS 13+
