@echo off
setlocal
if "%ANDROID_SDK_ROOT%"=="" set "ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"
rem tools/android looks for an extension-less "platform-tools/adb"; on Windows
rem only adb.exe exists, so expose platform-tools through PATH instead.
set "PATH=%ANDROID_SDK_ROOT%\platform-tools;%PATH%"
py "%~dp0android" %*
