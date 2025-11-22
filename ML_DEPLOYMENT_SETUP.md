# ML 서비스 배포 설정 가이드

## 개요
이 문서는 Python ML 서비스(Trainer, Inference)를 ECR을 통해 배포하는 방법을 설명합니다.

## 배포 아키텍처

### 변경 사항
**이전**: EC2에서 소스 코드로 빌드 (느리고 비효율적)
**현재**: GitHub Actions에서 빌드 → ECR 푸시 → EC2에서 Pull (빠르고 일관성 있음)

### 배포 흐름
```
┌──────────────────────────────────────────────────────────────┐
│  GitHub Actions (main 브랜치 푸시)                            │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  1. Java API 이미지 빌드 및 ECR 푸시                           │
│     - weave-api/prod:${GITHUB_SHA}                             │
│     - weave-api/prod:latest                                    │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  2. ML Trainer 이미지 빌드 및 ECR 푸시                        │
│     - weave-ml-trainer:${GITHUB_SHA}                           │
│     - weave-ml-trainer:latest                                  │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  3. ML Inference 이미지 빌드 및 ECR 푸시                      │
│     - weave-ml-inference:${GITHUB_SHA}                         │
│     - weave-ml-inference:latest                                │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  4. docker-compose.yml 및 .env 파일 생성                      │
│     - ML_TRAINER_IMAGE 환경 변수 설정                          │
│     - ML_INFERENCE_IMAGE 환경 변수 설정                        │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  5. EC2로 배포 파일 업로드 (SCP)                              │
└────────────────────┬───────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  6. EC2에서 배포 스크립트 실행 (SSH)                          │
│     - ECR 로그인                                               │
│     - 이미지 Pull                                              │
│     - Docker Swarm Stack 배포                                  │
└────────────────────────────────────────────────────────────────┘
```

## GitHub Secrets 설정

### 필수 Secrets 추가

GitHub 리포지토리 → Settings → Secrets and variables → Actions에서 다음을 추가하세요:

#### ML 서비스 관련 Secrets

```bash
# Python ML 추론 서버 설정
PROD_PHISHING_ML_INFERENCE_URL=http://ml-inference:8000
PROD_PHISHING_ML_INFERENCE_ENABLED=true

# ML 재학습 설정
PROD_ML_RETRAIN_DAYS=30
PROD_ML_AUTO_DEPLOY=true
PROD_ML_RUN_MODE=cron
PROD_ML_CRON_SCHEDULE=0 2 * * 0
```

#### 기존 Secrets (이미 있어야 함)
```bash
AWS_ACCOUNT_ID=your-aws-account-id
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

PROD_EC2_HOST=your-ec2-ip
PROD_EC2_USER=ec2-user
PROD_EC2_SSH_KEY=your-ssh-private-key

PROD_SPRING_DATA_MONGODB_URI=mongodb://...
PROD_MONGODB_ROOT_PASSWORD=...
PROD_REDIS_PASSWORD=...
# ... 기타 기존 secrets
```

## ECR 리포지토리 생성

AWS ECR에 다음 리포지토리들을 생성해야 합니다:

### AWS Console 또는 AWS CLI로 생성

```bash
# 1. Java API 리포지토리 (이미 있을 것)
aws ecr create-repository \
  --repository-name weave-api/prod \
  --region ap-northeast-2

# 2. ML Trainer 리포지토리 (새로 생성)
aws ecr create-repository \
  --repository-name weave-ml-trainer \
  --region ap-northeast-2

# 3. ML Inference 리포지토리 (새로 생성)
aws ecr create-repository \
  --repository-name weave-ml-inference \
  --region ap-northeast-2
```

### 리포지토리 생명주기 정책 (선택사항)

오래된 이미지 자동 삭제를 위한 정책:

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 10 images",
      "selection": {
        "tagStatus": "any",
        "countType": "imageCountMoreThan",
        "countNumber": 10
      },
      "action": {
        "type": "expire"
      }
    }
  ]
}
```

적용:
```bash
aws ecr put-lifecycle-policy \
  --repository-name weave-ml-trainer \
  --lifecycle-policy-text file://lifecycle-policy.json \
  --region ap-northeast-2

aws ecr put-lifecycle-policy \
  --repository-name weave-ml-inference \
  --lifecycle-policy-text file://lifecycle-policy.json \
  --region ap-northeast-2
```

## 배포 환경 변수

### docker-compose.prod.yml에서 사용되는 변수

```yaml
# ML Trainer 이미지
ML_TRAINER_IMAGE: ${ECR_REGISTRY}/weave-ml-trainer:${IMAGE_TAG}

# ML Inference 이미지
ML_INFERENCE_IMAGE: ${ECR_REGISTRY}/weave-ml-inference:${IMAGE_TAG}

# ML 추론 서버 설정
PHISHING_ML_INFERENCE_URL: http://ml-inference:8000
PHISHING_ML_INFERENCE_ENABLED: true

# ML 재학습 설정
ML_RETRAIN_DAYS: 30          # 최근 N일 데이터 사용
ML_AUTO_DEPLOY: true         # 학습 후 자동 배포
ML_RUN_MODE: cron            # cron | once
ML_CRON_SCHEDULE: 0 2 * * 0  # 매주 일요일 새벽 2시
```

## 배포 프로세스

### 1. 코드 푸시
```bash
git add .
git commit -m "feat: Update ML model"
git push origin main
```

### 2. GitHub Actions 자동 실행
- Java API 이미지 빌드 (~3-5분)
- ML Trainer 이미지 빌드 (~2-3분)
- ML Inference 이미지 빌드 (~2-3분)
- ECR에 푸시
- EC2로 배포 파일 전송
- EC2에서 배포 실행

### 3. 배포 확인
```bash
# EC2 SSH 접속
ssh -i your-key.pem ec2-user@your-ec2-ip

# 서비스 상태 확인
docker service ls

# 출력 예시:
# ID        NAME                MODE        REPLICAS
# ...       weave_api           replicated  1/1
# ...       weave_ml-trainer    replicated  1/1
# ...       weave_ml-inference  replicated  1/1

# ML Inference 서버 로그 확인
docker service logs -f weave_ml-inference

# ML Trainer 로그 확인
docker service logs -f weave_ml-trainer
```

### 4. 헬스체크
```bash
# ML Inference 서버 헬스체크
curl http://localhost:8000/health

# 응답 예시:
{
  "status": "healthy",
  "model_loaded": true,
  "model_path": "/app/models/phishing_detection_model",
  "vocabulary_size": 10000
}
```

## 로컬 테스트

배포 전 로컬에서 테스트:

```bash
# 1. .env 파일 생성
cat > .env << EOF
ECR_REGISTRY=your-account.dkr.ecr.ap-northeast-2.amazonaws.com
ML_TRAINER_IMAGE=weave-ml-trainer:latest
ML_INFERENCE_IMAGE=weave-ml-inference:latest
MONGODB_URI=mongodb://root:1234@mongodb:27017/weave?authSource=admin
PHISHING_ML_INFERENCE_ENABLED=true
ML_RUN_MODE=once
EOF

# 2. ML 이미지 빌드
docker build -t weave-ml-trainer:latest ./ml_training
docker build -t weave-ml-inference:latest ./ml_inference

# 3. Docker Compose로 실행
docker-compose -f docker-compose.prod.yml up -d ml-inference

# 4. 테스트
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "010-1234-5678",
    "message": "긴급! 계좌번호 확인 필요",
    "sensitivity_level": "medium"
  }'
```

## 트러블슈팅

### 1. ECR 로그인 실패
```bash
# 수동 ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  your-account.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 2. 이미지 Pull 실패
```bash
# EC2에서 ECR 권한 확인
aws ecr describe-repositories --region ap-northeast-2

# EC2 IAM 역할에 ECR 권한 추가 필요
# AmazonEC2ContainerRegistryReadOnly 정책 연결
```

### 3. ML 서비스가 시작되지 않음
```bash
# 서비스 로그 확인
docker service logs weave_ml-inference
docker service logs weave_ml-trainer

# 일반적인 원인:
# - 모델 파일이 없음 (ml_models 볼륨 확인)
# - 메모리 부족 (리소스 제한 확인)
# - 환경 변수 누락 (.env 파일 확인)
```

### 4. 모델 파일이 없음
```bash
# 초기 모델 파일 업로드
# EC2에서:
docker volume inspect weave_ml_models

# 볼륨 위치 확인 후 모델 파일 복사
sudo cp -r /path/to/trained/model/* \
  /var/lib/docker/volumes/weave_ml_models/_data/
```

## 모니터링

### 리소스 사용량 확인
```bash
# 전체 서비스 리소스
docker stats

# ML Inference 서버만
docker stats $(docker ps -q -f name=weave_ml-inference)
```

### 추론 성능 모니터링
```bash
# ML Inference 로그에서 응답 시간 확인
docker service logs weave_ml-inference | grep "예측 완료"

# Prometheus 메트릭 (향후 추가 예정)
curl http://localhost:8000/metrics
```

## 업데이트 및 롤백

### 새 모델 배포
```bash
# 1. 새 모델 학습 (ML Trainer가 자동으로 수행)
# 2. 배포는 자동으로 이루어짐 (ML_AUTO_DEPLOY=true인 경우)

# 수동 배포
docker service update --force weave_ml-inference
```

### 롤백
```bash
# 이전 이미지로 롤백
docker service update \
  --image your-account.dkr.ecr.ap-northeast-2.amazonaws.com/weave-ml-inference:previous-sha \
  weave_ml-inference

# 또는 latest 태그로
docker service update \
  --image your-account.dkr.ecr.ap-northeast-2.amazonaws.com/weave-ml-inference:latest \
  weave_ml-inference
```

## 보안 고려사항

### ECR 이미지 스캔
```bash
# 이미지 취약점 스캔 활성화
aws ecr put-image-scanning-configuration \
  --repository-name weave-ml-inference \
  --image-scanning-configuration scanOnPush=true \
  --region ap-northeast-2
```

### 환경 변수 보안
- 민감한 정보는 GitHub Secrets에 저장
- EC2에서 .env 파일 권한 제한: `chmod 600 .env`
- MongoDB URI, Redis 비밀번호 등은 절대 코드에 포함 금지

## 비용 최적화

### ECR 스토리지
- 오래된 이미지 자동 삭제 (생명주기 정책)
- latest + 최근 10개 이미지만 유지

### EC2 리소스
- ML Trainer: 필요시만 실행 (cron 스케줄)
- ML Inference: 최소 리소스로 시작, 부하에 따라 스케일링

### Docker 이미지 최적화
- Multi-stage build 사용
- 불필요한 의존성 제거
- Alpine 기반 이미지 사용 고려

## 다음 단계

1. **모니터링 대시보드**: Grafana + Prometheus 통합
2. **자동 스케일링**: 부하에 따른 ML Inference 서비스 자동 확장
3. **A/B 테스트**: 여러 모델 버전 동시 운영
4. **캐싱**: Redis를 활용한 추론 결과 캐싱
5. **배치 추론**: 여러 메시지 한 번에 처리

## 참고 자료

- [AWS ECR 문서](https://docs.aws.amazon.com/ecr/)
- [Docker Swarm 문서](https://docs.docker.com/engine/swarm/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [FastAPI 문서](https://fastapi.tiangolo.com/)
