#!/bin/bash
set -e

# Config
BACKUP_DIR="backups"
MYSQL_CONTAINER="roro-mysql"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Source .env for DB credentials
if [ -f .env ]; then
  source .env
else
  echo "ERROR: .env file not found"
  exit 1
fi

DATE=$(date +%Y%m%d)
BACKUP_FILE="${BACKUP_DIR}/ro_ro_monitor_${DATE}.sql"

echo "Backing up database ${DB_NAME:-ro_ro_monitor}..."
docker exec "$MYSQL_CONTAINER" mysqldump \
  -uroot -p"${DB_PASSWORD}" \
  --single-transaction \
  --routines \
  --triggers \
  "${DB_NAME:-ro_ro_monitor}" > "$BACKUP_FILE"

# Check backup size
SIZE=$(stat -c%s "$BACKUP_FILE" 2>/dev/null || echo "0")
echo "Backup saved: $BACKUP_FILE ($SIZE bytes)"

# Clean up backups older than 7 days
find "$BACKUP_DIR" -name "ro_ro_monitor_*.sql" -mtime +7 -delete
echo "Cleaned up backups older than 7 days"
