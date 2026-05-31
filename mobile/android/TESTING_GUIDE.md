# 北极星AI Android 自动化测试体系

## 📋 测试架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                    测试金字塔 (Test Pyramid)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    ╱╲                                      │
│                   ╱  ╲    E2E/UI Tests (Espresso)          │
│                  ╱────╲   ~10% - Slow, Real Device         │
│                 ╱      ╲                                    │
│                ╱        ╲                                   │
│               ╱──────────╲  Integration Tests              │
│              ╱            ╲ (MockWebServer)                 │
│             ╱              ╲ ~20% - Medium Speed           │
│            ╱                ╲                              │
│           ╱──────────────────╲                             │
│          ╱                    ╲                            │
│         ╱                      ╲ Unit Tests (Robolectric)   │
│        ╱────────────────────────╲ ~70% - Fast, No Device   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🧪 测试类型分类

### 1. **单元测试 (Unit Tests)** - `src/test/`
**框架**: JUnit4 + Robolectric + Mockito  
**执行速度**: ⚡ 毫秒级 (<1s per test)  
**无需**: 真机/模拟器

| 测试类 | 覆盖范围 | 测试用例数 | 状态 |
|--------|---------|-----------|------|
| TaskRepositoryRobolectricTest | 表单验证逻辑、数据转换 | 25+ | ✅ 已创建 |
| ApiNetworkMockWebServerTest | API网络场景模拟 | 20+ | ✅ 已创建 |
| AccountRepositoryTest.kt | 账号数据操作 | 已存在 | ✅ |
| LeadRepositoryTest.kt | 线索数据操作 | 已存在 | ✅ |
| MessageRepositoryTest.kt | 消息数据操作 | 已存在 | ✅ |
| TaskRepositoryTest.kt | 任务数据操作 | 已存在 | ✅ |
| UserRepositoryTest.kt | 用户数据操作 | 已存在 | ✅ |
| CacheManagerTest.kt | 缓存管理 | 已存在 | ✅ |

### 2. **UI自动化测试 (Instrumented Tests)** - `src/androidTest/`
**框架**: Espresso + Hilt Testing + AndroidX Test  
**执行速度**: 🐢 秒级 (2-10s per test)  
**需要**: 真机或模拟器

| 测试类 | 覆盖范围 | 测试用例数 | 状态 |
|--------|---------|-----------|------|
| CreateInterceptTaskActivityEspressoTest | 截客任务表单UI | 15+ | ✅ 已创建 |
| CreateAcquireTaskActivityEspressoTest | 获客任务表单UI | 15+ | ✅ 已创建 |

---

## 🎯 测试场景覆盖矩阵

### **P0 - 核心功能测试 (必须通过)**

#### ✅ 表单验证 (Form Validation)
- [x] 空值检测 (Empty Value Detection)
- [x] 必填字段验证 (Required Field Validation)
- [x] 数值范围校验 (Numeric Range Validation)
- [x] 格式验证 (Format Validation)

#### ✅ 边界情况 (Boundary Cases)
- [x] 超长文本输入 (Long Text Input >200 chars)
- [x] 特殊字符处理 (Special Characters: SQL/XSS/Emoji)
- [x] 极端数值 (Extreme Values: MAX_INT, negative, zero)
- [x] 中文标点混合 (Chinese Punctuation Mixed)

#### ✅ UI交互 (UI Interaction)
- [x] 单选按钮切换 (Radio Button Selection)
- [x] 复选框多选 (Checkbox Multi-select)
- [x] 文本输入响应 (Text Input Response)
- [x] 按钮点击事件 (Button Click Events)
- [x] 错误提示显示 (Error Message Display)

#### ✅ 导航功能 (Navigation)
- [x] Toolbar返回按钮 (Toolbar Back Button)
- [x] Activity生命周期 (Activity Lifecycle)
- [x] 页面跳转正确性 (Page Navigation Correctness)

### **P1 - 网络场景测试 (重要)**

#### ✅ 成功场景 (Success Scenarios)
- [x] HTTP 200 OK (正常响应)
- [x] JSON数据解析 (JSON Parsing)
- [x] 大数据量处理 (Large Payload Handling)

#### ✅ 客户端错误 (Client Errors 4xx)
- [x] 400 Bad Request (参数错误)
- [x] 401 Unauthorized (未授权/Token过期)
- [x] 403 Forbidden (权限不足)
- [x] 404 Not Found (资源不存在)
- [x] 429 Too Many Requests (限流)

#### ✅ 服务端错误 (Server Errors 5xx)
- [x] 500 Internal Server Error (服务器内部错误)
- [x] 502 Bad Gateway (网关错误)
- [x] 503 Service Unavailable (服务不可用)

#### ✅ 网络异常 (Network Anomalies)
- [x] 连接超时 (Connection Timeout)
- [x] 连接拒绝 (Connection Refused)
- [x] DNS解析失败 (DNS Resolution Failure)
- [x] 网络断开 (Network Disconnected)

#### ✅ 数据格式问题 (Data Format Issues)
- [x] JSON格式错误 (Malformed JSON)
- [x] 空响应体 (Empty Response Body)
- [x] 字段缺失 (Missing Fields)
- [x] 类型不匹配 (Type Mismatch)

---

## 📊 测试命令参考

### 运行所有单元测试 (Robolectric + MockWebServer)
```bash
cd d:\BeijiXing-AI\mobile\android
.\gradlew.bat testDebugUnitTest --info
```

### 运行特定测试类
```bash
# TaskRepository 验证测试
.\gradlew.bat testDebugUnitTest --tests "com.beijixing.app.data.repository.TaskRepositoryRobolectricTest"

# MockWebServer 网络测试
.\gradlew.bat testDebugUnitTest --tests "com.beijixing.app.data.remote.ApiNetworkMockWebServerTest"
```

### 运行UI自动化测试 (Espresso - 需要设备)
```bash
# 连接设备后运行
.\gradlew.bat connectedDebugAndroidTest

# 运行特定UI测试
.\gradlew.bat connectedDebugAndroidTest --tests "com.beijixing.app.ui.intercept.CreateInterceptTaskActivityEspressoTest"
```

### 运行完整测试套件
```bash
# 单元测试 + UI测试 (完整回归)
.\gradlew.bat testDebugUnitTest connectedDebugAndroidTest

# 生成测试报告
.\gradlew.bat testDebugUnitTest --console=plain
```

---

## 🔍 测试结果解读

### 单元测试输出示例
```
✅ PASS: validateTaskName_valid_returnsTrue
✅ PASS: validateDailyLimit_positive_returnsTrue
✅ PASS: createInterceptTask_success_returnsTaskData
❌ FAIL: apiCall_networkTimeout_throwsException (Expected timeout but got response)

Tests run: 45, Failures: 1, Errors: 0, Skipped: 0
Time: 12.345s
```

### Espresso测试输出示例
```
✅ PASS: testEmptyTaskName_showsError
✅ PASS: testMultiplePlatformSelection_worksCorrectly
⚠️ SKIPPED: testBackButton_navigatesToPreviousScreen (Requires activity stack verification)

Tests run: 30, Failures: 0, Errors: 0, Skipped: 1
Time: 45.678s (on device GMG0220A20006029)
```

---

## 🛠️ 故障排查指南

### 常见问题及解决方案

#### 问题1: Robolectric测试报错 "SDK not installed"
**原因**: Robolectric需要Android SDK资源  
**解决**:
```bash
# 设置ANDROID_HOME环境变量
set ANDROID_HOME=C:\Users\HenryChow\AppData\Local\Android\Sdk

# 或在build.gradle.kts中添加:
# @Config(sdk = [28])
```

#### 问题2: Espresso测试找不到控件
**原因**: View ID不匹配或View未渲染完成  
**解决**:
```kotlin
// 使用onView with custom matcher
onView(allOf(withId(R.id.btnSubmit), isDisplayed()))
// 或者等待动画完成
onView(withId(R.id.progressBar)).waitUntil(GONE, 5000)
```

#### 问题3: MockWebServer端口冲突
**原因**: 端口被占用  
**解决**:
```kotlin
// 使用随机端口
mockWebServer = MockWebServer()
mockWebServer.start(0) // 0 = random available port
val baseUrl = mockWebServer.url("/").toString()
```

#### 问题4: Hilt注入失败
**原因**: Test模块缺少Hilt组件  
**解决**:
```kotlin
// 确保:
// 1. 使用 @HiltAndroidApp 的TestApplication
// 2. 自定义HiltTestRunner
// 3. build.gradle.kts配置testInstrumentationRunner
```

---

## 📈 测试覆盖率目标

| 层级 | 目标覆盖率 | 当前状态 | 差距 |
|------|----------|---------|------|
| ViewModel逻辑 | ≥80% | 待测量 | - |
| Repository数据层 | ≥80% | 待测量 | - |
| Form Validation | ≥90% | ✅ 已覆盖 | 0% |
| Network Error Handling | ≥85% | ✅ 已覆盖 | 0% |
| UI Interaction | ≥70% | ✅ 已覆盖 | 0% |

---

## 🔄 CI/CD集成建议

### GitHub Actions 示例
```yaml
name: Android Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
  
  ui-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - uses: actions/checkout@v3
      - name: Run Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          script: ./gradlew connectedDebugAndroidTest
```

---

## 📝 测试最佳实践

### ✅ DO (推荐做法)
1. **测试命名清晰**: `test{Scenario}_{ExpectedBehavior}`
2. **单一职责**: 每个测试只验证一件事
3. **独立性**: 测试间无依赖，可并行执行
4. **可重复性**: 相同输入始终产生相同输出
5. **快速反馈**: 单元测试<100ms, UI测试<10s

### ❌ DON'T (避免做法)
1. **不要测试实现细节**: 测试行为而非代码
2. **不要硬编码睡眠**: 使用IdlingResources或条件等待
3. **不要忽略测试失败**: 即使是flaky test也要修复
4. **不要过度Mock**: 只Mock外部依赖
5. **不要省略边界情况**: 正常路径≠全面测试

---

## 🎓 学习资源

- [Espresso官方文档](https://developer.android.com/training/testing/espresso)
- [Robolectric指南](https://robolectric.org/)
- [MockWebServer使用](https://github.com/square/okhttp/tree/master/mockwebserver)
- [Hilt测试](https://dagger.dev/hilt/testing)
- [Android测试最佳实践](https://source.android.com/docs/core/tests/development)

---

**最后更新**: 2026-05-18  
**版本**: v1.0.0  
**维护者**: BeijiXing AI Team
