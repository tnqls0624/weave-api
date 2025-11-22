"""
FastAPI 기반 ML 추론 서버
TensorFlow 모델을 로드하여 실시간 피싱 탐지 API 제공
"""

import os
import json
import logging
from typing import List, Optional
from contextlib import asynccontextmanager

import numpy as np
import tensorflow as tf
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 전역 변수
model = None
vocabulary = None
config = None

# 설정
MODEL_PATH = os.getenv("MODEL_PATH", "/app/models/phishing_detection_model")
MAX_SEQUENCE_LENGTH = 200
VOCAB_SIZE = 10000


class PredictionRequest(BaseModel):
    """피싱 탐지 요청 모델"""
    sender: str = Field(..., description="SMS 발신자", min_length=1)
    message: str = Field(..., description="SMS 메시지 내용", min_length=1)
    sensitivity_level: Optional[str] = Field("medium", description="민감도 레벨 (high/medium/low)")


class PredictionResponse(BaseModel):
    """피싱 탐지 응답 모델"""
    is_phishing: bool = Field(..., description="피싱 여부")
    risk_score: float = Field(..., description="위험 점수 (0.0~1.0)", ge=0.0, le=1.0)
    risk_level: str = Field(..., description="위험 수준 (high/medium/low)")
    confidence: float = Field(..., description="신뢰도 (0.0~1.0)", ge=0.0, le=1.0)
    model_version: str = Field(..., description="모델 버전")


class HealthResponse(BaseModel):
    """헬스체크 응답 모델"""
    status: str
    model_loaded: bool
    model_path: str
    vocabulary_size: int


def load_model_and_vocabulary():
    """모델과 어휘 사전 로드"""
    global model, vocabulary, config

    try:
        logger.info(f"모델 로드 중: {MODEL_PATH}")

        # TensorFlow 모델 로드
        if not os.path.exists(MODEL_PATH):
            raise FileNotFoundError(f"모델 경로를 찾을 수 없습니다: {MODEL_PATH}")

        model = tf.saved_model.load(MODEL_PATH)
        logger.info("✅ TensorFlow 모델 로드 완료")

        # 어휘 사전 로드
        vocab_path = os.path.join(MODEL_PATH, "vocabulary.json")
        if os.path.exists(vocab_path):
            with open(vocab_path, 'r', encoding='utf-8') as f:
                vocabulary = json.load(f)
            logger.info(f"✅ 어휘 사전 로드 완료 (크기: {len(vocabulary)})")
        else:
            logger.warning("⚠️ 어휘 사전을 찾을 수 없습니다. 기본 어휘 사용")
            vocabulary = {}

        # 설정 로드
        config_path = os.path.join(MODEL_PATH, "config.json")
        if os.path.exists(config_path):
            with open(config_path, 'r', encoding='utf-8') as f:
                config = json.load(f)
            logger.info(f"✅ 설정 로드 완료: {config}")
        else:
            logger.warning("⚠️ 설정 파일을 찾을 수 없습니다. 기본 설정 사용")
            config = {
                "max_sequence_length": MAX_SEQUENCE_LENGTH,
                "vocab_size": VOCAB_SIZE
            }

        logger.info("=" * 60)
        logger.info("모델 초기화 완료")
        logger.info("=" * 60)

    except Exception as e:
        logger.error(f"❌ 모델 로드 실패: {e}")
        raise


def text_to_sequence(text: str) -> List[int]:
    """텍스트를 시퀀스로 변환"""
    if not vocabulary:
        logger.warning("어휘 사전이 없습니다")
        return []

    # 토큰화 (간단한 공백 기반)
    tokens = text.lower().split()

    # 어휘 사전을 사용하여 인덱스로 변환
    sequence = []
    for token in tokens:
        if token in vocabulary:
            sequence.append(vocabulary[token])
        else:
            # OOV (Out of Vocabulary) 토큰
            sequence.append(1)  # 1은 보통 <OOV> 토큰

    return sequence


def pad_sequence(sequence: List[int], max_length: int) -> np.ndarray:
    """시퀀스를 고정 길이로 패딩"""
    if len(sequence) > max_length:
        # 잘라내기
        return np.array(sequence[:max_length])
    else:
        # 패딩
        padded = np.zeros(max_length, dtype=np.int32)
        padded[:len(sequence)] = sequence
        return padded


def adjust_score_by_sensitivity(score: float, sensitivity: str) -> float:
    """민감도에 따라 점수 조정"""
    if sensitivity == "high":
        return min(score * 1.2, 1.0)  # 20% 증가
    elif sensitivity == "low":
        return score * 0.8  # 20% 감소
    else:  # medium
        return score


def calculate_risk_level(score: float, sensitivity: str) -> str:
    """위험 수준 계산"""
    if sensitivity == "high":
        if score >= 0.3:
            return "high"
        elif score >= 0.2:
            return "medium"
        else:
            return "low"
    elif sensitivity == "low":
        if score >= 0.6:
            return "high"
        elif score >= 0.4:
            return "medium"
        else:
            return "low"
    else:  # medium
        if score >= 0.45:
            return "high"
        elif score >= 0.3:
            return "medium"
        else:
            return "low"


@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 생명주기 관리"""
    # 시작 시
    logger.info("=" * 60)
    logger.info("ML 추론 서버 시작")
    logger.info("=" * 60)
    load_model_and_vocabulary()
    yield
    # 종료 시
    logger.info("ML 추론 서버 종료")


# FastAPI 앱 생성
app = FastAPI(
    title="Phishing Detection ML Inference API",
    description="TensorFlow 기반 SMS 피싱 탐지 추론 서버",
    version="1.0.0",
    lifespan=lifespan
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/", tags=["Root"])
async def root():
    """루트 엔드포인트"""
    return {
        "service": "Phishing Detection ML Inference Server",
        "status": "running",
        "version": "1.0.0"
    }


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """헬스체크 엔드포인트"""
    return HealthResponse(
        status="healthy" if model is not None else "unhealthy",
        model_loaded=model is not None,
        model_path=MODEL_PATH,
        vocabulary_size=len(vocabulary) if vocabulary else 0
    )


@app.post("/predict", response_model=PredictionResponse, tags=["Prediction"])
async def predict(request: PredictionRequest):
    """
    피싱 메시지 탐지 API

    - **sender**: SMS 발신자 (전화번호 등)
    - **message**: SMS 메시지 내용
    - **sensitivity_level**: 민감도 (high/medium/low)

    Returns:
    - **is_phishing**: 피싱 여부
    - **risk_score**: 위험 점수 (0.0~1.0)
    - **risk_level**: 위험 수준 (high/medium/low)
    - **confidence**: 신뢰도
    """
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="모델이 로드되지 않았습니다"
        )

    try:
        logger.info(f"예측 요청 - 발신자: {request.sender[:20]}...")

        # 1. 텍스트 전처리
        full_text = f"{request.sender} {request.message}"
        sequence = text_to_sequence(full_text)

        # 2. 패딩
        max_length = config.get("max_sequence_length", MAX_SEQUENCE_LENGTH)
        padded_sequence = pad_sequence(sequence, max_length)

        # 3. 배치 차원 추가 (1, max_length)
        input_data = np.expand_dims(padded_sequence, axis=0)

        # 4. TensorFlow 추론
        # 모델 시그니처 확인 및 호출
        infer = model.signatures["serving_default"]

        # 입력 텐서 생성
        input_tensor = tf.constant(input_data, dtype=tf.int32)

        # 추론 실행
        predictions = infer(input_tensor)

        # 결과 추출 (첫 번째 출력)
        output_key = list(predictions.keys())[0]
        prediction = predictions[output_key].numpy()[0][0]

        # 5. 민감도에 따라 점수 조정
        adjusted_score = adjust_score_by_sensitivity(
            float(prediction),
            request.sensitivity_level
        )

        # 6. 위험 수준 결정
        risk_level = calculate_risk_level(
            adjusted_score,
            request.sensitivity_level
        )

        # 7. 피싱 여부 결정
        threshold = 0.5
        is_phishing = adjusted_score >= threshold

        logger.info(
            f"예측 완료 - 점수: {adjusted_score:.4f}, "
            f"위험도: {risk_level}, 피싱: {is_phishing}"
        )

        return PredictionResponse(
            is_phishing=is_phishing,
            risk_score=round(adjusted_score, 4),
            risk_level=risk_level,
            confidence=round(float(prediction), 4),
            model_version=config.get("version", "1.0")
        )

    except Exception as e:
        logger.error(f"예측 실패: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"예측 중 오류 발생: {str(e)}"
        )


@app.post("/batch-predict", tags=["Prediction"])
async def batch_predict(requests: List[PredictionRequest]):
    """배치 예측 API"""
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="모델이 로드되지 않았습니다"
        )

    results = []
    for req in requests:
        try:
            result = await predict(req)
            results.append(result)
        except Exception as e:
            logger.error(f"배치 예측 실패: {e}")
            results.append(None)

    return {"predictions": results}


@app.get("/model-info", tags=["Model"])
async def get_model_info():
    """모델 정보 조회"""
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="모델이 로드되지 않았습니다"
        )

    return {
        "model_path": MODEL_PATH,
        "vocabulary_size": len(vocabulary) if vocabulary else 0,
        "config": config,
        "max_sequence_length": MAX_SEQUENCE_LENGTH,
    }


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("PORT", "8000"))
    host = os.getenv("HOST", "0.0.0.0")

    logger.info(f"서버 시작: {host}:{port}")

    uvicorn.run(
        app,
        host=host,
        port=port,
        log_level="info"
    )
