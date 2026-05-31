#!/bin/bash
# 北极星AI系统 - 自动化多机型测试脚本
# 用于在多个Android虚拟设备上运行测试

echo "=========================================="
echo "🧪 北极星AI系统 - 多机型自动化测试"
echo "=========================================="

ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"

# 定义测试设备矩阵
declare -a DEVICES=(
    "pixel_6:pixel_6:30:1080x2400"
    "pixel_7:pixel_7:33:1080x2400"
    "pixel_8:pixel_8:34:1080x2400"
    "xiaomi_12:xiaomi_12:33:1440x3200"
    "samsung_s24:samsung_s24:34:1440x3168"
)

# 创建测试报告目录
REPORT_DIR="./test-reports/multi-device-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$REPORT_DIR"

echo ""
echo "📋 测试计划:"
echo "   - 设备数量: ${#DEVICES[@]}"
echo "   - 报告目录: $REPORT_DIR"
echo ""

PASS_COUNT=0
FAIL_COUNT=0
TOTAL_TESTS=0

for device_info in "${DEVICES[@]}"; do
    IFS=':' read -r device_name api_level resolution <<< "$device_info"
    
    echo "------------------------------------------"
    echo "🔧 启动设备: $device_name (API $api_level, $resolution)"
    echo "------------------------------------------"
    
    # 启动模拟器
    $EMulator -avd $device_name -no-snapshot-load &
    EMULATOR_PID=$!
    
    # 等待设备启动
    echo "⏳ 等待设备启动..."
    sleep 30
    
    # 检查设备是否就绪
    BOOT_COMPLETE=$($ADB shell getprop sys.boot_completed 2>/dev/null)
    TIMEOUT=0
    while [ "$BOOT_COMPLETED" != "1" ] && [ $TIMEOUT -lt 120 ]; do
        sleep 5
        BOOT_COMPLETE=$($ADB shell getprop sys.boot_completed 2>/dev/null)
        TIMEOUT=$((TIMEOUT + 5))
        echo "   ⏱️  已等待 ${TIMEOUT}s..."
    done
    
    if [ "$BOOT_COMPLETED" == "1" ]; then
        echo "✅ 设备启动成功"
        
        # 安装APK
        echo "📦 安装APK..."
        $ADB install -r app/build/outputs/apk/release/app-release.apk
        
        if [ $? -eq 0 ]; then
            echo "✅ APK安装成功"
            
            # 运行Instrumented Tests
            echo "🧪 运行集成测试..."
            ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.beijixing.app.*
            
            if [ $? -eq 0 ]; then
                echo "✅ 测试通过"
                PASS_COUNT=$((PASS_COUNT + 1))
                echo "PASS" > "$REPORT_DIR/${device_name}_result.txt"
            else
                echo "❌ 测试失败"
                FAIL_COUNT=$((FAIL_COUNT + 1))
                echo "FAIL" > "$REPORT_DIR/${device_name}_result.txt"
            fi
            
            # 收集日志
            $ADB logcat -d > "$REPORT_DIR/${device_name}_logcat.log"
            
            # 截图
            $ADB exec-out screencap -p > "$REPORT_DIR/${device_name}_screenshot.png"
        else
            echo "❌ APK安装失败"
            FAIL_COUNT=$((FAIL_COUNT + 1))
            echo "INSTALL_FAIL" > "$REPORT_DIR/${device_name}_result.txt"
        fi
    else
        echo "❌ 设备启动超时"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo "BOOT_TIMEOUT" > "$REPORT_DIR/${device_name}_result.txt"
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # 停止模拟器
    kill $EMULATOR_PID 2>/dev/null
    $ADB emu kill 2>/dev/null
    
    echo ""
done

# 生成汇总报告
echo "=========================================="
echo "📊 测试结果汇总"
echo "=========================================="
echo "总测试数: $TOTAL_TESTS"
echo "✅ 通过: $PASS_COUNT"
echo "❌ 失败: $FAIL_COUNT"
echo "通过率: $(( PASS_COUNT * 100 / TOTAL_TESTS ))%"
echo ""
echo "详细报告位置: $REPORT_DIR"
echo "=========================================="

# 输出最终结果
if [ $FAIL_COUNT -eq 0 ]; then
    echo "🎉 所有测试通过！可以发布。"
    exit 0
else
    echo "⚠️  存在失败的测试，请检查报告后修复问题。"
    exit 1
fi
