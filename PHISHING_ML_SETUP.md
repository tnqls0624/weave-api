# 피싱 탐지 ML 모델 설정 가이드

## 개요

이 프로젝트는 **Java Spring Boot 서버에서 TensorFlow/DJL을 사용한 피싱 탐지 시스템**을 구현합니다.

### 아키텍처

```
React Native (클라이언트)
    ↓
    경량 패턴 매칭 (빠른 응답)
    ↓
    서버 API 호출 (/api/phishing/detect)
    ↓
Java Spring Boot (서버)
    ↓
    패턴 매칭 + 휴리스틱 + TensorFlow ML 모델
    ↓
    정밀 분석 결과 반환
```

## 의존성

### build.gradle에 추가된 라이브러리

```gradle
// Deep Java Library (DJL) for TensorFlow - ML Framework
implementation platform('ai.djl:bom:0.30.0')
implementation 'ai.djl.tensorflow:tensorflow-engine'
implementation 'ai.djl.tensorflow:tensorflow-model-zoo'
runtimeOnly 'ai.djl.tensorflow:tensorflow-native-auto'
```

## 설정

### application.yml

```yaml
phishing:
  model:
    enabled: false  # ML 모델 활성화 여부
    path: models/phishing_detection_model  # 모델 파일 경로
```

### 환경 변수로 설정 (권장)

```bash
# ML 모델 활성화
export PHISHING_MODEL_ENABLED=true

# 모델 경로 지정
export PHISHING_MODEL_PATH=/app/models/phishing_detection_model
```

## ML 모델 준비

### 1. 모델 학습 (Python/TensorFlow)

```python
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Embedding, LSTM, Dense, Dropout

# LSTM 모델 생성
model = Sequential([
    Embedding(input_dim=10000, output_dim=128, input_length=200),
    LSTM(64, return_sequences=True, dropout=0.2),
    LSTM(32, dropout=0.2),
    Dense(16, activation='relu'),
    Dropout(0.3),
    Dense(1, activation='sigmoid')
])

model.compile(
    optimizer='adam',
    loss='binary_crossentropy',
    metrics=['accuracy']
)

# 학습
model.fit(X_train, y_train, epochs=10, batch_size=32)

# SavedModel 형식으로 저장
model.save('models/phishing_detection_model', save_format='tf')
```

### 2. 모델 파일 배치

학습된 모델을 다음 위치에 배치:

```
lovechedule-api/
├── models/
│   └── phishing_detection_model/
│       ├── saved_model.pb
│       ├── variables/
│       │   ├── variables.index
│       │   └── variables.data-00000-of-00001
│       └── assets/
```

### 3. 모델 활성화

`application.yml` 수정:

```yaml
phishing:
  model:
    enabled: true  # ← true로 변경
    path: models/phishing_detection_model
```

## 사용 방법

### API 엔드포인트

#### 피싱 탐지 API

```http
POST /api/phishing/detect
Content-Type: application/json

{
  "sender": "01012345678",
  "message": "긴급! 계좌번호를 입력하세요.",
  "sensitivityLevel": "medium"
}
```

**응답:**

```json
{
  "success": true,
  "data": {
    "isPhishing": true,
    "riskScore": 0.85,
    "riskLevel": "high",
    "detectionReasons": [
      "긴급성 유도 표현",
      "개인정보 요구",
      "AI 모델 피싱 판정"
    ],
    "phishingType": "financial",
    "confidence": 0.92
  }
}
```

### 서비스 계층 사용

```java
@Service
@RequiredArgsConstructor
public class SomeService {

    private final PhishingDetectionService detectionService;

    public void checkMessage(String sender, String message) {
        PhishingDetectionResult result = detectionService.detectPhishing(
            sender,
            message,
            "medium"
        );

        if (result.isPhishing()) {
            log.warn("피싱 감지! 점수: {}", result.getRiskScore());
        }
    }
}
```

## 모델 동작 방식

### 1. ML 모델 비활성화 시

```
패턴 매칭 (40%) + 휴리스틱 분석 (60%)
```

### 2. ML 모델 활성화 시

```
ML 모델 (60%) + 패턴 매칭 + 휴리스틱 (40%)
```

### 점수 계산 로직

```java
// PhishingDetectionService.java

// 1. 패턴 매칭
double patternScore = matchPatterns(message);

// 2. 휴리스틱 분석
double heuristicScore = analyzeHeuristics(sender, message);

// 3. ML 모델 분석
double mlScore = 0.0;
if (mlService.isModelAvailable()) {
    mlScore = mlService.predict(message);

    // ML 점수와 휴리스틱 결합
    totalScore = (mlScore * 0.6) + (heuristicScore * 0.4);
}

// 4. 민감도 조정
totalScore = adjustBySensitivity(totalScore, sensitivityLevel);
```

## 성능 최적화

### 1. 모델 캐싱

`PhishingMLService`는 초기화 시 모델을 메모리에 로드하여 재사용합니다.

### 2. 배치 예측

여러 메시지를 한 번에 처리:

```java
Map<String, Double> results = mlService.predictBatch(messageList);
```

### 3. 비동기 처리 (권장)

```java
@Async
public CompletableFuture<PhishingDetectionResult> detectPhishingAsync(
    String sender,
    String message
) {
    return CompletableFuture.completedFuture(
        detectPhishing(sender, message, "medium")
    );
}
```

## 모델 업데이트

### 1. 새 모델 배포

```bash
# 새 모델 파일 복사
cp -r new_model/* /app/models/phishing_detection_model/

# 서버 재시작 없이 모델 리로드 (관리자 API)
curl -X POST http://localhost:8080/api/admin/phishing/reload-model \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### 2. 모델 버전 관리

```
models/
├── phishing_detection_model_v1/
├── phishing_detection_model_v2/
└── phishing_detection_model/  # 심볼릭 링크
```

## 모니터링

### 로그 확인

```bash
# ML 모델 초기화 로그
grep "피싱 탐지 ML 모델" logs/weave-api.log

# 예측 실행 로그
grep "ML 모델 점수" logs/weave-api.log
```

### Actuator 메트릭

```http
GET /actuator/metrics/phishing.detection.ml.predictions
GET /actuator/metrics/phishing.detection.accuracy
```

## 문제 해결

### 모델 로드 실패

**증상:**
```
ML 모델 초기화 실패. 휴리스틱 분석만 사용합니다.
```

**해결:**
1. 모델 파일 경로 확인
2. `phishing.model.path` 설정 확인
3. 파일 권한 확인: `chmod -R 755 models/`

### 메모리 부족

**증상:**
```
OutOfMemoryError during model loading
```

**해결:**
```bash
# JVM 힙 메모리 증가
export JAVA_OPTS="-Xmx2g -Xms1g"
```

### 느린 예측 속도

**해결:**
1. GPU 사용 활성화 (가능한 경우)
2. 모델 경량화 (Quantization)
3. 배치 예측 사용

## React Native 클라이언트 통합

### API 호출 예시

```typescript
import { phishingDetectionEngine } from './services/phishingDetectionEngine';

const result = await phishingDetectionEngine.analyze({
  sender: sms.sender,
  message: sms.body,
  timestamp: Date.now(),
  sensitivityLevel: 'medium'
});

if (result.isPhishing) {
  console.log('피싱 감지!', result);
}
```

## 라이선스 및 주의사항

- DJL은 Apache 2.0 라이선스
- TensorFlow는 Apache 2.0 라이선스
- 모델 학습 데이터는 개인정보 보호 규정 준수 필요

## 참고 자료

- [Deep Java Library (DJL) 문서](https://djl.ai/)
- [TensorFlow SavedModel 포맷](https://www.tensorflow.org/guide/saved_model)
- [피싱 탐지 연구 논문](https://arxiv.org/abs/xxxx.xxxxx)
