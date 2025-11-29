#!/bin/bash
# Rolling Deployment Strategy
# 기존 서비스를 점진적으로 새 버전으로 교체

set -e

# 환경변수 확인
: "${ECR_REGISTRY:?ECR_REGISTRY is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"

FULL_IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
SERVICE_NAME="weave_api"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ROLLING] $1"
}

log "Starting rolling deployment..."
log "Image: ${FULL_IMAGE}"

# 1. 현재 상태 저장 (롤백용)
PREVIOUS_IMAGE=$(docker service inspect ${SERVICE_NAME} --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' 2>/dev/null || echo "none")
echo "${PREVIOUS_IMAGE}" > /tmp/previous_image.txt
log "Previous image saved: ${PREVIOUS_IMAGE}"

# 2. 새 이미지 Pull
log "Pulling new image..."
docker pull ${FULL_IMAGE}

# 3. Docker Stack 배포 (Rolling Update)
log "Deploying with rolling update strategy..."
cd ~/deployment

docker stack deploy \
    -c docker-compose.yml \
    weave \
    --with-registry-auth \
    --prune

# 4. 배포 진행 상황 모니터링
log "Monitoring deployment progress..."
TIMEOUT=300  # 5분
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
    # 서비스 상태 확인
    RUNNING=$(docker service ps ${SERVICE_NAME} --filter "desired-state=running" --format "{{.CurrentState}}" | grep -c "Running" || echo "0")
    TOTAL=$(docker service inspect ${SERVICE_NAME} --format '{{.Spec.Mode.Replicated.Replicas}}' 2>/dev/null || echo "1")

    log "Running: ${RUNNING}/${TOTAL}"

    if [ "$RUNNING" -ge "$TOTAL" ]; then
        # 새 이미지로 교체 완료 확인
        CURRENT_IMAGE=$(docker service inspect ${SERVICE_NAME} --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}')
        if [ "$CURRENT_IMAGE" == "$FULL_IMAGE" ]; then
            log "Rolling deployment completed successfully!"
            exit 0
        fi
    fi

    sleep 10
    ELAPSED=$((ELAPSED + 10))
done

log "ERROR: Deployment timeout after ${TIMEOUT} seconds"
exit 1
