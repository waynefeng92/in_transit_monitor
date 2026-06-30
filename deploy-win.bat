@echo off
REM ===========================================================================
REM deploy-win.bat -- Windows Server Deployment / Update Script
REM Run as Administrator on Windows Server
REM First run: initialize database + deploy application
REM Subsequent: backup old version -> stop -> replace -> start
REM ===========================================================================

setlocal enabledelayedexpansion

REM -- Paths --
set RORO_HOME=D:\in_transit_monitor
set BACKEND_DIR=%RORO_HOME%\backend
set FRONTEND_DIR=%RORO_HOME%\frontend
set BACKUP_DIR=%RORO_HOME%\backups
set SQL_DIR=%RORO_HOME%\sql
set DATA_DIR=%RORO_HOME%\data

REM -- Load env.bat --
if exist "%RORO_HOME%\env.bat" (
    echo [INFO] Loading env.bat
    call "%RORO_HOME%\env.bat"
) else (
    echo [WARN] env.bat not found, using defaults
)

REM -- MySQL executable (full path; adjust if installed elsewhere) --
set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
echo =============================================
echo   Ro-Ro Vehicle Monitor - Deployment Script
echo =============================================
echo Deploy path: %RORO_HOME%

REM -- First deploy or iterative update? --
REM Check if database exists (more reliable than file check)
"%MYSQL_EXE%" -u root -p%DB_PASSWORD% -P %DB_PORT% -h %DB_HOST% -e "USE %DB_NAME%" 2>nul
if errorlevel 1 (
    echo =============================================
    echo   FIRST DEPLOY MODE (database not found)
    echo =============================================
    goto :first_deploy
) else (
    echo =============================================
    echo   ITERATIVE UPDATE MODE
    echo =============================================
    goto :iterative_update
)

REM ===========================================================================
REM FIRST DEPLOY
REM ===========================================================================
:first_deploy
echo.
echo [STEP 1/7] Creating directory structure...
if not exist "%BACKEND_DIR%" mkdir "%BACKEND_DIR%"
if not exist "%FRONTEND_DIR%" mkdir "%FRONTEND_DIR%"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if not exist "%RORO_HOME%\logs" mkdir "%RORO_HOME%\logs"
echo [OK]

echo.
echo [STEP 2/7] Creating database schema...
echo Running schema SQL files...
for %%f in ("%SQL_DIR%\*.sql") do (
    echo   Running: %%~nxf
    "%MYSQL_EXE%" -u root -p%DB_PASSWORD% -P %DB_PORT% -h %DB_HOST% < "%%f"
    if !errorlevel! neq 0 (
        echo [ERROR] Failed: %%~nxf
        pause
        exit /b 1
    )
)
echo [OK] All SQL files executed

echo.
echo [STEP 2.5] Creating application user roro_app...
"%MYSQL_EXE%" -u root -p%DB_PASSWORD% -P %DB_PORT% -h %DB_HOST% -e "CREATE USER IF NOT EXISTS 'roro_app'@'localhost' IDENTIFIED BY '%DB_PASSWORD%'; GRANT SELECT, INSERT, UPDATE, DELETE ON %DB_NAME%.* TO 'roro_app'@'localhost'; FLUSH PRIVILEGES;"
if !errorlevel! neq 0 (
    echo [WARN] Failed to create roro_app user, continuing...
) else (
    echo [OK] roro_app user created
)
echo [STEP 3/7] Importing base data...
if exist "%DATA_DIR%\master-data.sql" (
    "%MYSQL_EXE%" -u root -p%DB_PASSWORD% -P %DB_PORT% -h %DB_HOST% %DB_NAME% < "%DATA_DIR%\master-data.sql"
    if !errorlevel! neq 0 (
        echo [ERROR] Base data import failed!
        pause
        exit /b 1
    )
    echo [OK] Base data imported
) else (
    echo [WARN] master-data.sql not found, skipping data import
)

echo.
echo.
echo [STEP 4/7] Verifying backend JAR...
if not exist "%BACKEND_DIR%\ro-ro-monitor.jar" (
    echo [ERROR] Backend JAR not found! Check zip extraction.
    pause
    exit /b 1
)
echo [OK] Backend JAR in place
echo.
echo [STEP 5/7] Verifying frontend files...
if not exist "%FRONTEND_DIR%\index.html" (
    echo [WARN] frontend/index.html not found! Check zip extraction.
)
echo [OK] Frontend files in place

echo.
echo [STEP 6/7] Registering NSSM service...
if exist "%~dp0scripts\register-service.bat" (
    call "%~dp0scripts\register-service.bat"
) else if exist "%RORO_HOME%\scripts\register-service.bat" (
    call "%RORO_HOME%\scripts\register-service.bat"
) else (
    echo [WARN] register-service.bat not found, register service manually
)

echo.
echo [STEP 7/7] Starting services and verifying...
call :start_services

echo.
echo =============================================
echo   First deploy complete!
echo   Open http://localhost in browser to verify
echo =============================================
echo deployed > "%RORO_HOME%\.first_deploy_done"
goto :end

REM ===========================================================================
REM ITERATIVE UPDATE
REM ===========================================================================
:iterative_update
echo.
echo [STEP 1/7] Backing up current JAR...

REM Generate timestamp (YYYYMMDD_HHMMSS)
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do set DATETIME=%%I
if "%DATETIME%"=="" (
    set DATETIME=%DATE:~0,4%%DATE:~5,2%%DATE:~8,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%
    set DATETIME=!DATETIME: =0!
)
set TIMESTAMP=!DATETIME:~0,8!_!DATETIME:~8,6!

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
copy "%BACKEND_DIR%\ro-ro-monitor.jar" "%BACKUP_DIR%\ro-ro-monitor_!TIMESTAMP!.jar"
if !errorlevel! equ 0 (
    echo [OK] Backed up to %BACKUP_DIR%\ro-ro-monitor_!TIMESTAMP!.jar
) else (
    echo [WARN] Backup failed, continuing anyway
)

echo.
echo [STEP 2/7] Stopping backend service...
nssm stop roro-backend
if !errorlevel! neq 0 (
    echo [WARN] nssm stop failed, trying taskkill...
    taskkill /f /im java.exe 2>nul
)
echo [OK]

echo.
echo [STEP 3/7] Stopping Nginx...
taskkill /f /im nginx.exe 2>nul
echo [OK]

echo.
echo [STEP 4/7] Replacing backend JAR...
copy /Y "%~dp0backend\ro-ro-monitor.jar" "%BACKEND_DIR%\ro-ro-monitor.jar"
if !errorlevel! neq 0 (
    echo [ERROR] Backend JAR replacement failed!
    pause
    exit /b 1
)
echo [OK]

echo.
echo [STEP 5/7] Replacing frontend files...
xcopy /E /Y "%~dp0frontend\*.*" "%FRONTEND_DIR%\"
echo [OK]

echo.
echo [STEP 6/7] Starting backend service...
nssm start roro-backend
if !errorlevel! neq 0 (
    echo [ERROR] Service start failed!
    pause
    exit /b 1
)
echo [OK]

echo.
echo [STEP 7/7] Health check + starting Nginx...
call :wait_for_health

taskkill /f /im nginx.exe 2>nul
start "" "%RORO_HOME%\nginx\nginx.exe"
echo [OK] Nginx started

echo.
echo =============================================
echo   Iterative update complete!
echo =============================================
goto :end

REM ===========================================================================
REM Start services (first deploy)
REM ===========================================================================
:start_services
echo Starting backend service...
nssm start roro-backend
if !errorlevel! neq 0 (
    echo [WARN] nssm start failed, check service status
)
call :wait_for_health

echo Starting Nginx...
if exist "%RORO_HOME%\nginx\nginx.exe" (
    taskkill /f /im nginx.exe 2>nul
    start "" "%RORO_HOME%\nginx\nginx.exe"
    echo [OK]
) else (
    echo [WARN] Nginx not installed or path incorrect
)
goto :eof

REM ===========================================================================
REM Health check polling
REM ===========================================================================
:wait_for_health
echo Waiting for backend to become ready (max 120s)...
set HEALTH_URL=http://localhost:8080/actuator/health
set MAX_RETRIES=24
set RETRY_COUNT=0

:health_loop
timeout /t 5 /nobreak >nul
set /a RETRY_COUNT+=1

powershell -Command "try { $r = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if !errorlevel! equ 0 (
    echo [OK] Backend is ready
    goto :eof
)

if !RETRY_COUNT! geq !MAX_RETRIES! (
    echo [ERROR] Health check timeout! Check backend manually:
    echo   curl %HEALTH_URL%
    goto :eof
)

echo Waiting (attempt !RETRY_COUNT!/!MAX_RETRIES!)
goto :health_loop

:end
pause
