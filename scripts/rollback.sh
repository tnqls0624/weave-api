#!/bin/bash
# Rollback Script
# 이전 버전으로 즉시 롤백

set -e

SERVICE_NAME="weave_api"
ENVIRONMENT=${1:-production}

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ROLLBACK] $1"
}

log "Starting rollback for ${ENVIRONMENT}..."

# 1. 이전 이미지 확인
if [ -f /tmp/previous_image.txt ]; then
    PREVIOUS_IMAGE=$(cat /tmp/previous_image.txt)
else
    log "ERROR: No previous image found. Cannot rollback."
    exit 1
fi

if [ "$PREVIOUS_IMAGE" == "none" ] || [ -z "$PREVIOUS_IMAGE" ]; then
    log "ERROR: Invalid previous image. Cannot rollback."
    exit 1
fi

log "Rolling back to: ${PREVIOUS_IMAGE}"

# 2. Blue-Green 롤백 확인
ACTIVE_FILE="/tmp/active_deployment.txt"
if [ -f "$ACTIVE_FILE" ]; then
    ACTIVE_COLOR=$(cat "$ACTIVE_FILE")
    if [ "$ACTIVE_COLOR" == "blue" ]; then
        ROLLBACK_COLOR="green"
    else
        ROLLBACK_COLOR="blue"
    fi

    log "Blue-Green detected. Switching back to ${ROLLBACK_COLOR}..."

    # 트래픽 전환
    docker service update \
        --label-add "traefik.enable=true" \
        weave_${ROLLBACK_COLOR}_api_${ROLLBACK_COLOR} 2>/dev/null || true

    docker service update \
        --label-add "traefik.enable=false" \
        weave_${ACTIVE_COLOR}_api_${ACTIVE_COLOR} 2>/dev/null || true

    echo "${ROLLBACK_COLOR}" > "$ACTIVE_FILE"
    log "Traffic switched to ${ROLLBACK_COLOR}"
else
    # 3. Rolling 롤백
    log "Performing rolling rollback..."
    docker service update \
        --image ${PREVIOUS_IMAGE} \
        --rollback \
        ${SERVICE_NAME}
fi

# 4. Canary 서비스 정리 (있다면)
if docker stack ls | grep -q "weave_api_canary"; then
    log "Cleaning up canary service..."
    docker stack rm weave_api_canary
fi

# 5. 롤백 확인
log "Waiting for rollback to complete..."
sleep 30

# 헬스체크
CONTAINER=$(docker ps --filter "name=${SERVICE_NAME}" --format "{{.ID}}" | head -1)
if [ -n "$CONTAINER" ]; then
    HEALTH=$(docker exec $CONTAINER curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
    if [ "$HEALTH" == "200" ]; then
        log "Rollback successful! Service is healthy."
    else
        log "WARNING: Service health check returned ${HEALTH}"
    fi
else
    log "WARNING: Could not find service container"
fi

log "Rollback completed"
log "Current image: ${PREVIOUS_IMAGE}"

# 6. 서비스 상태 출력
echo ""
echo "=== Service Status ==="
docker service ls | grep weave
echo ""
echo "=== API Service Replicas ==="
docker service ps ${SERVICE_NAME} --format "table {{.Name}}\t{{.CurrentState}}\t{{.Error}}" | head -5
