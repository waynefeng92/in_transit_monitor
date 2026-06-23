#!/bin/bash
set -e

# Config
TRACK_FILE=".last_migration"
SQL_DIR="ro-ro-monitor/src/main/resources/sql"

# Source .env for DB credentials
if [ -f .env ]; then
  source .env
else
  echo "ERROR: .env file not found"
  exit 1
fi

# Read last applied migration
LAST="none"
if [ -f "$TRACK_FILE" ]; then
  LAST=$(cat "$TRACK_FILE")
fi

echo "Last migration: $LAST"

# Iterate through SQL files (sorted by name)
APPLIED=0
for sql in $(ls "$SQL_DIR"/*.sql 2>/dev/null | sort); do
  fname=$(basename "$sql")
  
  # Skip init scripts (0_ to 4_) - they are executed by initdb.d
  if [[ "$fname" =~ ^[0-4]_ ]]; then
    echo "  Skip (init): $fname"
    continue
  fi
  
  # Check if already applied
  if [ "$LAST" != "none" ] && [ "$fname" \< "$LAST" -o "$fname" = "$LAST" ]; then
    echo "  Skip (done): $fname"
    continue
  fi
  
  # Execute migration
  echo "  Apply: $fname"
  docker exec -i roro-mysql mysql -uroot -p"${DB_PASSWORD}" ${DB_NAME:-ro_ro_monitor} < "$sql"
  
  # Record success
  echo "$fname" > "$TRACK_FILE"
  APPLIED=$((APPLIED + 1))
done

echo "Migrations applied: $APPLIED"
