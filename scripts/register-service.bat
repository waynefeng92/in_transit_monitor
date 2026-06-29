@echo off
REM ===========================================================================
REM register-service.bat -- NSSM Service Registration
REM Run as Administrator on Windows Server
REM Prerequisites: NSSM installed, JDK 17 installed
REM ===========================================================================

setlocal enabledelayedexpansion

set RORO_HOME=D:\in_transit_monitor
set SERVICE_NAME=roro-backend

echo =============================================
echo   Ro-Ro Vehicle Monitor - Service Registration
echo =============================================
echo.

REM -- Check NSSM --
echo [STEP 1/4] Checking NSSM...
where nssm >nul 2>&1
if errorlevel 1 (
    echo [WARN] NSSM not found in PATH
    echo Download from: https://nssm.cc/download
    echo Extract to: %RORO_HOME%\nssm\
    REM Try default location
    if exist "%RORO_HOME%\nssm\win64\nssm.exe" (
        set "PATH=%PATH%;%RORO_HOME%\nssm\win64"
        echo Found NSSM at: %RORO_HOME%\nssm\win64\nssm.exe
    ) else (
        echo [ERROR] NSSM not available - please install first
        pause
        exit /b 1
    )
)
echo [OK] NSSM ready

REM -- Check JDK --
echo.
echo [STEP 2/4] Checking JDK 17...
if "%JDK_PATH%"=="" set JDK_PATH=C:\Program Files\Java\jdk-17

if not exist "%JDK_PATH%\bin\java.exe" (
    echo [ERROR] JDK 17 not found at: %JDK_PATH%
    echo Edit this script and set JDK_PATH variable
    echo Eclipse Temurin JDK 17 supports Windows Server 2012 R2
    pause
    exit /b 1
)
echo [OK] JDK path: %JDK_PATH%

REM -- Check existing service --
echo.
echo [STEP 4/5] Checking service status...
nssm status %SERVICE_NAME% >nul 2>&1
if errorlevel 1 (
    echo Service not registered - installing...
    echo JDK: %JDK_PATH%\bin\java.exe
    echo JAR: %RORO_HOME%\backend\ro-ro-monitor.jar
    
    nssm install %SERVICE_NAME% "%JDK_PATH%\bin\java.exe"
    if errorlevel 1 (
        echo [ERROR] Service registration failed! Run as Administrator.
        pause
        exit /b 1
    )
    echo [OK] Service registered
) else (
    echo Service already registered - updating config...
)

REM -- Configure service --
echo.
echo [STEP 5/5] Configuring service...

nssm set %SERVICE_NAME% AppParameters "-jar %RORO_HOME%\backend\ro-ro-monitor.jar --spring.profiles.active=prod"
if errorlevel 1 echo [WARN] AppParameters failed

nssm set %SERVICE_NAME% AppDirectory %RORO_HOME%\backend
nssm set %SERVICE_NAME% DisplayName "RoRo Monitor Backend"
nssm set %SERVICE_NAME% Description "Ro-Ro Vehicle Monitor Backend Service"
nssm set %SERVICE_NAME% Start SERVICE_AUTO_START

REM -- Environment variables --
nssm set %SERVICE_NAME% AppEnvironmentExtra ^
    DB_HOST=%DB_HOST% ^
    DB_PORT=%DB_PORT% ^
    DB_NAME=%DB_NAME% ^
    DB_USERNAME=%DB_USERNAME% ^
    DB_PASSWORD=%DB_PASSWORD% ^
    ADMIN_DEFAULT_PASSWORD=%ADMIN_DEFAULT_PASSWORD% ^
    CORS_ALLOWED_ORIGINS=%CORS_ALLOWED_ORIGINS% ^
    SERVER_PORT=%SERVER_PORT% ^
    TZ=Asia/Shanghai

REM -- Auto-restart on crash --
nssm set %SERVICE_NAME% AppThrottle 5000
nssm set %SERVICE_NAME% AppRestartDelay 10000

echo [OK] Service configured

REM -- Start service --
echo.
echo Starting service...
nssm start %SERVICE_NAME%
if errorlevel 1 (
    echo [WARN] Start failed - check config and try manually:
    echo   nssm start %SERVICE_NAME%
) else (
    echo [OK] Service started
)

REM -- Status --
echo.
echo =============================================
nssm status %SERVICE_NAME%
echo =============================================
echo.
echo Service management commands:
echo   Status : nssm status %SERVICE_NAME%
echo   Stop   : nssm stop %SERVICE_NAME%
echo   Restart: nssm restart %SERVICE_NAME%
echo.
echo NOTE: Nginx does NOT auto-start on boot.
echo   To register Nginx as a service:
echo     nssm install roro-nginx "%RORO_HOME%\nginx\nginx.exe"
echo     nssm set roro-nginx AppDirectory %RORO_HOME%\nginx
echo     nssm set roro-nginx Start SERVICE_AUTO_START
echo.
pause
