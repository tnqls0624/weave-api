# 지속적 학습 (Continuous Learning) 가이드

ML 모델을 프로덕션 데이터로 지속적으로 개선하는 방법

---

## 📋 목차

1. [개요](#개요)
2. [학습 데이터 수집](#학습-데이터-수집)
3. [수동 재학습](#수동-재학습)
4. [자동 재학습 설정](#자동-재학습-설정)
5. [모델 성능 모니터링](#모델-성능-모니터링)
6. [트러블슈팅](#트러블슈팅)

---

## 개요

### 학습 사이클

```
1. 프로덕션 사용
   ↓
2. 사용자 피드백 수집 (오탐/정탐 판정)
   ↓
3. MongoDB에 누적
   ↓
4. 주기적으로 데이터 추출
   ↓
5. 모델 재학습
   ↓
6. 검증 & 배포
   ↓
7. 1번으로 돌아가기
```

### 데이터 레이블링 전략

모델은 다음 데이터를 학습에 사용합니다:

1. **사용자 피드백** (최우선)
   - "오탐"이라고 표시 → 정상 메시지로 학습
   - "피싱"이라고 확인 → 피싱 메시지로 학습

2. **관리자 검증**
   - `status: "verified"` → 피싱
   - `status: "false_positive"` → 정상

3. **고신뢰도 자동 분류**
   - risk_score >= 0.8 → 피싱
   - risk_score <= 0.3 → 정상
   - 0.3 ~ 0.8 → 제외 (불확실)

---

## 학습 데이터 수집

### 1. 프로덕션 데이터 확인

MongoDB에서 현재 수집된 데이터 확인:

```bash
cd ~/Desktop/Project/lovechedule-api/ml_training

# MongoDB 연결
mongosh mongodb://localhost:27017/lovechedule

# 최근 30일 데이터 통계
db.phishing_reports.countDocuments({
  timestamp: { $gte: new Date(Date.now() - 30*24*60*60*1000) }
})

# 사용자 피드백이 있는 데이터
db.phishing_reports.countDocuments({
  user_feedback: { $exists: true, $ne: "" }
})

# 관리자 검증 데이터
db.phishing_reports.countDocuments({
  status: { $in: ["verified", "false_positive"] }
})
```

### 2. 데이터 품질 개선

더 많은 학습 데이터를 얻기 위한 방법:

**방법 1: 사용자에게 피드백 요청**
- React Native 앱에서 "이 메시지가 정확히 분류되었나요?" 버튼 추가
- 푸시 알림으로 피드백 요청

**방법 2: 관리자 검증 프로세스**
- 고위험 미처리 신고 정기적으로 검토
- API: `GET /api/phishing/reports/high-risk/pending`

**방법 3: 외부 데이터 추가**
- 한국인터넷진흥원(KISA) 피싱 DB
- 공개 한국어 SMS 피싱 데이터셋

---

## 수동 재학습

### 1. Python 환경 설정

```bash
cd ~/Desktop/Project/lovechedule-api/ml_training

# 가상환경 생성
python3 -m venv venv

# 활성화
source venv/bin/activate

# 패키지 설치
pip install -r requirements.txt
```

### 2. 데이터 추출

프로덕션 MongoDB에서 최근 30일 데이터 추출:

```bash
python export_production_data.py \
  --mongo-uri mongodb://localhost:27017/lovechedule \
  --days 30 \
  --output phishing_dataset_updated.csv \
  --merge \
  --balance
```

**옵션 설명:**
- `--mongo-uri`: MongoDB 연결 문자열
- `--days`: 최근 N일간의 데이터
- `--merge`: 기존 데이터와 병합
- `--balance`: 클래스 균형 조정 (피싱:정상 = 1:1)

### 3. 데이터 확인

```bash
# CSV 파일 열기
head -20 phishing_dataset_updated.csv

# 통계 확인
python -c "
import pandas as pd
df = pd.read_csv('phishing_dataset_updated.csv')
print(f'총 샘플: {len(df)}')
print(f'피싱: {df[\"is_phishing\"].sum()}')
print(f'정상: {len(df) - df[\"is_phishing\"].sum()}')
"
```

### 4. 모델 재학습

```bash
python train_model.py
```

학습 과정:
- 데이터 로드 및 전처리
- LSTM 모델 구축
- 20 에포크 학습 (Early Stopping 적용)
- 검증 데이터 평가
- 모델 저장: `../models/phishing_detection_model/`

### 5. 학습 결과 확인

```bash
# 학습 히스토리 그래프
open training_history.png

# 모델 파일 확인
ls -lh ../models/phishing_detection_model/
```

**필수 파일:**
- `saved_model.pb` - TensorFlow 모델
- `vocabulary.json` - 어휘 사전
- `config.json` - 설정 파일

### 6. 서버 재시작

새 모델을 로드하려면 Spring Boot 서버 재시작:

```bash
cd ~/Desktop/Project/lovechedule-api

# 기존 서버 중지 (Ctrl+C)

# 서버 재시작
./gradlew bootRun
```

---

## 자동 재학습 설정

### 1. 스케줄러 설정

```bash
cd ~/Desktop/Project/lovechedule-api/ml_training

# 실행 권한 부여
chmod +x setup_scheduler.sh

# 스케줄러 설정 실행
./setup_scheduler.sh
```

### 2. 스케줄 선택

대화형 프롬프트에서 선택:

1. **매주 일요일 새벽 2시** (권장)
   - 주간 데이터 누적 후 재학습
   - 서버 부하 최소화

2. **매일 새벽 2시**
   - 빠른 피드백 반영
   - 계산 리소스 많이 필요

3. **매월 1일 새벽 2시**
   - 충분한 데이터 누적
   - 안정적인 업데이트

### 3. Cron 작업 확인

```bash
# 현재 cron 작업 목록
crontab -l

# 로그 파일 확인
ls -lht ~/Desktop/Project/lovechedule-api/ml_training/logs/
```

### 4. 수동 테스트

스케줄러가 제대로 작동하는지 테스트:

```bash
cd ~/Desktop/Project/lovechedule-api/ml_training
./run_retraining.sh
```

---

## 자동화 파이프라인 사용

전체 파이프라인을 한 번에 실행:

```bash
python continuous_learning_pipeline.py \
  --mongo-uri mongodb://localhost:27017/lovechedule \
  --days 30 \
  --auto-deploy
```

**파이프라인 단계:**

1. ✅ 현재 모델 백업
2. ✅ 프로덕션 데이터 추출
3. ✅ 데이터 품질 확인
4. ✅ 모델 재학습
5. ✅ 새 모델 검증
6. ✅ 모델 배포
7. ✅ 리포트 생성

**배포 옵션:**
- `--auto-deploy`: 자동 배포 (확인 없이)
- 옵션 없음: 배포 전 확인 요청

---

## 모델 성능 모니터링

### 1. 정확도 추적

Spring Boot 서버에서 제공하는 통계 API:

```bash
# 사용자별 통계
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/phishing/statistics/me

# 워크스페이스별 통계
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/phishing/statistics/workspace/{workspaceId}
```

**주요 메트릭:**
- `accuracyRate`: 정확도 (0.0 ~ 1.0)
- `falsePositiveCount`: 오탐지 수
- `detectionRate`: 탐지율

### 2. 오탐지 모니터링

```bash
# MongoDB에서 오탐지 확인
mongosh mongodb://localhost:27017/lovechedule

db.phishing_reports.find({
  status: "false_positive"
}).sort({ timestamp: -1 }).limit(10)
```

### 3. 성능 저하 감지

정확도가 떨어지는 경우:

```javascript
// MongoDB Aggregation
db.phishing_statistics.aggregate([
  {
    $match: {
      stat_type: "daily",
      date: { $gte: "2025-01-01" }
    }
  },
  {
    $group: {
      _id: null,
      avg_accuracy: { $avg: "$accuracy_rate" },
      avg_false_positive: { $avg: "$false_positive_count" }
    }
  }
])
```

**성능 저하 원인:**
- 새로운 유형의 피싱 등장
- 데이터 불균형
- 모델 과적합

**해결책:**
- 더 많은 다양한 데이터 수집
- 하이퍼파라미터 조정
- 모델 아키텍처 변경

---

## 모델 버전 관리

### 1. 백업 확인

```bash
ls -lh ~/Desktop/Project/lovechedule-api/ml_training/backups/
```

### 2. 모델 롤백

문제가 있는 경우 이전 모델로 복원:

```python
# continuous_learning_pipeline.py 사용
from continuous_learning_pipeline import ContinuousLearningPipeline

pipeline = ContinuousLearningPipeline()
pipeline.backup_dir = "backups/model_backup_20250121_020000"  # 백업 디렉토리
pipeline.rollback_model()
```

또는 수동 복원:

```bash
BACKUP_DIR="backups/model_backup_20250121_020000"
MODEL_DIR="../models/phishing_detection_model"

rm -rf "$MODEL_DIR"
cp -r "$BACKUP_DIR/model" "$MODEL_DIR"
```

### 3. 버전 태깅

Git으로 모델 버전 관리:

```bash
cd ~/Desktop/Project/lovechedule-api

# 모델 파일 커밋
git add models/phishing_detection_model/
git commit -m "feat: Retrain phishing model with 5000 new samples

- Accuracy: 98.5% → 99.2%
- False positive rate: 2.1% → 1.3%
- Training data: 13,000 samples
- Date: 2025-01-21"

# 태그 생성
git tag -a ml-model-v1.2 -m "ML model version 1.2 - improved accuracy"
```

---

## 트러블슈팅

### 문제 1: 데이터가 너무 적음

**증상:**
```
⚠️ 경고: 데이터가 너무 적습니다 (최소 100개 권장)
```

**해결책:**
1. 더 긴 기간 데이터 수집 (--days 60)
2. 사용자 피드백 독려
3. 외부 데이터셋 추가

### 문제 2: 클래스 불균형

**증상:**
```
피싱: 900개 (90%)
정상: 100개 (10%)
```

**해결책:**
```bash
python export_production_data.py --balance
```

### 문제 3: 모델 학습 실패

**증상:**
```
❌ 모델 학습 실패. 로그 확인: logs/training_xxx.log
```

**해결책:**
```bash
# 로그 확인
cat logs/training_xxx.log

# 일반적인 원인:
# 1. 메모리 부족 → BATCH_SIZE 줄이기
# 2. GPU 오류 → CPU로 학습
# 3. 데이터 오류 → CSV 파일 확인
```

### 문제 4: 새 모델 성능 저하

**증상:**
```
테스트 정확도: 0.8500 (이전: 0.9500)
```

**해결책:**
1. 모델 즉시 롤백
2. 학습 데이터 품질 확인
3. 하이퍼파라미터 재조정

```python
# train_model.py에서 조정
EPOCHS = 30  # 20 → 30
LEARNING_RATE = 0.0005  # 0.001 → 0.0005
DROPOUT_RATE = 0.5  # 0.3 → 0.5
```

### 문제 5: Cron 작업이 실행되지 않음

**확인 사항:**
```bash
# Cron 서비스 상태
sudo service cron status

# Cron 로그 확인
grep CRON /var/log/syslog

# 스크립트 권한 확인
ls -l ~/Desktop/Project/lovechedule-api/ml_training/run_retraining.sh
```

---

## 고급 기법

### 1. Transfer Learning (전이 학습)

기존 모델에서 시작하여 새 데이터로 미세 조정:

```python
# train_model.py 수정
# 기존 모델 로드
model = keras.models.load_model('../models/phishing_detection_model')

# 일부 레이어 동결
for layer in model.layers[:-3]:
    layer.trainable = False

# 새 데이터로 미세 조정
model.compile(
    optimizer=keras.optimizers.Adam(learning_rate=0.0001),
    loss='binary_crossentropy',
    metrics=['accuracy']
)
```

### 2. Ensemble 모델

여러 모델의 예측 결합:

```java
// PhishingMLService.java
double prediction1 = model1.predict(text);
double prediction2 = model2.predict(text);
double prediction3 = model3.predict(text);

double ensemble = (prediction1 + prediction2 + prediction3) / 3.0;
```

### 3. Active Learning

불확실한 샘플을 사용자에게 레이블링 요청:

```java
if (confidence < 0.6) {
    // 사용자에게 피드백 요청
    requestUserFeedback(message, sender);
}
```

---

## 참고 자료

### 관련 파일

- `export_production_data.py` - 데이터 추출
- `train_model.py` - 모델 학습
- `continuous_learning_pipeline.py` - 전체 파이프라인
- `setup_scheduler.sh` - 자동화 설정

### MongoDB 쿼리

```javascript
// 최근 피싱 리포트
db.phishing_reports.find().sort({ timestamp: -1 }).limit(10)

// 사용자 피드백
db.phishing_reports.find({ user_feedback: { $exists: true } })

// 고위험 미처리
db.phishing_reports.find({ risk_level: "high", status: "pending" })
```

### API 엔드포인트

- `POST /api/phishing/detect` - 피싱 탐지
- `POST /api/phishing/report` - 피싱 신고
- `PUT /api/phishing/reports/{id}/feedback` - 사용자 피드백
- `GET /api/phishing/statistics/me` - 내 통계
- `GET /api/phishing/reports/me` - 내 신고 목록

---

## 문의

문제가 있거나 도움이 필요하면:
- GitHub Issues 생성
- 로그 파일 첨부
- 재현 단계 설명
