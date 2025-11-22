#!/bin/bash

# Docker 환경에서 실행되는 ML 재학습 스케줄러
# Cron 또는 주기적 실행을 위한 래퍼 스크립트

echo "=========================================="
echo "ML 자동 재학습 스케줄러 (Docker)"
echo "시작 시간: $(date)"
echo "=========================================="

# 환경 변수 확인
MONGO_URI=${MONGO_URI:-mongodb://mongodb:27017/lovechedule}
RETRAIN_DAYS=${RETRAIN_DAYS:-30}
AUTO_DEPLOY=${AUTO_DEPLOY:-true}

echo ""
echo "설정:"
echo "  MongoDB URI: $MONGO_URI"
echo "  데이터 기간: 최근 ${RETRAIN_DAYS}일"
echo "  자동 배포: $AUTO_DEPLOY"
echo ""

# 로그 디렉토리 생성
mkdir -p /app/logs

# 로그 파일
LOG_FILE="/app/logs/scheduler_$(date +%Y%m%d_%H%M%S).log"

# 파이프라인 실행
if [ "$AUTO_DEPLOY" = "true" ]; then
    python /app/continuous_learning_pipeline.py \
        --mongo-uri "$MONGO_URI" \
        --days "$RETRAIN_DAYS" \
        --auto-deploy 2>&1 | tee "$LOG_FILE"
else
    python /app/continuous_learning_pipeline.py \
        --mongo-uri "$MONGO_URI" \
        --days "$RETRAIN_DAYS" 2>&1 | tee "$LOG_FILE"
fi

EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ 재학습 성공"
else
    echo "❌ 재학습 실패 (종료 코드: $EXIT_CODE)"
fi
echo "종료 시간: $(date)"
echo "로그 파일: $LOG_FILE"
echo "=========================================="

exit $EXIT_CODE
