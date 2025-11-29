#!/bin/bash
# MongoDB 일일 백업 스크립트
# 7일치 백업 보관, 이후 자동 삭제

set -e

# 설정
BACKUP_DIR="/home/ec2-user/backups/mongodb"
RETENTION_DAYS=7
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="mongodb_backup_${DATE}"

# MongoDB 접속 정보 (환경변수 또는 기본값)
MONGO_HOST="${MONGO_HOST:-mongodb}"
MONGO_USER="${MONGO_USER:-root}"
MONGO_PASSWORD="${MONGO_PASSWORD:-$MONGODB_ROOT_PASSWORD}"
MONGO_DB="${MONGO_DB:-lovechedule}"

# 로그 함수
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "MongoDB 백업 시작: ${BACKUP_NAME}"

# 백업 디렉토리 생성
mkdir -p "${BACKUP_DIR}"

# Docker Swarm에서 MongoDB 컨테이너 찾기
MONGO_CONTAINER=$(docker ps --filter "name=mongodb" --format "{{.ID}}" | head -1)

if [ -z "$MONGO_CONTAINER" ]; then
    log "ERROR: MongoDB 컨테이너를 찾을 수 없습니다"
    exit 1
fi

log "MongoDB 컨테이너: ${MONGO_CONTAINER}"

# mongodump 실행 (컨테이너 내부에서)
docker exec ${MONGO_CONTAINER} mongodump \
    --username="${MONGO_USER}" \
    --password="${MONGO_PASSWORD}" \
    --authenticationDatabase=admin \
    --db="${MONGO_DB}" \
    --out="/tmp/${BACKUP_NAME}" \
    --quiet

# 백업 파일을 호스트로 복사
docker cp "${MONGO_CONTAINER}:/tmp/${BACKUP_NAME}" "${BACKUP_DIR}/"

# 컨테이너 내 임시 파일 삭제
docker exec ${MONGO_CONTAINER} rm -rf "/tmp/${BACKUP_NAME}"

# 백업 압축
cd "${BACKUP_DIR}"
tar -czf "${BACKUP_NAME}.tar.gz" "${BACKUP_NAME}"
rm -rf "${BACKUP_NAME}"

log "백업 완료: ${BACKUP_DIR}/${BACKUP_NAME}.tar.gz"

# 백업 크기 확인
BACKUP_SIZE=$(du -h "${BACKUP_DIR}/${BACKUP_NAME}.tar.gz" | cut -f1)
log "백업 크기: ${BACKUP_SIZE}"

# 7일 이상 된 백업 삭제
log "오래된 백업 정리 중 (${RETENTION_DAYS}일 이상)..."
DELETED_COUNT=$(find "${BACKUP_DIR}" -name "mongodb_backup_*.tar.gz" -type f -mtime +${RETENTION_DAYS} | wc -l)
find "${BACKUP_DIR}" -name "mongodb_backup_*.tar.gz" -type f -mtime +${RETENTION_DAYS} -delete

log "삭제된 백업 수: ${DELETED_COUNT}"

# 현재 백업 목록 출력
log "현재 백업 목록:"
ls -lh "${BACKUP_DIR}"/*.tar.gz 2>/dev/null || log "백업 파일 없음"

log "MongoDB 백업 완료"
