# ML 추론 서버 Dockerfile
FROM python:3.11-slim

# 작업 디렉토리
WORKDIR /app

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python 패키지 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY inference_server.py .

# 환경 변수
ENV PYTHONUNBUFFERED=1
ENV MODEL_PATH=/app/models/phishing_detection_model
ENV PORT=8000
ENV HOST=0.0.0.0

# 포트 노출
EXPOSE 8000

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

# 서버 실행
CMD ["python", "inference_server.py"]
