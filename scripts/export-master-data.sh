#!/usr/bin/env bash
# ============================================================================
# export-master-data.sh — 从本地 MySQL 导出基础信息表为 INSERT 语句
# 功能：导出 10 张基础信息表（品牌、港口、线路等）用于迁移到 Windows MySQL 8.0
# 使用：bash scripts/export-master-data.sh [-h host] [-u user] [-p password] [-P port] [-o output]
# 示例：bash scripts/export-master-data.sh -h localhost -u root -p -P 3306 -o master-data.sql
# ============================================================================

set -euo pipefail

# —— Defaults ——
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASS=""
DB_NAME="ro_ro_monitor"
OUTPUT_FILE="master-data.sql"

# —— Color output ——
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# —— Parse arguments ——
while getopts "h:u:P:p:o:" opt; do
    case $opt in
        h) DB_HOST="$OPTARG" ;;
        u) DB_USER="$OPTARG" ;;
        P) DB_PORT="$OPTARG" ;;
        p) DB_PASS="$OPTARG" ;;
        o) OUTPUT_FILE="$OPTARG" ;;
        *) echo "Usage: $0 [-h host] [-u user] [-p password] [-P port] [-o output]"; exit 1 ;;
    esac
done

# —— Check dependencies ——
command -v mysqldump >/dev/null 2>&1 || { error "mysqldump 未安装"; exit 1; }

# —— Build connection args ——
# If no password given, prompt once at the start
if [ -z "$DB_PASS" ]; then
    read -s -p "MySQL 密码: " DB_PASS
    echo ""
fi
MYSQL_ARGS="-h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS"

# —— Test connection ——
info "测试数据库连接..."
mysql $MYSQL_ARGS -e "SELECT 1" >/dev/null 2>&1 || { error "无法连接到 MySQL $DB_HOST:$DB_PORT"; exit 1; }
info "连接成功"

# —— Master data tables (10) ——
# Ordered by FK dependency: parents first, then dependent tables
TABLES=(
    "brand_dict"              # 品牌字典（父表）
    "port_dict"               # 港口字典（父表）
    "monitor_status_dict"     # 监控状态字典（父表）
    "transport_status_dict"   # 在途状态字典（父表）
    "users"                   # 用户表（无 FK）
    "location_alias"          # 地点别名（无 FK）
    "excel_field_mapping"     # Excel 字段映射（FK→brand_dict）
    "excel_parse_config"      # Excel 解析配置（FK→brand_dict）
    "route_dict"              # 线路表（FK→brand_dict, port_dict）
    "route_otd_config"        # OTD 时效配置（FK→route_dict）
)

# —— Business tables (NOT exported) ——
BUSINESS_TABLES=(
    "order_info"
    "vehicle_transit"
    "upload_batch"
    "monitor_snapshot"
)

MYSQLDUMP_ARGS="--no-create-info --replace --complete-insert --default-character-set=utf8mb4 --single-transaction --skip-triggers --skip-add-locks --skip-comments --skip-tz-utc"

# —— Export ——
info "开始导出基础数据到: $OUTPUT_FILE"

# Write header
cat > "$OUTPUT_FILE" << 'HEADER'
-- ============================================================================
-- master-data.sql — 基础信息数据（由 export-master-data.sh 自动生成）
-- 用于迁移到 Windows Server MySQL 8.0
-- 注意：仅包含 INSERT 语句，不包含 DDL（建表已由 init SQL 完成）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

HEADER

# Export each table
SUMMARY=""
info "导出全部 ${#TABLES[@]} 张基础信息表..."

if mysqldump $MYSQL_ARGS $MYSQLDUMP_ARGS "$DB_NAME" ${TABLES[@]} >> "$OUTPUT_FILE" 2>/dev/null; then
    # Count rows per table (no password prompt — already authenticated)
    for table in "${TABLES[@]}"; do
        row_count=$(mysql $MYSQL_ARGS -N -e "SELECT COUNT(*) FROM $table" "$DB_NAME" 2>/dev/null || echo "?")
        SUMMARY="$SUMMARY  - $table: $row_count 行\n"
        info "  → $table: $row_count 行"
    done
else
    error "mysqldump 失败，请检查数据库连接和权限"
    exit 1
fi

# Write footer (restore FK checks)
cat >> "$OUTPUT_FILE" << 'FOOTER'

SET FOREIGN_KEY_CHECKS = 1;

FOOTER

# Verify no business tables were exported
info "验证：检查是否包含业务数据..."
for table in "${BUSINESS_TABLES[@]}"; do
    if grep -q "$table" "$OUTPUT_FILE" 2>/dev/null; then
        warn "⚠️  警告：$table 出现在导出文件中，这不应该发生！"
    fi
done

# —— Summary ——
echo ""
info "======================================"
info "导出完成！"
echo -e "$SUMMARY"
info "======================================"
info "文件: $(pwd)/$OUTPUT_FILE"
info "大小: $(wc -c < "$OUTPUT_FILE") bytes"
info "行数: $(wc -l < "$OUTPUT_FILE") 行"
echo ""
info "打包部署包时，此文件会自动包含在 zip 的 data/ 目录下"
