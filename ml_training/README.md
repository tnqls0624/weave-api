# SMS 피싱 탐지 ML 모델 학습 가이드

## 📋 개요

이 디렉토리는 **한국어 SMS 피싱 탐지를 위한 LSTM 딥러닝 모델**을 학습하는 스크립트를 포함합니다.

### 모델 아키텍처

```
입력 (문자 메시지)
    ↓
Tokenization & Padding
    ↓
Embedding Layer (128차원)
    ↓
Bidirectional LSTM (64 units) + Dropout
    ↓
Bidirectional LSTM (32 units) + Dropout
    ↓
Dense Layer (16 units, ReLU)
    ↓
Dropout (0.3)
    ↓
Output Layer (Sigmoid)
    ↓
피싱 확률 (0~1)
```

## 🚀 빠른 시작

### 1. 자동 실행 (권장)

```bash
cd ml_training
./run_training.sh
```

이 스크립트는 자동으로:
- Python 가상환경 생성
- 의존성 설치
- 데이터셋 생성 (10,000개)
- 모델 학습
- 모델 저장

### 2. 수동 실행

#### Step 1: 환경 설정

```bash
# Python 가상환경 생성
python3 -m venv venv

# 활성화
source venv/bin/activate  # macOS/Linux
# or
venv\Scripts\activate  # Windows

# 의존성 설치
pip install -r requirements.txt
```

#### Step 2: 데이터셋 생성

```bash
python generate_training_data.py
```

**출력:**
- `phishing_dataset.csv`: 10,000개의 한국어 SMS 메시지
  - 피싱 메시지: 5,000개
  - 정상 메시지: 5,000개

#### Step 3: 모델 학습

```bash
python train_model.py
```

**학습 시간:**
- CPU: 약 10-15분
- GPU: 약 3-5분

**출력 파일:**
```
../models/phishing_detection_model/
├── saved_model.pb           # TensorFlow 모델
├── variables/
│   ├── variables.index
│   └── variables.data-00000-of-00001
├── assets/
├── vocabulary.json          # 어휘 사전
└── config.json             # 모델 설정

ml_training/
├── phishing_dataset.csv    # 학습 데이터
├── training_history.png    # 학습 그래프
└── best_model.h5           # 최고 성능 모델 (백업)
```

## 📊 데이터셋 구성

### 피싱 메시지 카테고리

1. **금융 (financial)**: 은행/카드사 사칭
2. **정부기관 (government)**: 국세청/경찰청 사칭
3. **택배 (delivery)**: 택배사 사칭
4. **이벤트 (event)**: 당첨/쿠폰 사기

### 샘플 데이터

**피싱 메시지:**
```
국민은행입니다. 계좌가 정지되었습니다. http://bit.ly/abc123에서 확인하세요.
[국세청] 환급금 500,000원이 발생했습니다. http://me2.do/xyz에서 계좌 입력하세요.
축하합니다! 삼성전자 이벤트 당첨 1,000,000원 상품권. http://short.link/abc
```

**정상 메시지:**
```
안녕하세요. 내일 회의 일정 확인 부탁드립니다.
[카카오톡] 새 메시지가 도착했습니다.
택배가 현관 앞에 놓여있어요.
```

## 🎯 모델 성능 목표

| 메트릭 | 목표 | 설명 |
|--------|------|------|
| **정확도 (Accuracy)** | > 92% | 전체 정확도 |
| **정밀도 (Precision)** | > 90% | 피싱으로 판정한 것 중 실제 피싱 비율 |
| **재현율 (Recall)** | > 95% | 실제 피싱 중 탐지한 비율 (중요!) |
| **F1 Score** | > 92% | 정밀도와 재현율의 조화 평균 |

### 성능 우선순위

1. **재현율 (Recall)**: 가장 중요! 피싱을 놓치면 안됨
2. **F1 Score**: 균형 잡힌 성능
3. **정밀도 (Precision)**: 오탐 최소화

## 🔧 하이퍼파라미터 튜닝

### 기본 설정 (train_model.py)

```python
MAX_WORDS = 10000              # 어휘 크기
MAX_SEQUENCE_LENGTH = 200      # 최대 시퀀스 길이
EMBEDDING_DIM = 128            # 임베딩 차원
LSTM_UNITS_1 = 64             # 첫 번째 LSTM 유닛
LSTM_UNITS_2 = 32             # 두 번째 LSTM 유닛
DENSE_UNITS = 16              # Dense 레이어 유닛
DROPOUT_RATE = 0.3            # 드롭아웃 비율
BATCH_SIZE = 32               # 배치 크기
EPOCHS = 20                   # 에포크 수
```

### 성능 개선 팁

#### 과적합(Overfitting) 발생 시:
```python
DROPOUT_RATE = 0.5            # 드롭아웃 증가
LSTM_UNITS_1 = 32             # 유닛 감소
LSTM_UNITS_2 = 16
```

#### 과소적합(Underfitting) 발생 시:
```python
LSTM_UNITS_1 = 128            # 유닛 증가
LSTM_UNITS_2 = 64
EPOCHS = 30                   # 에포크 증가
```

## 📈 학습 모니터링

### 학습 중 출력

```
Epoch 1/20
250/250 [==============================] - 45s 180ms/step
  loss: 0.3241 - accuracy: 0.8523 - precision: 0.8421 - recall: 0.8734
  val_loss: 0.2145 - val_accuracy: 0.9156 - val_precision: 0.9023 - val_recall: 0.9312

Epoch 2/20
...
```

### 학습 그래프

`training_history.png` 파일로 저장:
- Loss 그래프
- Accuracy 그래프
- Precision 그래프
- Recall 그래프

## 🧪 모델 테스트

### 학습 후 자동 테스트

학습 완료 후 다음 메시지로 자동 테스트:

```python
test_messages = [
    "긴급! 국민은행 계좌가 정지되었습니다. http://bit.ly/abc123에서 확인하세요.",
    "내일 회의 일정 확인 부탁드립니다.",
    "[국세청] 환급금 500,000원이 발생했습니다.",
    "오늘 저녁 약속 어때요?",
]
```

### 수동 테스트

```python
from train_model import PhishingModelTrainer

trainer = PhishingModelTrainer()
# ... 모델 로드 ...

message = "귀하의 계좌가 정지되었습니다. 클릭하세요."
trainer.test_predictions([message])
```

## 🔄 모델 업데이트 프로세스

### 1. 재학습이 필요한 경우

- 새로운 피싱 패턴 발견
- 오탐률이 높아진 경우
- 사용자 피드백 데이터 축적

### 2. 재학습 프로세스

```bash
# 1. 새 데이터 추가
# phishing_dataset.csv에 새 데이터 추가

# 2. 재학습
python train_model.py

# 3. 모델 비교
# 이전 모델과 새 모델 성능 비교

# 4. 배포
# 성능이 개선되었으면 프로덕션에 배포
```

### 3. A/B 테스트

```
models/
├── phishing_detection_model_v1/  # 현재 프로덕션
├── phishing_detection_model_v2/  # 새 모델 (테스트)
└── phishing_detection_model/     # 심볼릭 링크
```

## 🐛 문제 해결

### 1. 메모리 부족 (OOM)

**증상:**
```
ResourceExhaustedError: OOM when allocating tensor
```

**해결:**
```python
# BATCH_SIZE 감소
BATCH_SIZE = 16  # 32 → 16

# 또는 MAX_SEQUENCE_LENGTH 감소
MAX_SEQUENCE_LENGTH = 100  # 200 → 100
```

### 2. 학습이 너무 느림

**해결:**
```bash
# GPU 사용 확인
python -c "import tensorflow as tf; print(tf.config.list_physical_devices('GPU'))"

# GPU가 없으면 CPU 코어 수 증가
export TF_NUM_INTEROP_THREADS=4
export TF_NUM_INTRAOP_THREADS=4
```

### 3. 낮은 정확도

**원인:**
- 데이터가 부족
- 불균형한 데이터
- 하이퍼파라미터 미조정

**해결:**
```python
# 1. 데이터 증강
num_phishing = 10000  # 5000 → 10000
num_normal = 10000

# 2. Class Weight 추가
from sklearn.utils.class_weight import compute_class_weight

class_weights = compute_class_weight(
    'balanced',
    classes=np.unique(y_train),
    y=y_train
)

model.fit(..., class_weight=dict(enumerate(class_weights)))
```

## 📦 Java 서버 통합

### 1. 모델 파일 복사

```bash
cp -r models/phishing_detection_model/ ../models/
```

### 2. application.yml 수정

```yaml
phishing:
  model:
    enabled: true  # ← true로 변경
    path: models/phishing_detection_model
```

### 3. 서버 재시작

```bash
cd ..
./gradlew bootRun
```

### 4. 테스트

```bash
curl -X POST http://localhost:8080/api/phishing/detect \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "sender": "01012345678",
    "message": "긴급! 계좌번호를 입력하세요.",
    "sensitivityLevel": "medium"
  }'
```

## 📚 참고 자료

- [TensorFlow 공식 문서](https://www.tensorflow.org/)
- [Keras LSTM 가이드](https://keras.io/api/layers/recurrent_layers/lstm/)
- [텍스트 분류 튜토리얼](https://www.tensorflow.org/tutorials/text/text_classification_rnn)
- [한국어 자연어 처리](https://github.com/lovit/soynlp)

## 📄 라이선스

이 프로젝트는 학습 목적으로만 사용하세요. 실제 프로덕션에서는 개인정보 보호 규정을 준수해야 합니다.

---

**문의사항이나 이슈가 있으면 GitHub Issues에 등록해주세요.**
