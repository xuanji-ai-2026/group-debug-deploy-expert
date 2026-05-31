@echo off
chcp 65001 >nul 2>&1
cd /d d:\BeijiXing-AI\mobile\android
echo ============================================
echo   BeijiXing AI Android Unit Tests
echo ============================================
echo.

echo [1/3] Stopping Gradle Daemon...
call gradlew.bat --stop >nul 2>&1

echo [2/3] Running unit tests...
call gradlew.bat testDebugUnitTest --console=plain --no-daemon

echo.
if %ERRORLEVEL% EQU 0 (
    echo [3/3] Result: SUCCESS - All tests passed!
    echo Report: app\build\reports\tests\debugUnitTest\index.html
) else (
    echo [3/3] Result: FAILED - Check error messages above
)

echo.
pause
