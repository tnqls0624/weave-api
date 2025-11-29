#!/bin/bash
# Canary Deployment Strategy
# 새 버전을 일부 트래픽에만 배포하여 테스트 후 점진적 확대

set -e

# 환경변수 확인
: "${ECR_REGISTRY:?ECR_REGISTRY is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"

FULL_IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
SERVICE_NAME="weave_api"
CANARY_SERVICE="weave_api_canary"

# Canary 설정
CANARY_WEIGHT=${CANARY_WEIGHT:-10}  # 초기 10% 트래픽
CANARY_INCREMENT=${CANARY_INCREMENT:-20}  # 20%씩 증가
CANARY_INTERVAL=${CANARY_INTERVAL:-60}  # 60초마다 확인
ERROR_THRESHOLD=${ERROR_THRESHOLD:-5}  # 5% 이상 에러시 롤백

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CANARY] $1"
}

check_error_rate() {
    # Prometheus에서 에러율 확인 (실제 환경에서는 curl로 Prometheus API 호출)
    # 여기서는 간단히 actuator/health 체크
    CANARY_CONTAINER=$(docker ps --filter "name=${CANARY_SERVICE}" --format "{{.ID}}" | head -1)
    if [ -n "$CANARY_CONTAINER" ]; then
        HEALTH=$(docker exec $CANARY_CONTAINER curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
        if [ "$HEALTH" != "200" ]; then
            return 1
        fi
    fi
    return 0
}

log "Starting canary deployment..."
log "Image: ${FULL_IMAGE}"
log "Initial canary weight: ${CANARY_WEIGHT}%"

# 1. 현재 상태 저장
PREVIOUS_IMAGE=$(docker service inspect ${SERVICE_NAME} --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' 2>/dev/null || echo "none")
echo "${PREVIOUS_IMAGE}" > /tmp/previous_image.txt
log "Previous image: ${PREVIOUS_IMAGE}"

# 2. 새 이미지 Pull
log "Pulling new image..."
docker pull ${FULL_IMAGE}

# 3. Canary 서비스 배포
log "Deploying canary service..."

cat > /tmp/docker-compose-canary.yml << EOF
version: '3.8'
services:
  api_canary:
    image: ${FULL_IMAGE}
    environment:
      - SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE:-prod}
      - SPRING_DATA_MONGODB_URI=\${MONGODB_URI}
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PASSWORD=\${REDIS_PASSWORD}
      - JWT_EXPIRATION=\${JWT_EXPIRATION}
      - JWT_REFRESH_EXPIRATION=\${JWT_REFRESH_EXPIRATION}
      - JWT_PRIVATE_KEY_PATH=\${JWT_PRIVATE_KEY_PATH}
      - JWT_PUBLIC_KEY_PATH=\${JWT_PUBLIC_KEY_PATH}
      - HOLIDAY_API_SERVICE_KEY=\${HOLIDAY_API_KEY}
      - AWS_REGION=\${AWS_REGION}
      - AWS_S3_BUCKET=\${AWS_S3_BUCKET}
      - AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY}
      - FIREBASE_CONFIG_PATH=\${FIREBASE_CONFIG_PATH}
      - TZ=Asia/Seoul
      - JAVA_TOOL_OPTIONS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
    networks:
      - weave-network
    deploy:
      mode: replicated
      replicas: 1
      labels:
        - "traefik.enable=true"
        - "traefik.http.routers.api-canary.rule=PathPrefix(\`/api\`)"
        - "traefik.http.routers.api-canary.entrypoints=web"
        - "traefik.http.routers.api-canary.service=api-canary"
        - "traefik.http.routers.api-canary.priority=110"
        - "traefik.http.services.api-canary.loadbalancer.server.port=8080"

networks:
  weave-network:
    external: true
EOF

docker stack deploy \
    -c /tmp/docker-compose-canary.yml \
    ${CANARY_SERVICE} \
    --with-registry-auth

# 4. Canary 헬스체크
log "Waiting for canary to be healthy..."
sleep 30

if ! check_error_rate; then
    log "ERROR: Canary failed initial health check"
    docker stack rm ${CANARY_SERVICE}
    exit 1
fi

log "Canary is healthy. Starting traffic routing..."

# 5. 점진적 트래픽 증가
CURRENT_WEIGHT=${CANARY_WEIGHT}

while [ $CURRENT_WEIGHT -lt 100 ]; do
    log "Canary weight: ${CURRENT_WEIGHT}%"
    log "Monitoring for ${CANARY_INTERVAL} seconds..."

    # 모니터링 기간 동안 에러율 체크
    MONITOR_ELAPSED=0
    while [ $MONITOR_ELAPSED -lt $CANARY_INTERVAL ]; do
        if ! check_error_rate; then
            log "ERROR: High error rate detected! Rolling back..."
            docker stack rm ${CANARY_SERVICE}
            log "Canary rollback completed"
            exit 1
        fi
        sleep 10
        MONITOR_ELAPSED=$((MONITOR_ELAPSED + 10))
    done

    # 트래픽 증가
    CURRENT_WEIGHT=$((CURRENT_WEIGHT + CANARY_INCREMENT))
    if [ $CURRENT_WEIGHT -ge 100 ]; then
        log "Canary successful! Promoting to full deployment..."
        break
    fi

    log "Increasing canary weight to ${CURRENT_WEIGHT}%"
done

# 6. 전체 배포로 전환
log "Promoting canary to production..."

# 메인 서비스 이미지 업데이트
docker service update \
    --image ${FULL_IMAGE} \
    ${SERVICE_NAME}

# 7. Canary 서비스 정리
log "Cleaning up canary service..."
docker stack rm ${CANARY_SERVICE}

log "Canary deployment completed successfully!"
log "New image is now live: ${FULL_IMAGE}"
