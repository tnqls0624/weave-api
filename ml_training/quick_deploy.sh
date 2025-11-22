#!/bin/bash

# ML 파이프라인 빠른 배포 스크립트
# Docker Compose를 통한 자동 재학습 시스템 시작

set -e

echo "=========================================="
echo "ML 파이프라인 Docker 배포"
echo "=========================================="

# 현재 디렉토리 확인
if [ ! -f "docker-compose.prod.yml" ]; then
    echo "❌ docker-compose.prod.yml 파일을 찾을 수 없습니다"
    echo "프로젝트 루트 디렉토리에서 실행하세요"
    exit 1
fi

# 환경 변수 파일 확인
if [ ! -f ".env" ]; then
    echo ""
    echo "⚠️  .env 파일이 없습니다"
    echo "환경 변수 파일을 생성하시겠습니까? (y/n): "
    read -r create_env

    if [ "$create_env" = "y" ]; then
        if [ -f ".env.ml.example" ]; then
            cp .env.ml.example .env
            echo "✅ .env 파일 생성 완료"
            echo ""
            echo "⚠️  .env 파일을 편집하여 MongoDB URI 등을 설정하세요:"
            echo "   vim .env"
            echo ""
            echo "계속하시겠습니까? (y/n): "
            read -r continue
            if [ "$continue" != "y" ]; then
                exit 0
            fi
        else
            echo "❌ .env.ml.example 파일을 찾을 수 없습니다"
            exit 1
        fi
    else
        echo "종료합니다"
        exit 0
    fi
fi

# 배포 모드 선택
echo ""
echo "=========================================="
echo "배포 모드 선택"
echo "=========================================="
echo ""
echo "1) Cron 모드 - 자동 스케줄링 (권장)"
echo "   매주 일요일 새벽 2시 자동 재학습"
echo ""
echo "2) 일회성 실행 - 즉시 재학습"
echo "   한 번 실행 후 종료"
echo ""
echo "3) 전체 스택 배포 - API + MongoDB + ML Trainer"
echo "   모든 서비스 시작"
echo ""
read -p "선택 (1-3): " deploy_mode

case $deploy_mode in
    1)
        echo ""
        echo "=========================================="
        echo "Cron 모드 배포"
        echo "=========================================="

        # 스케줄 선택
        echo ""
        echo "재학습 주기를 선택하세요:"
        echo "1) 매주 일요일 새벽 2시 (권장)"
        echo "2) 매일 새벽 2시"
        echo "3) 매월 1일 새벽 2시"
        echo ""
        read -p "선택 (1-3): " schedule_choice

        case $schedule_choice in
            1)
                CRON_SCHEDULE="0 2 * * 0"
                ;;
            2)
                CRON_SCHEDULE="0 2 * * *"
                ;;
            3)
                CRON_SCHEDULE="0 2 1 * *"
                ;;
            *)
                echo "잘못된 선택입니다"
                exit 1
                ;;
        esac

        echo ""
        echo "ML Trainer 시작 중..."
        ML_RUN_MODE=cron ML_CRON_SCHEDULE="$CRON_SCHEDULE" \
            docker compose -f docker-compose.prod.yml up -d ml-trainer

        echo ""
        echo "✅ 배포 완료!"
        echo ""
        echo "📝 로그 확인:"
        echo "   docker compose -f docker-compose.prod.yml logs -f ml-trainer"
        ;;

    2)
        echo ""
        echo "=========================================="
        echo "일회성 실행"
        echo "=========================================="
        echo ""
        echo "ML Trainer 실행 중..."
        ML_RUN_MODE=once \
            docker compose -f docker-compose.prod.yml run --rm ml-trainer

        echo ""
        echo "✅ 실행 완료!"
        ;;

    3)
        echo ""
        echo "=========================================="
        echo "전체 스택 배포"
        echo "=========================================="
        echo ""
        echo "모든 서비스 시작 중..."
        docker compose -f docker-compose.prod.yml up -d

        echo ""
        echo "✅ 배포 완료!"
        echo ""
        echo "📊 서비스 상태:"
        docker compose -f docker-compose.prod.yml ps
        echo ""
        echo "📝 ML Trainer 로그:"
        echo "   docker compose -f docker-compose.prod.yml logs -f ml-trainer"
        ;;

    *)
        echo "잘못된 선택입니다"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo "배포 정보"
echo "=========================================="
echo ""
echo "🔍 서비스 상태 확인:"
echo "   docker compose -f docker-compose.prod.yml ps"
echo ""
echo "📝 로그 확인:"
echo "   docker compose -f docker-compose.prod.yml logs -f ml-trainer"
echo ""
echo "🛑 중지:"
echo "   docker compose -f docker-compose.prod.yml stop ml-trainer"
echo ""
echo "🔄 재시작:"
echo "   docker compose -f docker-compose.prod.yml restart ml-trainer"
echo ""
echo "📚 상세 가이드:"
echo "   cat DOCKER_ML_DEPLOYMENT.md"
echo ""
