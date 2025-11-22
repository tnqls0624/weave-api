"""
프로덕션 MongoDB에서 피싱 리포트 데이터를 추출하여 학습 데이터 생성
사용자 피드백을 기반으로 레이블 정확도 향상
"""

import os
import sys
import pandas as pd
from pymongo import MongoClient
from datetime import datetime, timedelta
import argparse

class ProductionDataExporter:
    def __init__(self, mongo_uri=None):
        """
        MongoDB 연결 초기화

        Args:
            mongo_uri: MongoDB 연결 URI (None이면 환경 변수 MONGO_URI 사용)
        """
        # 환경 변수 우선, 없으면 기본값
        if mongo_uri is None:
            mongo_uri = os.getenv('MONGO_URI', 'mongodb://localhost:27017/lovechedule')

        self.client = MongoClient(mongo_uri)
        self.db = self.client.get_default_database()
        self.reports_collection = self.db['phishing_reports']

    def export_verified_reports(self, days_back=30, min_confidence=0.7):
        """
        검증된 피싱 리포트 추출

        Args:
            days_back: 최근 N일간의 데이터
            min_confidence: 최소 신뢰도 (ML 모델 confidence)

        Returns:
            DataFrame with columns: message, is_phishing, feedback, confidence
        """
        print("=" * 60)
        print("프로덕션 데이터 추출 중...")
        print("=" * 60)

        # 날짜 필터
        since_date = datetime.now() - timedelta(days=days_back)

        # MongoDB 쿼리
        query = {
            'timestamp': {'$gte': since_date},
            'message': {'$exists': True, '$ne': ''},
        }

        # 데이터 추출
        reports = list(self.reports_collection.find(query))

        print(f"\n총 {len(reports)}개의 리포트 발견")

        data = []

        for report in reports:
            message = report.get('message', '')
            risk_score = report.get('risk_score', 0)
            risk_level = report.get('risk_level', 'low')
            user_feedback = report.get('user_feedback', '')
            status = report.get('status', 'pending')

            # 레이블 결정 로직
            is_phishing = None

            # 1. 사용자 피드백이 있으면 최우선
            if user_feedback:
                if '오탐' in user_feedback or 'false' in user_feedback.lower():
                    is_phishing = 0  # 정상 (오탐지)
                elif '피싱' in user_feedback or 'phishing' in user_feedback.lower():
                    is_phishing = 1  # 피싱 확인

            # 2. 관리자가 검증한 경우
            if status == 'verified':
                is_phishing = 1  # 피싱 확인됨
            elif status == 'false_positive':
                is_phishing = 0  # 오탐지 확인됨

            # 3. 리스크 점수 기반 (피드백이 없는 경우)
            if is_phishing is None:
                if risk_score >= 0.8:  # 고위험
                    is_phishing = 1
                elif risk_score <= 0.3:  # 저위험
                    is_phishing = 0
                # 0.3 ~ 0.8 사이는 불확실하므로 제외

            # 레이블이 결정된 경우만 추가
            if is_phishing is not None:
                data.append({
                    'message': message,
                    'is_phishing': is_phishing,
                    'risk_score': risk_score,
                    'risk_level': risk_level,
                    'user_feedback': user_feedback,
                    'status': status,
                    'timestamp': report.get('timestamp', datetime.now())
                })

        df = pd.DataFrame(data)

        if len(df) > 0:
            print(f"\n레이블링된 데이터: {len(df)}개")
            print(f"  - 피싱: {df['is_phishing'].sum()}개")
            print(f"  - 정상: {len(df) - df['is_phishing'].sum()}개")
            print(f"\n데이터 소스:")
            print(f"  - 사용자 피드백: {df['user_feedback'].notna().sum()}개")
            print(f"  - 관리자 검증: {(df['status'] == 'verified').sum()}개")
            print(f"  - 자동 레이블: {df['user_feedback'].isna().sum()}개")
        else:
            print("\n⚠️ 추출된 데이터가 없습니다")

        return df

    def balance_dataset(self, df, balance_ratio=1.0):
        """
        데이터셋 균형 맞추기 (오버샘플링/언더샘플링)

        Args:
            df: 입력 DataFrame
            balance_ratio: 피싱/정상 비율 (1.0 = 1:1)

        Returns:
            균형 잡힌 DataFrame
        """
        phishing_count = df['is_phishing'].sum()
        normal_count = len(df) - phishing_count

        print(f"\n균형 조정 전:")
        print(f"  피싱: {phishing_count}개")
        print(f"  정상: {normal_count}개")
        print(f"  비율: 1:{normal_count/max(phishing_count, 1):.2f}")

        # 소수 클래스 오버샘플링
        if phishing_count < normal_count * balance_ratio:
            phishing_df = df[df['is_phishing'] == 1]
            normal_df = df[df['is_phishing'] == 0]

            # 피싱 데이터 오버샘플링
            target_phishing_count = int(normal_count * balance_ratio)
            phishing_oversampled = phishing_df.sample(
                n=target_phishing_count,
                replace=True,
                random_state=42
            )

            df_balanced = pd.concat([phishing_oversampled, normal_df], ignore_index=True)
        else:
            # 정상 데이터 언더샘플링
            phishing_df = df[df['is_phishing'] == 1]
            normal_df = df[df['is_phishing'] == 0]

            target_normal_count = int(phishing_count / balance_ratio)
            normal_undersampled = normal_df.sample(
                n=min(target_normal_count, len(normal_df)),
                random_state=42
            )

            df_balanced = pd.concat([phishing_df, normal_undersampled], ignore_index=True)

        # 셔플
        df_balanced = df_balanced.sample(frac=1, random_state=42).reset_index(drop=True)

        phishing_count_new = df_balanced['is_phishing'].sum()
        normal_count_new = len(df_balanced) - phishing_count_new

        print(f"\n균형 조정 후:")
        print(f"  피싱: {phishing_count_new}개")
        print(f"  정상: {normal_count_new}개")
        print(f"  비율: 1:{normal_count_new/max(phishing_count_new, 1):.2f}")

        return df_balanced

    def merge_with_existing(self, new_df, existing_csv="phishing_dataset.csv"):
        """
        기존 학습 데이터와 신규 데이터 병합

        Args:
            new_df: 신규 데이터
            existing_csv: 기존 데이터셋 경로

        Returns:
            병합된 DataFrame
        """
        print("\n" + "=" * 60)
        print("기존 데이터와 병합 중...")
        print("=" * 60)

        if os.path.exists(existing_csv):
            existing_df = pd.read_csv(existing_csv, encoding='utf-8-sig')
            print(f"\n기존 데이터: {len(existing_df)}개")

            # 중복 제거 (메시지 기준)
            merged_df = pd.concat([existing_df, new_df], ignore_index=True)
            merged_df = merged_df.drop_duplicates(subset=['message'], keep='last')

            print(f"신규 데이터: {len(new_df)}개")
            print(f"병합 후: {len(merged_df)}개")
            print(f"중복 제거: {len(existing_df) + len(new_df) - len(merged_df)}개")
        else:
            print("\n기존 데이터 없음. 신규 데이터만 사용")
            merged_df = new_df

        return merged_df

    def save_dataset(self, df, output_path="phishing_dataset_updated.csv"):
        """
        데이터셋 저장

        Args:
            df: 저장할 DataFrame
            output_path: 출력 파일 경로
        """
        print("\n" + "=" * 60)
        print("데이터셋 저장 중...")
        print("=" * 60)

        # 필요한 컬럼만 선택
        df_export = df[['message', 'is_phishing']].copy()

        # CSV 저장
        df_export.to_csv(output_path, index=False, encoding='utf-8-sig')

        print(f"\n저장 완료: {output_path}")
        print(f"총 샘플 수: {len(df_export)}")
        print(f"  - 피싱: {df_export['is_phishing'].sum()}개")
        print(f"  - 정상: {len(df_export) - df_export['is_phishing'].sum()}개")

        return output_path

    def generate_statistics(self, df):
        """
        데이터 통계 생성
        """
        print("\n" + "=" * 60)
        print("데이터 통계")
        print("=" * 60)

        print(f"\n총 샘플 수: {len(df)}")
        print(f"피싱: {df['is_phishing'].sum()} ({df['is_phishing'].sum()/len(df)*100:.1f}%)")
        print(f"정상: {len(df) - df['is_phishing'].sum()} ({(len(df) - df['is_phishing'].sum())/len(df)*100:.1f}%)")

        if 'risk_level' in df.columns:
            print("\n위험도 분포:")
            print(df['risk_level'].value_counts())

        if 'timestamp' in df.columns:
            print(f"\n데이터 기간:")
            print(f"  시작: {df['timestamp'].min()}")
            print(f"  종료: {df['timestamp'].max()}")

    def close(self):
        """MongoDB 연결 종료"""
        self.client.close()

def main():
    parser = argparse.ArgumentParser(description='프로덕션 데이터 추출 및 학습 데이터 생성')
    parser.add_argument('--mongo-uri',
                        default=os.getenv('MONGO_URI', 'mongodb://localhost:27017/lovechedule'),
                        help='MongoDB 연결 URI (환경 변수 MONGO_URI 사용 가능)')
    parser.add_argument('--days', type=int, default=30,
                        help='최근 N일간의 데이터 추출')
    parser.add_argument('--output', default='phishing_dataset_updated.csv',
                        help='출력 파일 경로')
    parser.add_argument('--merge', action='store_true',
                        help='기존 데이터와 병합')
    parser.add_argument('--balance', action='store_true',
                        help='데이터 균형 조정')

    args = parser.parse_args()

    print("=" * 60)
    print("프로덕션 데이터 추출 시작")
    print("=" * 60)
    print(f"\nMongoDB URI: {args.mongo_uri}")
    print(f"추출 기간: 최근 {args.days}일")

    try:
        # 추출기 초기화
        exporter = ProductionDataExporter(args.mongo_uri)

        # 데이터 추출
        df = exporter.export_verified_reports(days_back=args.days)

        if len(df) == 0:
            print("\n⚠️ 추출된 데이터가 없습니다. 종료합니다.")
            return

        # 기존 데이터와 병합
        if args.merge:
            df = exporter.merge_with_existing(df, "phishing_dataset.csv")

        # 데이터 균형 조정
        if args.balance:
            df = exporter.balance_dataset(df)

        # 통계 생성
        exporter.generate_statistics(df)

        # 저장
        output_path = exporter.save_dataset(df, args.output)

        print("\n" + "=" * 60)
        print("✅ 데이터 추출 완료!")
        print("=" * 60)
        print(f"\n다음 단계:")
        print(f"1. python train_model.py  # 모델 재학습")
        print(f"2. 새 모델을 서버에 배포")

        # 연결 종료
        exporter.close()

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
