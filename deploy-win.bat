@echo off
REM ===========================================================================
REM deploy-win.bat — Windows Server 部署/更新脚本
REM 功能：首次部署 — 初始化数据库 + 部署应用
REM       迭代更新 — 备份旧版 → 停止服务 → 替换文件 → 启动服务
REM 使用：以管理员身份运行 deploy-win.bat
REM 注意：首次部署前请先修改 env.bat 中的配置
REM ===========================================================================

setlocal enabledelayedexpansion

REM —— 路径配置 ——
set RORO_HOME=D:\in_transit_monitor
set BACKEND_DIR=%RORO_HOME%\backend
set FRONTEND_DIR=%RORO_HOME%\frontend
set BACKUP_DIR=%RORO_HOME%\backups
set SQL_DIR=%RORO_HOME%\sql
set DATA_DIR=%RORO_HOME%\data

REM —— 加载环境变量 ——
if exist "%RORO_HOME%\env.bat" (
    echo [INFO] 加载环境变量: %RORO_HOME%\env.bat
    call "%RORO_HOME%\env.bat"
) else (
    echo [WARN] env.bat 不存在，使用默认环境变量
)

REM —— MySQL 连接参数 ——
set MYSQL_CMD=mysql -u root -p%DB_PASSWORD% -P %DB_PORT% -h %DB_HOST%
set MYSQL_IMPORT=mysql -u root -p%DB_PASSWORD% -P %DB_PORT% -h %DB_HOST% %DB_NAME%

echo =============================================
echo  在途车辆监控系统 — Windows 部署脚本
echo =============================================
echo 部署路径: %RORO_HOME%
echo 首次部署判断: 检查 %BACKEND_DIR%\ro-ro-monitor.jar

REM —— 判断首次部署还是迭代更新 ——
if not exist "%BACKEND_DIR%\ro-ro-monitor.jar" (
    echo =============================================
    echo  首次部署模式
    echo =============================================
    goto :first_deploy
) else (
    echo =============================================
    echo  迭代更新模式
    echo =============================================
    goto :iterative_update
)

REM ===========================================================================
REM 首次部署
REM ===========================================================================
:first_deploy
echo.
echo [STEP 1/7] 创建目录结构...
if not exist "%BACKEND_DIR%" mkdir "%BACKEND_DIR%"
if not exist "%FRONTEND_DIR%" mkdir "%FRONTEND_DIR%"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if not exist "%RORO_HOME%\logs" mkdir "%RORO_HOME%\logs"
echo [OK]

echo.
echo [STEP 2/7] 初始化数据库（以 root 执行 DDL）...
echo ⚠️  将执行 0_ro_ro_monitor_full.sql（含 DROP DATABASE），仅首次运行！
if exist "%SQL_DIR%\0_ro_ro_monitor_full.sql" (
    %MYSQL_CMD% < "%SQL_DIR%\0_ro_ro_monitor_full.sql"
    if !errorlevel! neq 0 (
        echo [ERROR] 建表 DDL 执行失败！请检查 MySQL 连接和密码
        pause
        exit /b 1
    )
    echo [OK] 数据库表结构创建完成
) else (
    echo [WARN] 未找到 0_ro_ro_monitor_full.sql，跳过建表
)

echo.
echo [STEP 3/7] 导入基础数据...
if exist "%DATA_DIR%\master-data.sql" (
    %MYSQL_IMPORT% < "%DATA_DIR%\master-data.sql"
    if !errorlevel! neq 0 (
        echo [ERROR] 基础数据导入失败！
        pause
        exit /b 1
    )
    echo [OK] 基础数据导入完成
) else (
    echo [WARN] 未找到 master-data.sql，跳过基础数据导入
)

echo.
echo [STEP 4/7] 部署后端 JAR...
if not exist "%BACKEND_DIR%" mkdir "%BACKEND_DIR%"
copy /Y "%~dp0backend\ro-ro-monitor.jar" "%BACKEND_DIR%\ro-ro-monitor.jar"
if !errorlevel! neq 0 (
    echo [ERROR] 后端 JAR 部署失败！
    pause
    exit /b 1
)
echo [OK] 后端 JAR 已部署

echo.
echo [STEP 5/7] 部署前端静态文件...
if not exist "%FRONTEND_DIR%" mkdir "%FRONTEND_DIR%"
xcopy /E /Y "%~dp0frontend\*.*" "%FRONTEND_DIR%\"
if !errorlevel! neq 0 (
    echo [WARN] 前端文件部署可能不完整
)
echo [OK] 前端文件已部署

echo.
echo [STEP 6/7] 注册 NSSM 服务...
if exist "%~dp0scripts\register-service.bat" (
    call "%~dp0scripts\register-service.bat"
) else if exist "%RORO_HOME%\register-service.bat" (
    call "%RORO_HOME%\register-service.bat"
) else (
    echo [WARN] register-service.bat 未找到，请手动注册服务
)

echo.
echo [STEP 7/7] 启动服务并验证...
call :start_services

echo.
echo =============================================
echo  首次部署完成！
echo  请通过浏览器访问 http://localhost 验证
echo =============================================
goto :end

REM ===========================================================================
REM 迭代更新
REM ===========================================================================
:iterative_update
echo.
echo [STEP 1/7] 备份旧版 JAR...

REM 生成时间戳（YYYYMMDD_HHMMSS 格式）
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do set DATETIME=%%I
if "%DATETIME%"=="" (
    REM 备用方案
    set DATETIME=%DATE:~0,4%%DATE:~5,2%%DATE:~8,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%
    set DATETIME=!DATETIME: =0!
)
set TIMESTAMP=!DATETIME:~0,8!_!DATETIME:~8,6!

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
copy "%BACKEND_DIR%\ro-ro-monitor.jar" "%BACKUP_DIR%\ro-ro-monitor_!TIMESTAMP!.jar"
if !errorlevel! equ 0 (
    echo [OK] 已备份到 %BACKUP_DIR%\ro-ro-monitor_!TIMESTAMP!.jar
) else (
    echo [WARN] 备份失败，继续执行
)

echo.
echo [STEP 2/7] 停止后端服务...
nssm stop roro-backend
if !errorlevel! neq 0 (
    echo [WARN] nssm stop 失败，尝试 taskkill...
    taskkill /f /im java.exe 2>nul
)
echo [OK]

echo.
echo [STEP 3/7] 停止 Nginx...
taskkill /f /im nginx.exe 2>nul
echo [OK]

echo.
echo [STEP 4/7] 替换后端 JAR...
copy /Y "%~dp0backend\ro-ro-monitor.jar" "%BACKEND_DIR%\ro-ro-monitor.jar"
if !errorlevel! neq 0 (
    echo [ERROR] 后端 JAR 替换失败！
    pause
    exit /b 1
)
echo [OK]

echo.
echo [STEP 5/7] 替换前端文件...
xcopy /E /Y "%~dp0frontend\*.*" "%FRONTEND_DIR%\"
echo [OK]

echo.
echo [STEP 6/7] 启动后端服务...
nssm start roro-backend
if !errorlevel! neq 0 (
    echo [ERROR] 服务启动失败！
    pause
    exit /b 1
)
echo [OK]

echo.
echo [STEP 7/7] 健康检查 + 启动 Nginx...
call :wait_for_health

taskkill /f /im nginx.exe 2>nul
start "" "%RORO_HOME%\nginx\nginx.exe"
echo [OK] Nginx 已启动

echo.
echo =============================================
echo  迭代更新完成！
echo =============================================
goto :end

REM ===========================================================================
REM 启动服务（首次部署使用）
REM ===========================================================================
:start_services
echo 启动后端服务...
nssm start roro-backend
if !errorlevel! neq 0 (
    echo [WARN] nssm start 失败，请检查服务状态
)
call :wait_for_health

echo 启动 Nginx...
if exist "%RORO_HOME%\nginx\nginx.exe" (
    taskkill /f /im nginx.exe 2>nul
    start "" "%RORO_HOME%\nginx\nginx.exe"
    echo [OK]
) else (
    echo [WARN] Nginx 未安装或路径不正确
)
goto :eof

REM ===========================================================================
REM 健康检查轮询
REM ===========================================================================
:wait_for_health
echo 等待后端就绪（最多 120 秒）...
set HEALTH_URL=http://localhost:8080/actuator/health
set MAX_RETRIES=24
set RETRY_COUNT=0

:health_loop
timeout /t 5 /nobreak >nul
set /a RETRY_COUNT+=1

REM 使用 PowerShell 进行 HTTP 请求
powershell -Command "try { $r = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if !errorlevel! equ 0 (
    echo [OK] 后端已就绪
    goto :eof
)

if !RETRY_COUNT! geq !MAX_RETRIES! (
    echo [ERROR] 健康检查超时！请手动检查后端状态
    echo  curl %HEALTH_URL%
    goto :eof
)

echo 等待中（第 !RETRY_COUNT! 次 / !MAX_RETRIES! 次）
goto :health_loop

:end
pause
