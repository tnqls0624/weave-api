#!/bin/bash
# Health Check Script
# 배포 후 서비스 상태 확인

set -e

ENVIRONMENT=${1:-production}
MAX_RETRIES=${2:-10}
RETRY_INTERVAL=${3:-10}

SERVICE_NAME="weave_api"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [HEALTH] $1"
}

log "Starting health check for ${ENVIRONMENT}..."

# 1. 서비스 존재 확인
if ! docker service ls | grep -q ${SERVICE_NAME}; then
    log "ERROR: Service ${SERVICE_NAME} not found"
    exit 1
fi

# 2. 헬스체크 수행
RETRY_COUNT=0
HEALTHY=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    log "Health check attempt ${RETRY_COUNT}/${MAX_RETRIES}..."

    # API 헬스 엔드포인트 체크
    HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/actuator/health 2>/dev/null || echo "000")

    if [ "$HEALTH_STATUS" == "200" ]; then
        log "Health endpoint returned 200 OK"

        # 상세 헬스 정보 확인
        HEALTH_DETAIL=$(curl -s http://localhost/actuator/health 2>/dev/null || echo "{}")
        log "Health details: ${HEALTH_DETAIL}"

        # 모든 컴포넌트 상태 확인
        if echo "$HEALTH_DETAIL" | grep -q '"status":"UP"'; then
            log "All components are healthy!"
            HEALTHY=true
            break
        else
            log "WARNING: Some components may be unhealthy"
        fi
    else
        log "Health check failed with status: ${HEALTH_STATUS}"
    fi

    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
        log "Retrying in ${RETRY_INTERVAL} seconds..."
        sleep $RETRY_INTERVAL
    fi
done

if [ "$HEALTHY" = true ]; then
    log "Health check passed!"

    # 추가 검증
    echo ""
    echo "=== Additional Checks ==="

    # Database 연결 확인
    log "Checking database connection..."
    DB_STATUS=$(curl -s http://localhost/actuator/health/mongo 2>/dev/null | grep -o '"status":"[^"]*"' | head -1 || echo "unknown")
    log "MongoDB: ${DB_STATUS}"

    # Redis 연결 확인
    log "Checking Redis connection..."
    REDIS_STATUS=$(curl -s http://localhost/actuator/health/redis 2>/dev/null | grep -o '"status":"[^"]*"' | head -1 || echo "unknown")
    log "Redis: ${REDIS_STATUS}"

    # 메모리 사용량 확인
    log "Checking memory usage..."
    MEM_INFO=$(curl -s http://localhost/actuator/metrics/jvm.memory.used 2>/dev/null | grep -o '"value":[0-9]*' | head -1 || echo "unknown")
    log "JVM Memory: ${MEM_INFO}"

    exit 0
else
    log "ERROR: Health check failed after ${MAX_RETRIES} attempts"
    exit 1
fi
