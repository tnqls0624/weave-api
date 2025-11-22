"""
지속적 학습 파이프라인
프로덕션 데이터 → 모델 재학습 → 배포 자동화
"""

import os
import sys
import shutil
import subprocess
from datetime import datetime
import json
import argparse

class ContinuousLearningPipeline:
    def __init__(self, mongo_uri=None):
        # 환경 변수 우선, 없으면 기본값
        if mongo_uri is None:
            mongo_uri = os.getenv('MONGO_URI', 'mongodb://localhost:27017/lovechedule')

        self.mongo_uri = mongo_uri
        self.timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        self.backup_dir = f"backups/model_backup_{self.timestamp}"
        self.training_log = f"logs/training_{self.timestamp}.log"

    def setup_directories(self):
        """디렉토리 구조 생성"""
        print("=" * 60)
        print("디렉토리 설정 중...")
        print("=" * 60)

        os.makedirs("backups", exist_ok=True)
        os.makedirs("logs", exist_ok=True)
        os.makedirs(self.backup_dir, exist_ok=True)

        print(f"백업 디렉토리: {self.backup_dir}")
        print(f"로그 파일: {self.training_log}")

    def backup_current_model(self):
        """현재 모델 백업"""
        print("\n" + "=" * 60)
        print("현재 모델 백업 중...")
        print("=" * 60)

        model_dir = "../models/phishing_detection_model"

        if os.path.exists(model_dir):
            backup_model_dir = os.path.join(self.backup_dir, "model")
            shutil.copytree(model_dir, backup_model_dir)
            print(f"✅ 모델 백업 완료: {backup_model_dir}")

            # 백업 메타데이터 저장
            metadata = {
                "backup_time": self.timestamp,
                "model_path": model_dir,
                "backup_path": backup_model_dir
            }
            with open(os.path.join(self.backup_dir, "metadata.json"), 'w') as f:
                json.dump(metadata, f, indent=2)
        else:
            print("⚠️ 기존 모델이 없습니다")

    def export_production_data(self, days=30):
        """프로덕션 데이터 추출"""
        print("\n" + "=" * 60)
        print("Step 1: 프로덕션 데이터 추출")
        print("=" * 60)

        cmd = [
            "python",
            "export_production_data.py",
            "--mongo-uri", self.mongo_uri,
            "--days", str(days),
            "--output", "phishing_dataset_updated.csv",
            "--merge",  # 기존 데이터와 병합
            "--balance"  # 데이터 균형 조정
        ]

        result = subprocess.run(cmd, capture_output=True, text=True)

        if result.returncode != 0:
            print(f"❌ 데이터 추출 실패:")
            print(result.stderr)
            return False

        print(result.stdout)
        return True

    def check_data_quality(self):
        """데이터 품질 확인"""
        print("\n" + "=" * 60)
        print("데이터 품질 확인 중...")
        print("=" * 60)

        import pandas as pd

        try:
            df = pd.read_csv("phishing_dataset_updated.csv", encoding='utf-8-sig')

            total = len(df)
            phishing = df['is_phishing'].sum()
            normal = total - phishing

            print(f"총 샘플: {total}")
            print(f"피싱: {phishing} ({phishing/total*100:.1f}%)")
            print(f"정상: {normal} ({normal/total*100:.1f}%)")

            # 최소 데이터 요구사항 확인
            if total < 100:
                print("⚠️ 경고: 데이터가 너무 적습니다 (최소 100개 권장)")
                return False

            if phishing < 20 or normal < 20:
                print("⚠️ 경고: 클래스 불균형이 심합니다")
                return False

            print("✅ 데이터 품질 통과")
            return True

        except Exception as e:
            print(f"❌ 데이터 품질 확인 실패: {e}")
            return False

    def train_model(self):
        """모델 재학습"""
        print("\n" + "=" * 60)
        print("Step 2: 모델 재학습")
        print("=" * 60)

        # 업데이트된 데이터셋으로 교체
        if os.path.exists("phishing_dataset_updated.csv"):
            shutil.copy("phishing_dataset_updated.csv", "phishing_dataset.csv")
            print("✅ 학습 데이터셋 업데이트 완료")

        cmd = ["python", "train_model.py"]

        with open(self.training_log, 'w') as log_file:
            result = subprocess.run(
                cmd,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                text=True
            )

        if result.returncode != 0:
            print(f"❌ 모델 학습 실패. 로그 확인: {self.training_log}")
            return False

        print(f"✅ 모델 학습 완료. 로그: {self.training_log}")
        return True

    def validate_new_model(self):
        """새 모델 검증"""
        print("\n" + "=" * 60)
        print("Step 3: 새 모델 검증")
        print("=" * 60)

        model_dir = "../models/phishing_detection_model"

        # 모델 파일 존재 확인
        if not os.path.exists(model_dir):
            print("❌ 모델 디렉토리가 없습니다")
            return False

        required_files = ["saved_model.pb", "vocabulary.json", "config.json"]
        for file in required_files:
            file_path = os.path.join(model_dir, file)
            if not os.path.exists(file_path):
                print(f"❌ 필수 파일 누락: {file}")
                return False

        print("✅ 모델 파일 검증 통과")

        # 간단한 예측 테스트
        try:
            import tensorflow as tf
            import json

            # 모델 로드
            model = tf.saved_model.load(model_dir)
            print("✅ 모델 로드 성공")

            # Vocabulary 로드
            with open(os.path.join(model_dir, "vocabulary.json"), 'r', encoding='utf-8') as f:
                vocab = json.load(f)

            print(f"✅ 어휘 사전 로드 성공 (크기: {len(vocab)})")

            return True

        except Exception as e:
            print(f"❌ 모델 검증 실패: {e}")
            return False

    def deploy_model(self, auto_deploy=False):
        """모델 배포"""
        print("\n" + "=" * 60)
        print("Step 4: 모델 배포")
        print("=" * 60)

        if not auto_deploy:
            response = input("새 모델을 배포하시겠습니까? (y/n): ")
            if response.lower() != 'y':
                print("배포를 취소했습니다")
                return False

        model_dir = "../models/phishing_detection_model"
        deploy_timestamp = os.path.join(model_dir, "deployed_at.txt")

        # 배포 타임스탬프 기록
        with open(deploy_timestamp, 'w') as f:
            f.write(datetime.now().isoformat())

        print(f"✅ 모델 배포 완료: {model_dir}")
        print("\n서버를 재시작하여 새 모델을 로드하세요:")
        print("  cd ~/Desktop/Project/lovechedule-api")
        print("  ./gradlew bootRun")

        return True

    def rollback_model(self):
        """모델 롤백 (백업에서 복원)"""
        print("\n" + "=" * 60)
        print("모델 롤백 중...")
        print("=" * 60)

        backup_model_dir = os.path.join(self.backup_dir, "model")

        if not os.path.exists(backup_model_dir):
            print("❌ 백업 모델을 찾을 수 없습니다")
            return False

        model_dir = "../models/phishing_detection_model"

        # 현재 모델 삭제
        if os.path.exists(model_dir):
            shutil.rmtree(model_dir)

        # 백업에서 복원
        shutil.copytree(backup_model_dir, model_dir)

        print(f"✅ 모델 롤백 완료: {backup_model_dir} → {model_dir}")
        return True

    def generate_report(self, success=True):
        """학습 리포트 생성"""
        print("\n" + "=" * 60)
        print("학습 리포트 생성 중...")
        print("=" * 60)

        report = {
            "timestamp": self.timestamp,
            "mongo_uri": self.mongo_uri,
            "success": success,
            "backup_dir": self.backup_dir,
            "training_log": self.training_log,
            "model_dir": "../models/phishing_detection_model"
        }

        report_file = f"logs/report_{self.timestamp}.json"
        with open(report_file, 'w') as f:
            json.dump(report, f, indent=2)

        print(f"✅ 리포트 저장: {report_file}")

        return report_file

    def run_pipeline(self, days=30, auto_deploy=False):
        """전체 파이프라인 실행"""
        print("=" * 60)
        print("지속적 학습 파이프라인 시작")
        print("=" * 60)
        print(f"시작 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

        success = True

        try:
            # 1. 디렉토리 설정
            self.setup_directories()

            # 2. 현재 모델 백업
            self.backup_current_model()

            # 3. 프로덕션 데이터 추출
            if not self.export_production_data(days):
                print("\n❌ 데이터 추출 실패. 파이프라인 중단.")
                success = False
                return

            # 4. 데이터 품질 확인
            if not self.check_data_quality():
                print("\n⚠️ 데이터 품질 미달. 계속 진행하시겠습니까? (y/n): ", end='')
                response = input()
                if response.lower() != 'y':
                    print("파이프라인 중단.")
                    success = False
                    return

            # 5. 모델 재학습
            if not self.train_model():
                print("\n❌ 모델 학습 실패")
                success = False
                self.rollback_model()
                return

            # 6. 새 모델 검증
            if not self.validate_new_model():
                print("\n❌ 모델 검증 실패")
                success = False
                self.rollback_model()
                return

            # 7. 모델 배포
            if not self.deploy_model(auto_deploy):
                print("\n배포가 취소되었습니다")
                success = False
                return

            print("\n" + "=" * 60)
            print("✅ 파이프라인 완료!")
            print("=" * 60)

        except Exception as e:
            print(f"\n❌ 파이프라인 실행 중 오류 발생: {e}")
            import traceback
            traceback.print_exc()
            success = False

            # 롤백 시도
            print("\n모델을 이전 버전으로 롤백합니다...")
            self.rollback_model()

        finally:
            # 리포트 생성
            self.generate_report(success)

            print(f"\n종료 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

def main():
    parser = argparse.ArgumentParser(description='지속적 학습 파이프라인')
    parser.add_argument('--mongo-uri',
                        default=os.getenv('MONGO_URI', 'mongodb://localhost:27017/lovechedule'),
                        help='MongoDB 연결 URI (환경 변수 MONGO_URI 사용 가능)')
    parser.add_argument('--days', type=int, default=30,
                        help='최근 N일간의 데이터 사용')
    parser.add_argument('--auto-deploy', action='store_true',
                        help='자동 배포 (확인 없이)')

    args = parser.parse_args()

    pipeline = ContinuousLearningPipeline(mongo_uri=args.mongo_uri)
    pipeline.run_pipeline(days=args.days, auto_deploy=args.auto_deploy)

if __name__ == "__main__":
    main()
