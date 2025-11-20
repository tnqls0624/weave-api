# 피싱 탐지 ML 모델 배포 가이드

## 개요

이 가이드는 학습된 피싱 탐지 ML 모델을 Git을 통해 배포하는 방법을 설명합니다 (옵션 1).

## 배포 방식: Git에 모델 포함

### 장점
- 구현이 간단하고 빠름
- 기존 GitHub Actions 워크플로우 그대로 사용 가능
- Docker 빌드 시 자동으로 모델 포함
- 버전 관리를 통한 모델 이력 추적

### 단점
- Git 저장소 크기 증가 (~20MB)
- 모델 업데이트마다 커밋 필요

---

## 1. 모델 학습 완료 확인

학습이 완료되면 다음 파일들이 생성됩니다:

```
models/phishing_detection_model/
├── saved_model.pb              # TensorFlow 모델
├── variables/
│   ├── variables.index
│   └── variables.data-00000-of-00001
├── vocabulary.json             # 어휘 사전 (Java 서버용)
└── config.json                # 모델 설정

ml_training/
├── phishing_dataset.csv       # 학습 데이터
├── training_history.png       # 학습 그래프
└── best_model.h5              # 최고 성능 모델 (백업)
```

모델 파일 확인:
```bash
ls -lh models/phishing_detection_model/
```

---

## 2. Git 설정

### 2.1 .gitignore 업데이트 (이미 완료)

`.gitignore`에 다음이 추가되었습니다:

```gitignore
### ML Training (학습 관련 임시 파일 제외) ###
ml_training/venv/
ml_training/__pycache__/
ml_training/*.png
ml_training/*.csv
ml_training/*.h5

# 하지만 최종 학습된 모델은 Git에 포함
!models/phishing_detection_model/**
```

### 2.2 모델 파일 커밋

```bash
cd /Users/soobeen/Desktop/Project/lovechedule-api

# 변경된 파일 확인
git status

# 모델 파일 추가
git add models/phishing_detection_model/
git add .gitignore
git add Dockerfile
git add ml_training/train_model.py
git add ml_training/generate_training_data.py
git add ml_training/requirements.txt
git add ml_training/run_training.sh
git add ml_training/README.md
git add PHISHING_ML_SETUP.md
git add ML_DEPLOYMENT_GUIDE.md

# 커밋
git commit -m "feat: Add trained phishing detection ML model

- Add trained LSTM model for Korean SMS phishing detection
- Update Dockerfile to include models directory
- Update .gitignore to include model files but exclude training artifacts
- Increase JVM memory from 256MB to 512MB for ML model loading
- Add ML training scripts and documentation"

# 푸시
git push origin main
```

---

## 3. Dockerfile 변경사항 (이미 완료)

### 3.1 모델 파일 복사

```dockerfile
# Stage 1: Build
COPY src ./src
COPY models ./models  # ← 추가됨
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/models ./models  # ← 추가됨

# 디렉토리 권한 설정
RUN mkdir -p /app/logs /var/log/weave-api /app/keys && \
    chown -R spring:spring /app/logs /var/log/weave-api /app/keys /app/models  # ← 추가됨
```

### 3.2 메모리 설정 증가

```dockerfile
# JVM 옵션 설정 (ML 모델 사용을 위해 메모리 증가)
ENV JAVA_OPTS="\
  -Xms256m \
  -Xmx512m \  # 256MB → 512MB
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/./urandom"
```

---

## 4. 프로덕션 환경 설정

### 4.1 application.yml 업데이트

배포 후 프로덕션 환경에서 ML 모델을 활성화해야 합니다:

**현재 (비활성화):**
```yaml
phishing:
  model:
    enabled: false  # ← 모델 비활성화
    path: ${PHISHING_MODEL_PATH:models/phishing_detection_model}
```

**배포 후 (활성화):**
```yaml
phishing:
  model:
    enabled: true  # ← true로 변경
    path: ${PHISHING_MODEL_PATH:models/phishing_detection_model}
```

### 4.2 환경 변수로 설정 (대안)

GitHub Actions Secrets에 추가:
```
PHISHING_MODEL_ENABLED=true
```

그리고 deploy.yml에서 환경 변수로 전달:
```yaml
- name: Deploy to EC2
  uses: appleboy/ssh-action@v1.0.3
  with:
    script: |
      export PHISHING_MODEL_ENABLED=true
      ./deploy.sh
```

---

## 5. Docker Compose 메모리 설정

`docker-compose.prod.yml`에서 API 서비스의 메모리 제한 확인:

```yaml
services:
  api:
    image: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G  # ← 최소 1GB 권장 (ML 모델 + JVM)
        reservations:
          memory: 512M
```

---

## 6. 배포 프로세스

### 6.1 자동 배포 (GitHub Actions)

main 브랜치에 푸시하면 자동으로 배포됩니다:

```bash
git push origin main
```

GitHub Actions가 다음을 수행합니다:
1. Docker 이미지 빌드 (모델 파일 포함)
2. ECR에 푸시
3. EC2에 배포 스크립트 업로드
4. Docker Swarm으로 서비스 업데이트

### 6.2 배포 확인

```bash
# EC2에 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-host

# 서비스 상태 확인
docker service ls

# API 서비스 로그 확인
docker service logs weave_api --tail 100 --follow

# 모델 로드 확인 (로그에서 다음 메시지 확인)
# "피싱 ML 모델 초기화 완료"
```

---

## 7. 테스트

### 7.1 API 테스트

```bash
curl -X POST http://your-server/api/phishing/detect \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "sender": "01012345678",
    "message": "긴급! 국민은행입니다. 계좌가 정지되었습니다. http://bit.ly/abc123에서 확인하세요.",
    "sensitivityLevel": "medium"
  }'
```

**예상 응답:**
```json
{
  "isPhishing": true,
  "riskLevel": "high",
  "riskScore": 0.95,
  "confidence": 0.92,
  "detectionReasons": [
    "AI 모델 피싱 판정",
    "의심스러운 URL 발견: http://bit.ly/abc123",
    "긴급성 단어 발견: 긴급",
    "금융 기관 사칭"
  ]
}
```

### 7.2 모델 성능 확인

서비스 로그에서 ML 모델 초기화 메시지 확인:
```
피싱 탐지 ML 모델 초기화 완료
모델 경로: models/phishing_detection_model
어휘 크기: 256
```

---

## 8. 모니터링

### 8.1 메모리 사용량 확인

```bash
# Docker 서비스 메모리 사용량
docker stats $(docker ps -q -f name=weave_api)
```

### 8.2 DJL 네이티브 라이브러리 다운로드

**중요**: 첫 실행 시 DJL이 TensorFlow 네이티브 라이브러리(~100-200MB)를 자동으로 다운로드합니다.

- 다운로드 위치: `/tmp/.djl.ai/` (컨테이너 내부)
- 다운로드 시간: 1-3분 (네트워크 속도에 따라)
- 재시작 시: 이미 다운로드되어 있으면 재사용

**로그 예시:**
```
Downloading TensorFlow native library from https://publish.djl.ai/...
Downloaded 157.3 MB in 45s
```

---

## 9. 문제 해결

### 9.1 모델 파일을 찾을 수 없음

**에러:**
```
파일을 찾을 수 없습니다: models/phishing_detection_model/saved_model.pb
```

**해결:**
1. Docker 이미지에 모델이 포함되었는지 확인:
   ```bash
   docker run --rm weave-api ls -la /app/models/phishing_detection_model/
   ```
2. Dockerfile에 `COPY models ./models` 라인 확인

### 9.2 메모리 부족 (OOM)

**에러:**
```
java.lang.OutOfMemoryError: Java heap space
```

**해결:**
1. Dockerfile에서 JVM 메모리 증가:
   ```dockerfile
   ENV JAVA_OPTS="-Xms256m -Xmx1024m ..."
   ```
2. Docker Compose에서 컨테이너 메모리 증가:
   ```yaml
   resources:
     limits:
       memory: 2G
   ```

### 9.3 DJL 다운로드 실패

**에러:**
```
Failed to download TensorFlow native library
```

**해결:**
1. 인터넷 연결 확인
2. 프록시 설정 (필요한 경우):
   ```dockerfile
   ENV HTTP_PROXY=http://proxy:port
   ENV HTTPS_PROXY=http://proxy:port
   ```

---

## 10. 모델 업데이트

새 모델을 학습하고 배포하는 경우:

```bash
# 1. 새 모델 학습
cd ml_training
./run_training.sh

# 2. 모델 성능 확인
# training_history.png 확인

# 3. Git 커밋
cd ..
git add models/phishing_detection_model/
git commit -m "feat: Update phishing detection model

- Accuracy: 95.2% → 96.1%
- Precision: 93.5% → 94.8%
- Recall: 96.8% → 97.2%
- Trained with 15,000 samples"

# 4. 배포
git push origin main
```

---

## 11. 향후 개선 사항

현재는 Git에 모델을 포함하지만, 나중에 다음과 같이 개선할 수 있습니다:

### 옵션 2: S3 저장 (장기 권장)

모델 크기가 커지거나 업데이트가 잦아지면 S3로 마이그레이션:

1. **모델을 S3에 업로드:**
   ```bash
   aws s3 sync models/phishing_detection_model/ \
     s3://your-bucket/models/phishing_detection_model/
   ```

2. **애플리케이션 시작 시 다운로드:**
   ```java
   @PostConstruct
   public void initialize() {
       downloadModelFromS3();  // S3에서 다운로드
       loadModel();            // 모델 로드
   }
   ```

3. **.gitignore에 추가:**
   ```gitignore
   models/
   ```

---

## 요약 체크리스트

- [ ] 모델 학습 완료 (`models/phishing_detection_model/` 확인)
- [ ] `.gitignore` 업데이트 완료
- [ ] Dockerfile 수정 완료 (모델 복사, 메모리 증가)
- [ ] 모델 파일 Git에 커밋 & 푸시
- [ ] GitHub Actions 배포 성공 확인
- [ ] `application.yml`에서 `phishing.model.enabled: true` 설정
- [ ] 서비스 로그에서 모델 초기화 메시지 확인
- [ ] API 테스트 (피싱 메시지로 테스트)
- [ ] 메모리 사용량 모니터링

---

**문의사항**: [GitHub Issues](https://github.com/your-repo/issues)
