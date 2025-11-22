#!/bin/bash

# 자동 재학습 스케줄러 설정
# Cron을 사용하여 주기적으로 모델 재학습 실행

echo "=========================================="
echo "ML 모델 자동 재학습 스케줄러 설정"
echo "=========================================="

# 현재 디렉토리 저장
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo ""
echo "프로젝트 디렉토리: $PROJECT_DIR"
echo "ML 디렉토리: $SCRIPT_DIR"

# Python 가상환경 확인
if [ ! -d "$SCRIPT_DIR/venv" ]; then
    echo ""
    echo "⚠️ Python 가상환경이 없습니다."
    echo "가상환경을 생성하시겠습니까? (y/n): "
    read -r create_venv

    if [ "$create_venv" = "y" ]; then
        echo "가상환경 생성 중..."
        python3 -m venv "$SCRIPT_DIR/venv"
        source "$SCRIPT_DIR/venv/bin/activate"

        echo "필수 패키지 설치 중..."
        pip install -r "$SCRIPT_DIR/requirements.txt"

        echo "✅ 가상환경 설정 완료"
    else
        echo "가상환경이 필요합니다. 종료합니다."
        exit 1
    fi
fi

# Cron 작업 생성
echo ""
echo "=========================================="
echo "Cron 작업 설정"
echo "=========================================="
echo ""
echo "재학습 주기를 선택하세요:"
echo "1) 매주 일요일 새벽 2시 (권장)"
echo "2) 매일 새벽 2시"
echo "3) 매월 1일 새벽 2시"
echo "4) 사용자 정의"
echo ""
read -p "선택 (1-4): " schedule_choice

case $schedule_choice in
    1)
        CRON_SCHEDULE="0 2 * * 0"
        DESCRIPTION="매주 일요일 새벽 2시"
        ;;
    2)
        CRON_SCHEDULE="0 2 * * *"
        DESCRIPTION="매일 새벽 2시"
        ;;
    3)
        CRON_SCHEDULE="0 2 1 * *"
        DESCRIPTION="매월 1일 새벽 2시"
        ;;
    4)
        echo "Cron 표현식을 입력하세요 (예: 0 2 * * 0):"
        read -r CRON_SCHEDULE
        DESCRIPTION="사용자 정의 스케줄"
        ;;
    *)
        echo "잘못된 선택입니다."
        exit 1
        ;;
esac

# MongoDB URI 입력
echo ""
read -p "MongoDB URI를 입력하세요 [mongodb://localhost:27017/lovechedule]: " MONGO_URI
MONGO_URI=${MONGO_URI:-mongodb://localhost:27017/lovechedule}

# Cron 작업 스크립트 생성
CRON_SCRIPT="$SCRIPT_DIR/run_retraining.sh"

cat > "$CRON_SCRIPT" << EOF
#!/bin/bash
# ML 모델 자동 재학습 스크립트
# 생성일: $(date)

# 디렉토리 이동
cd "$SCRIPT_DIR"

# 가상환경 활성화
source venv/bin/activate

# 로그 파일
LOG_FILE="logs/cron_\$(date +%Y%m%d_%H%M%S).log"
mkdir -p logs

# 파이프라인 실행
echo "=========================================" >> "\$LOG_FILE"
echo "자동 재학습 시작: \$(date)" >> "\$LOG_FILE"
echo "=========================================" >> "\$LOG_FILE"

python continuous_learning_pipeline.py \\
    --mongo-uri "$MONGO_URI" \\
    --days 30 \\
    --auto-deploy >> "\$LOG_FILE" 2>&1

EXIT_CODE=\$?

echo "" >> "\$LOG_FILE"
echo "종료 코드: \$EXIT_CODE" >> "\$LOG_FILE"
echo "종료 시간: \$(date)" >> "\$LOG_FILE"

# 결과 알림 (선택사항)
if [ \$EXIT_CODE -eq 0 ]; then
    echo "✅ 자동 재학습 성공" >> "\$LOG_FILE"
else
    echo "❌ 자동 재학습 실패" >> "\$LOG_FILE"
fi

# 가상환경 비활성화
deactivate
EOF

# 실행 권한 부여
chmod +x "$CRON_SCRIPT"

echo ""
echo "✅ 재학습 스크립트 생성 완료: $CRON_SCRIPT"

# Cron 작업 등록
echo ""
echo "=========================================="
echo "Cron 작업 등록"
echo "=========================================="
echo ""
echo "스케줄: $DESCRIPTION ($CRON_SCHEDULE)"
echo ""
echo "다음 명령을 실행하여 cron에 등록하시겠습니까? (y/n): "
read -r register_cron

if [ "$register_cron" = "y" ]; then
    # 기존 cron 작업 백업
    crontab -l > "$SCRIPT_DIR/crontab_backup_$(date +%Y%m%d_%H%M%S).txt" 2>/dev/null

    # 새 cron 작업 추가
    (crontab -l 2>/dev/null; echo "# ML 모델 자동 재학습 - $DESCRIPTION"; echo "$CRON_SCHEDULE $CRON_SCRIPT") | crontab -

    echo ""
    echo "✅ Cron 작업 등록 완료"
    echo ""
    echo "현재 cron 작업 목록:"
    crontab -l
else
    echo ""
    echo "수동으로 cron에 등록하려면 다음 명령을 실행하세요:"
    echo "  crontab -e"
    echo ""
    echo "그리고 다음 라인을 추가하세요:"
    echo "  $CRON_SCHEDULE $CRON_SCRIPT"
fi

echo ""
echo "=========================================="
echo "설정 완료!"
echo "=========================================="
echo ""
echo "📝 요약:"
echo "  - 스케줄: $DESCRIPTION"
echo "  - 스크립트: $CRON_SCRIPT"
echo "  - MongoDB: $MONGO_URI"
echo ""
echo "🔍 로그 확인:"
echo "  ls -lht $SCRIPT_DIR/logs/"
echo ""
echo "🧪 수동 테스트:"
echo "  $CRON_SCRIPT"
echo ""
echo "🗑️  Cron 작업 삭제:"
echo "  crontab -e  # 해당 라인 삭제"
echo ""
