@echo off
chcp 65001 >nul
REM ===========================================================================
REM register-service.bat — NSSM 服务注册脚本
REM 功能：将 Spring Boot JAR 注册为 Windows 服务，开机自启 + 崩溃自动重启
REM 使用：以管理员身份运行 scripts\register-service.bat
REM 前置条件：NSSM 已安装、JDK 17 已安装
REM ===========================================================================

setlocal enabledelayedexpansion

set RORO_HOME=D:\in_transit_monitor
REM 不设置系统 JAVA_HOME，本项目专用
set SERVICE_NAME=roro-backend

echo =============================================
echo  在途车辆监控系统 — NSSM 服务注册
echo =============================================
echo.

REM —— 检查 NSSM ——
echo [STEP 1/4] 检查 NSSM...
where nssm >nul 2>&1
if errorlevel 1 (
    echo [WARN] NSSM 未安装或不在 PATH 中！
    echo.
    echo 请从 https://nssm.cc/download 下载 NSSM
    echo 解压到 %RORO_HOME%\nssm\ 并添加到 PATH
    echo 或直接使用：set PATH=%%PATH%%;%RORO_HOME%\nssm\win64
    echo.
    REM 尝试从默认位置加载
    if exist "%RORO_HOME%\nssm\win64\nssm.exe" (
        set PATH=%PATH%;%RORO_HOME%\nssm\win64
        echo 找到 NSSM 在: %RORO_HOME%\nssm\win64\nssm.exe
    ) else (
        echo [ERROR] NSSM 不可用，请先安装
        pause
        exit /b 1
    )
)
echo [OK] NSSM 可用

REM —— 检查 JDK（不依赖系统 JAVA_HOME，本项目专用） ——
echo.
echo [STEP 2/4] 检查 JDK...
if "%JDK_PATH%"=="" set JDK_PATH=C:\Program Files\Java\jdk-17

if not exist "%JDK_PATH%\bin\java.exe" (
    echo [ERROR] 未找到 JDK 17！
    echo.
    echo 本项目不使用系统 JAVA_HOME，请在脚本中指定 JDK 路径：
    echo   编辑此脚本，修改 JDK_PATH 变量
    echo   当前搜索路径: %JDK_PATH%
    echo.
    echo   Eclipse Temurin JDK 17 官方支持 Windows Server 2012 R2
    pause
    exit /b 1
)
echo [OK] JDK 路径: %JDK_PATH%

REM —— 检查服务是否已存在 ——
echo.
echo [STEP 3/4] 检查服务状态...
nssm status %SERVICE_NAME% >nul 2>&1
if errorlevel 1 (
    echo 服务不存在，执行注册...
    echo JDK 17 路径: %JDK_PATH%\bin\java.exe
    echo JAR 路径: %RORO_HOME%\backend\ro-ro-monitor.jar
    
    nssm install %SERVICE_NAME% "%JDK_PATH%\bin\java.exe"
    if errorlevel 1 (
        echo [ERROR] 服务注册失败！请以管理员身份运行此脚本
        pause
        exit /b 1
    )
    echo [OK] 服务已注册
) else (
    echo 服务已存在，更新配置...
)

REM —— 设置服务参数 ——
echo.
echo [STEP 4/4] 配置服务参数...

nssm set %SERVICE_NAME% AppParameters "-jar %RORO_HOME%\backend\ro-ro-monitor.jar --spring.profiles.active=prod"
if errorlevel 1 echo [WARN] AppParameters 设置失败

nssm set %SERVICE_NAME% AppDirectory %RORO_HOME%\backend
nssm set %SERVICE_NAME% DisplayName "在途车辆监控系统后端"
nssm set %SERVICE_NAME% Description "Ro-Ro Vehicle Monitor Backend Service — 在途车辆运输监控系统"
nssm set %SERVICE_NAME% Start SERVICE_AUTO_START

REM —— 设置环境变量（重要：ADMIN_DEFAULT_PASSWORD 不能遗漏） ——
nssm set %SERVICE_NAME% AppEnvironmentExtra ^
    DB_HOST=localhost ^
    DB_PORT=3307 ^
    DB_NAME=ro_ro_monitor ^
    DB_USERNAME=roro_app ^
    DB_PASSWORD=CHANGE_ME ^
    ADMIN_DEFAULT_PASSWORD=CHANGE_ME ^
    CORS_ALLOWED_ORIGINS=http://localhost ^
    SERVER_PORT=8080 ^
    TZ=Asia/Shanghai

REM —— 崩溃自动重启 ——
nssm set %SERVICE_NAME% AppThrottle 5000
nssm set %SERVICE_NAME% AppRestartDelay 10000

echo [OK] 服务配置完成

REM —— 启动服务 ——
echo.
echo 启动服务...
nssm start %SERVICE_NAME%
if errorlevel 1 (
    echo [WARN] 启动失败，请检查配置后手动启动
    echo   nssm start %SERVICE_NAME%
) else (
    echo [OK] 服务已启动
)

REM —— 输出状态 ——
echo.
echo =============================================
nssm status %SERVICE_NAME%
echo =============================================
echo.
echo 服务管理命令：
echo   查看状态: nssm status %SERVICE_NAME%
echo   停止服务: nssm stop %SERVICE_NAME%
echo   重启服务: nssm restart %SERVICE_NAME%
echo.
echo ⚠️  注意：Nginx 不会自动开机自启
echo   如需 Nginx 开机自启，可使用 NSSM 注册 nginx 服务:
echo     nssm install roro-nginx "%RORO_HOME%\nginx\nginx.exe"
echo     nssm set roro-nginx AppDirectory %RORO_HOME%\nginx
echo     nssm set roro-nginx Start SERVICE_AUTO_START
echo.
pause
