#!/bin/bash
# Cron 설정 스크립트
# EC2 서버에서 실행하여 백업 및 로그 관리 cron 등록

set -e

SCRIPT_DIR="/home/ec2-user/scripts"

# 로그 함수
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Cron 설정 시작"

# 스크립트 디렉토리 생성
mkdir -p "${SCRIPT_DIR}"
mkdir -p /home/ec2-user/backups/mongodb
mkdir -p /home/ec2-user/logs

# 현재 디렉토리의 스크립트를 서버 스크립트 디렉토리로 복사
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cp "${CURRENT_DIR}/mongodb-backup.sh" "${SCRIPT_DIR}/"
cp "${CURRENT_DIR}/cleanup-logs.sh" "${SCRIPT_DIR}/"

# 실행 권한 부여
chmod +x "${SCRIPT_DIR}/mongodb-backup.sh"
chmod +x "${SCRIPT_DIR}/cleanup-logs.sh"

log "스크립트 복사 및 권한 설정 완료"

# 기존 cron 백업
crontab -l > /tmp/crontab_backup 2>/dev/null || true

# 기존 백업/로그 관련 cron 제거 후 새로 추가
(crontab -l 2>/dev/null | grep -v "mongodb-backup.sh" | grep -v "cleanup-logs.sh") > /tmp/crontab_new || true

# 새로운 cron 작업 추가
cat >> /tmp/crontab_new << 'EOF'

# MongoDB 일일 백업 (매일 새벽 3시, 7일 보관)
0 3 * * * /home/ec2-user/scripts/mongodb-backup.sh >> /home/ec2-user/logs/cron-backup.log 2>&1

# 서버 로그 정리 (매일 새벽 4시, 30일 보관)
0 4 * * * /home/ec2-user/scripts/cleanup-logs.sh >> /home/ec2-user/logs/cron-cleanup.log 2>&1
EOF

# 새 crontab 적용
crontab /tmp/crontab_new
rm /tmp/crontab_new

log "Cron 설정 완료"

# 현재 crontab 확인
log "현재 crontab 설정:"
crontab -l

log "설정 완료!"
echo ""
echo "=== 설정 요약 ==="
echo "1. MongoDB 백업: 매일 새벽 3시 실행, 7일 보관"
echo "2. 로그 정리: 매일 새벽 4시 실행, 30일 보관"
echo ""
echo "백업 위치: /home/ec2-user/backups/mongodb/"
echo "로그 위치: /home/ec2-user/logs/"
echo ""
echo "수동 테스트:"
echo "  ${SCRIPT_DIR}/mongodb-backup.sh"
echo "  ${SCRIPT_DIR}/cleanup-logs.sh"
