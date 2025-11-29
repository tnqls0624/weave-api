#!/bin/bash
# 서버 로그 관리 스크립트
# 30일치 로그 보관, 이후 자동 삭제

set -e

# 설정
LOG_DIR="/home/ec2-user/logs"
DOCKER_LOG_DIR="/var/lib/docker/containers"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)

# 로그 함수
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "서버 로그 정리 시작"

# 애플리케이션 로그 디렉토리 생성
mkdir -p "${LOG_DIR}"

# 1. Docker 컨테이너 로그 수집 및 보관
log "Docker 컨테이너 로그 수집 중..."

# API 서비스 로그 수집
API_CONTAINER=$(docker ps --filter "name=api" --format "{{.ID}}" | head -1)
if [ -n "$API_CONTAINER" ]; then
    docker logs --since 24h ${API_CONTAINER} > "${LOG_DIR}/api_${DATE}.log" 2>&1 || true
    log "API 로그 저장: api_${DATE}.log"
fi

# MongoDB 로그 수집
MONGO_CONTAINER=$(docker ps --filter "name=mongodb" --format "{{.ID}}" | head -1)
if [ -n "$MONGO_CONTAINER" ]; then
    docker logs --since 24h ${MONGO_CONTAINER} > "${LOG_DIR}/mongodb_${DATE}.log" 2>&1 || true
    log "MongoDB 로그 저장: mongodb_${DATE}.log"
fi

# Redis 로그 수집
REDIS_CONTAINER=$(docker ps --filter "name=redis" --format "{{.ID}}" | head -1)
if [ -n "$REDIS_CONTAINER" ]; then
    docker logs --since 24h ${REDIS_CONTAINER} > "${LOG_DIR}/redis_${DATE}.log" 2>&1 || true
    log "Redis 로그 저장: redis_${DATE}.log"
fi

# Traefik 로그 수집
TRAEFIK_CONTAINER=$(docker ps --filter "name=traefik" --format "{{.ID}}" | head -1)
if [ -n "$TRAEFIK_CONTAINER" ]; then
    docker logs --since 24h ${TRAEFIK_CONTAINER} > "${LOG_DIR}/traefik_${DATE}.log" 2>&1 || true
    log "Traefik 로그 저장: traefik_${DATE}.log"
fi

# 2. 오늘 로그 압축 (선택적)
cd "${LOG_DIR}"
for logfile in *_${DATE}.log; do
    if [ -f "$logfile" ] && [ -s "$logfile" ]; then
        gzip -f "$logfile" 2>/dev/null || true
    elif [ -f "$logfile" ]; then
        # 빈 로그 파일 삭제
        rm -f "$logfile"
    fi
done

# 3. 30일 이상 된 로그 삭제
log "오래된 로그 정리 중 (${RETENTION_DAYS}일 이상)..."

DELETED_COUNT=0

# 애플리케이션 로그 정리
if [ -d "${LOG_DIR}" ]; then
    DELETED_COUNT=$(find "${LOG_DIR}" -name "*.log*" -type f -mtime +${RETENTION_DAYS} | wc -l)
    find "${LOG_DIR}" -name "*.log*" -type f -mtime +${RETENTION_DAYS} -delete
fi

log "삭제된 로그 파일 수: ${DELETED_COUNT}"

# 4. Docker 로그 truncate (용량 관리)
log "Docker 컨테이너 로그 용량 정리 중..."
if [ -d "${DOCKER_LOG_DIR}" ]; then
    for container_dir in ${DOCKER_LOG_DIR}/*/; do
        if [ -d "$container_dir" ]; then
            for log_file in ${container_dir}*-json.log; do
                if [ -f "$log_file" ]; then
                    LOG_SIZE=$(du -m "$log_file" 2>/dev/null | cut -f1)
                    if [ "${LOG_SIZE:-0}" -gt 100 ]; then
                        # 100MB 초과 시 truncate
                        truncate -s 0 "$log_file" 2>/dev/null || true
                        log "Truncated: $log_file (was ${LOG_SIZE}MB)"
                    fi
                fi
            done
        fi
    done
fi

# 5. 시스템 저널 로그 정리 (systemd 사용 시)
if command -v journalctl &> /dev/null; then
    journalctl --vacuum-time=${RETENTION_DAYS}d 2>/dev/null || true
    log "systemd 저널 정리 완료"
fi

# 6. 현재 로그 디스크 사용량 확인
log "현재 로그 디스크 사용량:"
du -sh "${LOG_DIR}" 2>/dev/null || echo "로그 디렉토리 없음"

# 로그 파일 목록 (최근 10개)
log "최근 로그 파일 (최근 10개):"
ls -lt "${LOG_DIR}"/*.gz 2>/dev/null | head -10 || log "압축된 로그 파일 없음"

log "서버 로그 정리 완료"
