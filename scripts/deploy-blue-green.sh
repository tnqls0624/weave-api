#!/bin/bash
# Blue-Green Deployment Strategy
# 새 버전을 별도 환경에 배포 후 트래픽 전환
# Zero-downtime, 즉시 롤백 가능

set -e

# 환경변수 확인
: "${ECR_REGISTRY:?ECR_REGISTRY is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"

FULL_IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
BLUE_SERVICE="weave_api_blue"
GREEN_SERVICE="weave_api_green"
ACTIVE_FILE="/tmp/active_deployment.txt"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [BLUE-GREEN] $1"
}

get_active_color() {
    if [ -f "$ACTIVE_FILE" ]; then
        cat "$ACTIVE_FILE"
    else
        echo "blue"
    fi
}

log "Starting blue-green deployment..."
log "Image: ${FULL_IMAGE}"

# 1. 현재 활성 환경 확인
ACTIVE_COLOR=$(get_active_color)
if [ "$ACTIVE_COLOR" == "blue" ]; then
    DEPLOY_COLOR="green"
    DEPLOY_SERVICE=$GREEN_SERVICE
    ACTIVE_SERVICE=$BLUE_SERVICE
else
    DEPLOY_COLOR="blue"
    DEPLOY_SERVICE=$BLUE_SERVICE
    ACTIVE_SERVICE=$GREEN_SERVICE
fi

log "Active: ${ACTIVE_COLOR}, Deploying to: ${DEPLOY_COLOR}"

# 2. 현재 상태 저장
PREVIOUS_IMAGE=$(docker service inspect ${ACTIVE_SERVICE} --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' 2>/dev/null || echo "none")
echo "${PREVIOUS_IMAGE}" > /tmp/previous_image.txt
log "Previous image: ${PREVIOUS_IMAGE}"

# 3. 새 이미지 Pull
log "Pulling new image..."
docker pull ${FULL_IMAGE}

# 4. 대기 환경에 새 버전 배포
log "Deploying to ${DEPLOY_COLOR} environment..."

# 임시 compose 파일 생성
cat > /tmp/docker-compose-${DEPLOY_COLOR}.yml << EOF
version: '3.8'
services:
  api_${DEPLOY_COLOR}:
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
        - "traefik.enable=false"

networks:
  weave-network:
    external: true
EOF

docker stack deploy \
    -c /tmp/docker-compose-${DEPLOY_COLOR}.yml \
    weave_${DEPLOY_COLOR} \
    --with-registry-auth

# 5. 새 환경 헬스체크
log "Waiting for ${DEPLOY_COLOR} environment to be healthy..."
TIMEOUT=120
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
    # 서비스 상태 확인
    CONTAINER=$(docker ps --filter "name=weave_${DEPLOY_COLOR}_api" --format "{{.ID}}" | head -1)
    if [ -n "$CONTAINER" ]; then
        HEALTH=$(docker exec $CONTAINER curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
        if [ "$HEALTH" == "200" ]; then
            log "${DEPLOY_COLOR} environment is healthy!"
            break
        fi
    fi

    log "Waiting... (${ELAPSED}s)"
    sleep 10
    ELAPSED=$((ELAPSED + 10))
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    log "ERROR: ${DEPLOY_COLOR} environment failed health check"
    docker stack rm weave_${DEPLOY_COLOR}
    exit 1
fi

# 6. 트래픽 전환 (Traefik 라벨 업데이트)
log "Switching traffic to ${DEPLOY_COLOR}..."

# 새 환경에 Traefik 라벨 활성화
docker service update \
    --label-add "traefik.enable=true" \
    --label-add "traefik.http.routers.api.service=api_${DEPLOY_COLOR}" \
    --label-add "traefik.http.services.api_${DEPLOY_COLOR}.loadbalancer.server.port=8080" \
    weave_${DEPLOY_COLOR}_api_${DEPLOY_COLOR}

# 7. 이전 환경 비활성화
log "Disabling ${ACTIVE_COLOR} environment traffic..."
if docker service ls | grep -q "weave_${ACTIVE_COLOR}"; then
    docker service update \
        --label-add "traefik.enable=false" \
        weave_${ACTIVE_COLOR}_api_${ACTIVE_COLOR} 2>/dev/null || true
fi

# 8. 활성 환경 기록
echo "${DEPLOY_COLOR}" > "$ACTIVE_FILE"

# 9. 이전 환경 정리 (선택적, 롤백을 위해 유지할 수도 있음)
log "Keeping ${ACTIVE_COLOR} environment for potential rollback"
log "To cleanup: docker stack rm weave_${ACTIVE_COLOR}"

log "Blue-green deployment completed!"
log "Active environment: ${DEPLOY_COLOR}"
log "Rollback command: ./rollback.sh"
