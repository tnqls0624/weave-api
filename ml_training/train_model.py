"""
SMS 피싱 탐지 LSTM 모델 학습
TensorFlow/Keras 기반
"""

import os
import json
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt

# 설정
MAX_WORDS = 10000  # 어휘 크기
MAX_SEQUENCE_LENGTH = 200  # 최대 시퀀스 길이
EMBEDDING_DIM = 128  # 임베딩 차원
LSTM_UNITS_1 = 64
LSTM_UNITS_2 = 32
DENSE_UNITS = 16
DROPOUT_RATE = 0.3
BATCH_SIZE = 32
EPOCHS = 20
VALIDATION_SPLIT = 0.2

class PhishingModelTrainer:
    def __init__(self):
        self.tokenizer = None
        self.model = None
        self.history = None

    def load_data(self, csv_path="phishing_dataset.csv"):
        """데이터 로드"""
        print("=" * 60)
        print("데이터 로드 중...")
        print("=" * 60)

        df = pd.read_csv(csv_path, encoding='utf-8-sig')

        print(f"\n총 데이터 수: {len(df)}")
        print(f"피싱: {df['is_phishing'].sum()}")
        print(f"정상: {len(df) - df['is_phishing'].sum()}")

        # 메시지와 라벨 추출
        messages = df['message'].values
        labels = df['is_phishing'].values

        return messages, labels

    def preprocess_text(self, messages, labels):
        """텍스트 전처리 및 토큰화"""
        print("\n" + "=" * 60)
        print("텍스트 전처리 중...")
        print("=" * 60)

        # Tokenizer 초기화
        self.tokenizer = Tokenizer(
            num_words=MAX_WORDS,
            oov_token="<OOV>",
            filters='!"#$%&()*+,-./:;<=>?@[\\]^_`{|}~\t\n'
        )

        # 토큰화
        self.tokenizer.fit_on_texts(messages)

        # 시퀀스 변환
        sequences = self.tokenizer.texts_to_sequences(messages)

        # 패딩
        padded_sequences = pad_sequences(
            sequences,
            maxlen=MAX_SEQUENCE_LENGTH,
            padding='post',
            truncating='post'
        )

        print(f"\n어휘 크기: {len(self.tokenizer.word_index)}")
        print(f"시퀀스 길이: {MAX_SEQUENCE_LENGTH}")
        print(f"샘플 시퀀스 형태: {padded_sequences.shape}")

        # Train/Test 분할
        X_train, X_test, y_train, y_test = train_test_split(
            padded_sequences,
            labels,
            test_size=0.2,
            random_state=42,
            stratify=labels
        )

        print(f"\n학습 데이터: {len(X_train)}")
        print(f"테스트 데이터: {len(X_test)}")

        return X_train, X_test, y_train, y_test

    def build_model(self):
        """LSTM 모델 구축"""
        print("\n" + "=" * 60)
        print("모델 구축 중...")
        print("=" * 60)

        model = keras.Sequential([
            # 임베딩 레이어
            layers.Embedding(
                input_dim=MAX_WORDS,
                output_dim=EMBEDDING_DIM,
                input_length=MAX_SEQUENCE_LENGTH,
                name='embedding'
            ),

            # 첫 번째 LSTM 레이어
            layers.Bidirectional(
                layers.LSTM(
                    LSTM_UNITS_1,
                    return_sequences=True,
                    dropout=DROPOUT_RATE,
                    recurrent_dropout=0.2
                ),
                name='bi_lstm_1'
            ),

            # 두 번째 LSTM 레이어
            layers.Bidirectional(
                layers.LSTM(
                    LSTM_UNITS_2,
                    dropout=DROPOUT_RATE,
                    recurrent_dropout=0.2
                ),
                name='bi_lstm_2'
            ),

            # Dense 레이어
            layers.Dense(DENSE_UNITS, activation='relu', name='dense'),
            layers.Dropout(DROPOUT_RATE, name='dropout'),

            # 출력 레이어
            layers.Dense(1, activation='sigmoid', name='output')
        ])

        # 모델 컴파일
        model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=0.001),
            loss='binary_crossentropy',
            metrics=['accuracy', keras.metrics.Precision(), keras.metrics.Recall()]
        )

        self.model = model

        # 모델 구조 출력
        print("\n모델 구조:")
        model.summary()

        return model

    def train(self, X_train, y_train, X_test, y_test):
        """모델 학습"""
        print("\n" + "=" * 60)
        print("모델 학습 시작...")
        print("=" * 60)

        # 콜백 설정
        callbacks = [
            keras.callbacks.EarlyStopping(
                monitor='val_loss',
                patience=3,
                restore_best_weights=True,
                verbose=1
            ),
            keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=2,
                verbose=1,
                min_lr=0.00001
            ),
            keras.callbacks.ModelCheckpoint(
                'best_model.h5',
                monitor='val_accuracy',
                save_best_only=True,
                verbose=1
            )
        ]

        # 학습
        self.history = self.model.fit(
            X_train,
            y_train,
            batch_size=BATCH_SIZE,
            epochs=EPOCHS,
            validation_split=VALIDATION_SPLIT,
            callbacks=callbacks,
            verbose=1
        )

        # 테스트 평가
        print("\n" + "=" * 60)
        print("테스트 데이터 평가...")
        print("=" * 60)

        test_loss, test_accuracy, test_precision, test_recall = self.model.evaluate(
            X_test,
            y_test,
            verbose=1
        )

        print(f"\n테스트 손실: {test_loss:.4f}")
        print(f"테스트 정확도: {test_accuracy:.4f}")
        print(f"테스트 정밀도: {test_precision:.4f}")
        print(f"테스트 재현율: {test_recall:.4f}")
        print(f"F1 Score: {2 * (test_precision * test_recall) / (test_precision + test_recall):.4f}")

        return self.history

    def evaluate_detailed(self, X_test, y_test):
        """상세 평가"""
        print("\n" + "=" * 60)
        print("상세 평가...")
        print("=" * 60)

        # 예측
        y_pred_proba = self.model.predict(X_test)
        y_pred = (y_pred_proba > 0.5).astype(int)

        # 분류 리포트
        print("\n분류 리포트:")
        print(classification_report(
            y_test,
            y_pred,
            target_names=['정상', '피싱']
        ))

        # 혼동 행렬
        print("\n혼동 행렬:")
        cm = confusion_matrix(y_test, y_pred)
        print(cm)
        print(f"\nTrue Negatives: {cm[0][0]}")
        print(f"False Positives: {cm[0][1]}")
        print(f"False Negatives: {cm[1][0]}")
        print(f"True Positives: {cm[1][1]}")

        # 정확도 계산
        accuracy = (cm[0][0] + cm[1][1]) / cm.sum()
        precision = cm[1][1] / (cm[1][1] + cm[0][1])
        recall = cm[1][1] / (cm[1][1] + cm[1][0])
        f1 = 2 * (precision * recall) / (precision + recall)

        print(f"\n최종 메트릭:")
        print(f"정확도: {accuracy:.4f}")
        print(f"정밀도: {precision:.4f}")
        print(f"재현율: {recall:.4f}")
        print(f"F1 Score: {f1:.4f}")

    def plot_history(self):
        """학습 히스토리 시각화"""
        print("\n학습 히스토리 플롯 생성 중...")

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))

        # 손실
        axes[0, 0].plot(self.history.history['loss'], label='Train Loss')
        axes[0, 0].plot(self.history.history['val_loss'], label='Val Loss')
        axes[0, 0].set_title('Model Loss')
        axes[0, 0].set_xlabel('Epoch')
        axes[0, 0].set_ylabel('Loss')
        axes[0, 0].legend()
        axes[0, 0].grid(True)

        # 정확도
        axes[0, 1].plot(self.history.history['accuracy'], label='Train Accuracy')
        axes[0, 1].plot(self.history.history['val_accuracy'], label='Val Accuracy')
        axes[0, 1].set_title('Model Accuracy')
        axes[0, 1].set_xlabel('Epoch')
        axes[0, 1].set_ylabel('Accuracy')
        axes[0, 1].legend()
        axes[0, 1].grid(True)

        # 정밀도
        axes[1, 0].plot(self.history.history['precision'], label='Train Precision')
        axes[1, 0].plot(self.history.history['val_precision'], label='Val Precision')
        axes[1, 0].set_title('Model Precision')
        axes[1, 0].set_xlabel('Epoch')
        axes[1, 0].set_ylabel('Precision')
        axes[1, 0].legend()
        axes[1, 0].grid(True)

        # 재현율
        axes[1, 1].plot(self.history.history['recall'], label='Train Recall')
        axes[1, 1].plot(self.history.history['val_recall'], label='Val Recall')
        axes[1, 1].set_title('Model Recall')
        axes[1, 1].set_xlabel('Epoch')
        axes[1, 1].set_ylabel('Recall')
        axes[1, 1].legend()
        axes[1, 1].grid(True)

        plt.tight_layout()
        plt.savefig('training_history.png', dpi=300, bbox_inches='tight')
        print("학습 히스토리 저장: training_history.png")

    def save_model(self, output_dir="../models/phishing_detection_model"):
        """모델 SavedModel 형식으로 저장"""
        print("\n" + "=" * 60)
        print("모델 저장 중...")
        print("=" * 60)

        # 디렉토리 생성
        os.makedirs(output_dir, exist_ok=True)

        # SavedModel 형식으로 저장 (Keras 3 - export 사용)
        self.model.export(output_dir)
        print(f"모델 저장 완료: {output_dir}")

        # 어휘 사전 저장
        vocab_path = os.path.join(output_dir, "vocabulary.json")
        with open(vocab_path, 'w', encoding='utf-8') as f:
            json.dump(self.tokenizer.word_index, f, ensure_ascii=False, indent=2)
        print(f"어휘 사전 저장: {vocab_path}")

        # 설정 저장
        config = {
            "max_words": MAX_WORDS,
            "max_sequence_length": MAX_SEQUENCE_LENGTH,
            "embedding_dim": EMBEDDING_DIM,
            "vocab_size": len(self.tokenizer.word_index)
        }
        config_path = os.path.join(output_dir, "config.json")
        with open(config_path, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2)
        print(f"설정 저장: {config_path}")

    def test_predictions(self, test_messages):
        """테스트 예측"""
        print("\n" + "=" * 60)
        print("테스트 예측...")
        print("=" * 60)

        for message in test_messages:
            # 전처리
            sequence = self.tokenizer.texts_to_sequences([message])
            padded = pad_sequences(sequence, maxlen=MAX_SEQUENCE_LENGTH, padding='post')

            # 예측
            prediction = self.model.predict(padded, verbose=0)[0][0]

            print(f"\n메시지: {message}")
            print(f"피싱 확률: {prediction:.4f}")
            print(f"판정: {'🚨 피싱' if prediction > 0.5 else '✅ 정상'}")

def main():
    print("=" * 60)
    print("SMS 피싱 탐지 모델 학습")
    print("=" * 60)

    # 트레이너 초기화
    trainer = PhishingModelTrainer()

    # 1. 데이터 로드
    messages, labels = trainer.load_data("phishing_dataset.csv")

    # 2. 전처리
    X_train, X_test, y_train, y_test = trainer.preprocess_text(messages, labels)

    # 3. 모델 구축
    trainer.build_model()

    # 4. 학습
    trainer.train(X_train, y_train, X_test, y_test)

    # 5. 상세 평가
    trainer.evaluate_detailed(X_test, y_test)

    # 6. 히스토리 플롯
    trainer.plot_history()

    # 7. 모델 저장
    trainer.save_model()

    # 8. 테스트 예측
    test_messages = [
        "긴급! 국민은행 계좌가 정지되었습니다. http://bit.ly/abc123에서 확인하세요.",
        "내일 회의 일정 확인 부탁드립니다.",
        "[국세청] 환급금 500,000원이 발생했습니다. http://me2.do/xyz에서 계좌 입력하세요.",
        "오늘 저녁 약속 어때요?",
        "축하합니다! 이벤트 당첨 1,000,000원 상품권. http://short.link/abc",
    ]
    trainer.test_predictions(test_messages)

    print("\n" + "=" * 60)
    print("학습 완료!")
    print("=" * 60)
    print("\n모델 파일 위치: ../models/phishing_detection_model/")
    print("\n다음 단계:")
    print("1. application.yml에서 phishing.model.enabled=true로 변경")
    print("2. 서버 재시작")

if __name__ == "__main__":
    main()
