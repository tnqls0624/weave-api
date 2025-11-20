#!/bin/bash

echo "======================================"
echo "SMS 피싱 탐지 모델 학습 스크립트"
echo "======================================"

# 1. Python 가상환경 생성
echo ""
echo "Step 1: Python 가상환경 생성..."
python3 -m venv venv

# 2. 가상환경 활성화
echo ""
echo "Step 2: 가상환경 활성화..."
source venv/bin/activate

# 3. 의존성 설치
echo ""
echo "Step 3: 의존성 설치..."
pip install --upgrade pip
pip install -r requirements.txt

# 4. 데이터셋 생성
echo ""
echo "Step 4: 학습 데이터셋 생성..."
python generate_training_data.py

# 5. 모델 학습
echo ""
echo "Step 5: 모델 학습 시작..."
python train_model.py

# 6. 완료
echo ""
echo "======================================"
echo "학습 완료!"
echo "======================================"
echo ""
echo "생성된 파일:"
echo "  - phishing_dataset.csv (학습 데이터)"
echo "  - training_history.png (학습 그래프)"
echo "  - ../models/phishing_detection_model/ (모델 파일)"
echo ""
echo "다음 단계:"
echo "  1. application.yml에서 phishing.model.enabled=true로 변경"
echo "  2. ./gradlew bootRun으로 서버 시작"
echo ""
