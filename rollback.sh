#!/bin/bash
set -e

if [ ! -f .previous_tag ]; then
  echo "ERROR: No previous version found (.previous_tag does not exist)."
  echo "Run deploy.sh first to create a deployment history."
  exit 1
fi

PREV_TAG=$(cat .previous_tag)
echo "Rolling back to version: $PREV_TAG"

# Swap tags: current becomes new previous
if [ -f .current_tag ]; then
  CURRENT_TAG=$(cat .current_tag)
  echo "$CURRENT_TAG" > .previous_tag
fi

# Deploy with previous tag
export BACKEND_TAG=$PREV_TAG FRONTEND_TAG=$PREV_TAG
docker compose up -d

# Update current tag
echo "$PREV_TAG" > .current_tag
echo "Rolled back to: $PREV_TAG"

# Health check polling
FRONTEND_PORT=${FRONTEND_PORT:-80}
echo "Waiting for services to be ready..."
for i in $(seq 1 24); do
  sleep 5
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:${FRONTEND_PORT}/actuator/health | grep -q "200"; then
    echo "Services ready after rollback!"
    exit 0
  fi
  echo "  Attempt $i/24 - not ready yet..."
done

echo "ERROR: Services failed to become ready within 120s after rollback"
exit 1
