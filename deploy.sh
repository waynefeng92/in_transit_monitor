#!/bin/bash
set -e

TAG=$(git rev-parse --short HEAD)
echo "Building version: $TAG"

# Build images
docker build -t roro-backend:$TAG ./ro-ro-monitor
docker build -t roro-frontend:$TAG ./ro-ro-monitor-web

# Record previous tag for rollback
if [ -f .current_tag ]; then
  cp .current_tag .previous_tag
  echo "Previous version: $(cat .previous_tag)"
fi

# Export tags and deploy
export BACKEND_TAG=$TAG FRONTEND_TAG=$TAG
docker compose up -d

# Record current tag
echo "$TAG" > .current_tag
echo "Deployed version: $TAG"

# Health check polling
FRONTEND_PORT=${FRONTEND_PORT:-80}
echo "Waiting for services to be ready..."
for i in $(seq 1 24); do
  sleep 5
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:${FRONTEND_PORT}/actuator/health | grep -q "200"; then
    echo "Services ready!"
    exit 0
  fi
  echo "  Attempt $i/24 - not ready yet..."
done

echo "ERROR: Services failed to become ready within 120s"
exit 1
