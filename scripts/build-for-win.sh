#!/usr/bin/env bash
# ============================================================================
# build-for-win.sh -- 开发机构建脚本（在开发机 Linux/macOS 上运行）
# 功能：构建后端 JAR + 前端 dist + 导出基础数据 → 打包 roro-windows-deploy.zip
# 使用：bash scripts/build-for-win.sh
# 依赖：mvn, npm, zip, mysqldump
# ============================================================================

set -euo pipefail

# -- Configurable paths --
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/ro-ro-monitor"
FRONTEND_DIR="$PROJECT_ROOT/ro-ro-monitor-web"
BUILD_DIR="$PROJECT_ROOT/scripts/build-windows"

# -- Color output --
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# -- Step 0: Check required tools --
info "检查依赖工具..."
command -v mvn >/dev/null 2>&1 || { error "mvn 未安装，请先安装 Maven"; exit 1; }
command -v npm >/dev/null 2>&1 || { error "npm 未安装，请先安装 Node.js"; exit 1; }
command -v zip >/dev/null 2>&1 || { error "zip 未安装，请先安装 zip"; exit 1; }
command -v mysqldump >/dev/null 2>&1 || warn "mysqldump 未找到 -- export-master-data.sh 可能无法执行，可后续手动导出"
info "依赖检查通过"

# -- Step 1: Build backend JAR --
info "构建后端 JAR..."
cd "$BACKEND_DIR"
mvn clean package -DskipTests -P prod
JAR_SRC="$BACKEND_DIR/target/ro-ro-monitor-1.0.0.jar"
JAR_DST="ro-ro-monitor.jar"
if [ ! -f "$JAR_SRC" ]; then
    # Try to find any jar
    JAR_SRC=$(find "$BACKEND_DIR/target" -name "*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)
    if [ -z "$JAR_SRC" ]; then
        error "未找到构建产物 JAR"
        exit 1
    fi
    info "使用 JAR: $(basename "$JAR_SRC")"
fi
info "后端构建完成"

# -- Step 2: Build frontend dist --
info "构建前端 dist..."
cd "$FRONTEND_DIR"
npm run build
info "前端构建完成"

# -- Step 3: Export master data --
info "导出基础数据..."
cd "$PROJECT_ROOT"
if [ -f "scripts/export-master-data.sh" ]; then
    bash scripts/export-master-data.sh -h "${DB_HOST:-localhost}" -u "${DB_USER:-root}" -p "$DB_PASS" -P "${DB_PORT:-3306}" -o scripts/master-data.sql || warn "基础数据导出失败，可后续手动导出"
else
    warn "export-master-data.sh 不存在，跳过数据导出"
fi

# -- Step 4: Create build directory --
info "组装部署包..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/backend"
mkdir -p "$BUILD_DIR/frontend"
mkdir -p "$BUILD_DIR/data"
mkdir -p "$BUILD_DIR/sql"

# Copy and rename JAR
cp "$JAR_SRC" "$BUILD_DIR/backend/$JAR_DST"

# Copy frontend dist
cp -r "$FRONTEND_DIR/dist/"* "$BUILD_DIR/frontend/"

# Copy master data if exists
if [ -f "scripts/master-data.sql" ]; then
    cp "scripts/master-data.sql" "$BUILD_DIR/data/"
    rm -f "scripts/master-data.sql"  # clean up
fi

# Export fresh schema from local dev MySQL (one file, all tables)
# Reuse credentials from earlier export (prompt once if needed)
info "导出完整建表 DDL（从本地 MySQL）..."
if command -v mysqldump >/dev/null 2>&1; then
    mysql_args="-h ${DB_HOST:-localhost} -P ${DB_PORT:-3306} -u ${DB_USER:-root}"
    if [ -n "${DB_PASS:-}" ]; then
        mysql_args="$mysql_args -p$DB_PASS"
    fi
    if mysqldump --no-data --default-character-set=utf8mb4 --single-transaction         $mysql_args "$DB_NAME" > "$BUILD_DIR/sql/ro_ro_monitor_schema.sql" 2>/dev/null; then
        info "schema DDL exported"
    else
        warn "schema export failed, falling back to static SQL files"
        SQL_DIR="$BACKEND_DIR/src/main/resources/sql"
        cp "$SQL_DIR"/*.sql "$BUILD_DIR/sql/" 2>/dev/null || true
    fi
else
    SQL_DIR="$BACKEND_DIR/src/main/resources/sql"
    cp "$SQL_DIR"/*.sql "$BUILD_DIR/sql/" 2>/dev/null || true
fi

# Copy deployment scripts (deploy-win.bat + register-service.bat)
mkdir -p "$BUILD_DIR/scripts"
cp "$PROJECT_ROOT/deploy-win.bat" "$BUILD_DIR/"
cp "$PROJECT_ROOT/scripts/register-service.bat" "$BUILD_DIR/scripts/"

# Generate env.bat template with CRLF line endings (cmd.exe requirement)
python3 -c "
lines = [
    '@echo off',
    'REM ===========================================================================',
    'REM env.bat -- Environment variable configuration',
    'REM Edit values before running deploy-win.bat',
    'REM ===========================================================================',
    '',
    'REM JDK 17 path',
    'set JAVA_HOME=C:\\Program Files\\Java\\jdk-17',
    '',
    'REM Database connection',
    'set DB_HOST=localhost',
    'set DB_PORT=3307',
    'set DB_NAME=ro_ro_monitor',
    'set DB_USERNAME=roro_app',
    'set DB_PASSWORD=CHANGE_ME',
    '',
    'REM Admin default password (change after first login)',
    'set ADMIN_DEFAULT_PASSWORD=CHANGE_ME',
    '',
    'REM CORS allowed origins (server IP or domain)',
    'set CORS_ALLOWED_ORIGINS=http://localhost',
    '',
    'REM Backend server port',
    'set SERVER_PORT=8080',
]
with open('$BUILD_DIR/env.bat', 'wb') as f:
    f.write('\r\n'.join(lines).encode('ascii') + b'\r\n')
"


# -- Step 5: Package zip --
info "打包 roro-windows-deploy.zip..."
cd "$BUILD_DIR"
rm -f "$PROJECT_ROOT/scripts/roro-windows-deploy.zip"
zip -r "$PROJECT_ROOT/scripts/roro-windows-deploy.zip" .
cd "$PROJECT_ROOT/scripts"
rm -rf "$BUILD_DIR"

info "✅ 完成！产出文件: scripts/roro-windows-deploy.zip"
echo ""
echo "部署包内容："
unzip -l roro-windows-deploy.zip | head -30
echo ""
echo "下一步："
echo "  1. 将 roro-windows-deploy.zip 拷贝到 Windows Server"
echo "  2. 在服务器上解压到 D:\\in_transit_monitor\\"
echo "  3. 修改 env.bat 中的密码等配置"
echo "  4. 以管理员身份运行 scripts\\register-service.bat"
echo "  5. 运行 deploy-win.bat"
