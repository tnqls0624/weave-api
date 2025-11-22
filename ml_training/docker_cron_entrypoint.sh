#!/bin/bash

# Docker 컨테이너 진입점 - Cron 기반 자동 재학습

echo "=========================================="
echo "ML 자동 재학습 서비스 시작"
echo "=========================================="

# 환경 변수 확인
echo "MongoDB URI: ${MONGO_URI:-mongodb://mongodb:27017/lovechedule}"
echo "Cron 스케줄: ${CRON_SCHEDULE:-0 2 * * 0}"
echo ""

# Cron 스케줄 설정 (기본: 매주 일요일 새벽 2시)
CRON_SCHEDULE=${CRON_SCHEDULE:-"0 2 * * 0"}

# 환경 변수를 cron에서 사용할 수 있도록 설정
env | grep -E '^(MONGO_URI|RETRAIN_DAYS|AUTO_DEPLOY)=' > /etc/environment

# Cron 작업 생성
echo "$CRON_SCHEDULE /app/docker_scheduler.sh >> /var/log/cron.log 2>&1" > /etc/cron.d/ml-retraining

# Cron 파일 권한 설정
chmod 0644 /etc/cron.d/ml-retraining

# Cron 작업 등록
crontab /etc/cron.d/ml-retraining

# 로그 파일 생성
touch /var/log/cron.log

echo "✅ Cron 작업 등록 완료"
echo ""
echo "현재 Cron 작업:"
crontab -l
echo ""

# 실행 모드 확인
RUN_MODE=${RUN_MODE:-cron}

if [ "$RUN_MODE" = "once" ]; then
    echo "=========================================="
    echo "일회성 실행 모드"
    echo "=========================================="
    /app/docker_scheduler.sh
elif [ "$RUN_MODE" = "cron" ]; then
    echo "=========================================="
    echo "Cron 모드 - 주기적 자동 실행"
    echo "=========================================="
    echo ""
    echo "다음 실행 예정:"
    echo "  스케줄: $CRON_SCHEDULE"
    echo ""

    # Cron 서비스 시작 및 로그 모니터링
    cron && tail -f /var/log/cron.log
else
    echo "⚠️  알 수 없는 실행 모드: $RUN_MODE"
    echo "RUN_MODE를 'once' 또는 'cron'으로 설정하세요"
    exit 1
fi
