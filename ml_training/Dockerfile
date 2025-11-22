# ML 학습 및 자동화 파이프라인 Dockerfile
FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필수 도구 설치
RUN apt-get update && apt-get install -y \
    cron \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python 패키지 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 학습 스크립트 복사
COPY . .

# 디렉토리 생성
RUN mkdir -p logs backups

# 실행 권한 부여
RUN chmod +x docker_scheduler.sh docker_cron_entrypoint.sh

# 환경 변수 설정
ENV PYTHONUNBUFFERED=1
ENV TZ=Asia/Seoul
ENV MONGO_URI=mongodb://mongodb:27017/lovechedule
ENV RETRAIN_DAYS=30
ENV AUTO_DEPLOY=true
ENV RUN_MODE=cron
ENV CRON_SCHEDULE="0 2 * * 0"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD python -c "import sys; sys.exit(0)"

# 진입점 설정
ENTRYPOINT ["/app/docker_cron_entrypoint.sh"]
