"""
한국어 SMS 피싱 데이터셋 생성기
실제 피싱 패턴을 기반으로 학습 데이터 생성
"""

import json
import random
import pandas as pd
from datetime import datetime

# 피싱 메시지 템플릿
PHISHING_TEMPLATES = {
    "financial": [
        "{은행}입니다. 계좌가 정지되었습니다. {url}에서 확인하세요.",
        "{은행} 고객님의 비밀번호가 만료되었습니다. {url}에서 재설정하세요.",
        "긴급! {은행} 계좌에서 {금액}원이 출금되었습니다. 확인: {url}",
        "{카드사} 고객님, 카드 승인 {금액}원. 본인이 아니면 즉시 {url} 클릭",
        "[{은행}] 보안 강화를 위해 계좌번호와 비밀번호를 {url}에서 재입력하세요.",
        "{은행} 고객님 계좌가 해킹 시도로 차단되었습니다. {url}에서 본인인증 필요",
        "무료 대출 한도 조회 {금액}원까지 가능. 신청: {url}",
        "[{카드사}] 연체금 {금액}원 발생. 24시간 내 {url}에서 납부하세요.",
    ],
    "government": [
        "[국세청] 환급금 {금액}원이 발생했습니다. {url}에서 계좌 입력하세요.",
        "경찰청입니다. 귀하의 명의로 범죄가 발생했습니다. {url} 확인 요망",
        "[법원] 귀하에게 소환장이 발부되었습니다. {url}에서 확인하세요.",
        "검찰청 사이버수사대입니다. 개인정보 유출 확인. {url}",
        "[국세청] 세금 체납 {금액}원. 압류 예정. {url}에서 즉시 납부",
        "코로나19 지원금 {금액}원 지급 대상자입니다. {url}",
        "[관세청] 해외 직구 물품 관세 미납. {url}에서 납부하세요.",
    ],
    "delivery": [
        "[{택배}] 배송 실패. 재배송 신청: {url}",
        "CJ대한통운 택배가 보관 중입니다. {url}에서 주소 확인하세요.",
        "[우체국] 소포 수령 실패. 3일 내 {url}에서 재배송 신청",
        "{택배} 배송 중 파손. 보상금 {금액}원 지급. {url}",
        "국제우편 세관 통관 보류. {url}에서 개인정보 입력 필요",
    ],
    "event": [
        "축하합니다! {회사} 이벤트 당첨 {금액}원 상품권. {url}",
        "[{쇼핑몰}] 무료 쿠폰 {금액}원권 선착순 지급. {url}",
        "SK텔레콤 고객님 포인트 {금액}원 소멸 예정. {url}에서 사용하세요.",
        "{회사} 창립기념 경품 당첨! 개인정보 입력: {url}",
    ],
}

# 정상 메시지 템플릿
NORMAL_TEMPLATES = [
    "안녕하세요. 내일 회의 일정 확인 부탁드립니다.",
    "오늘 저녁 약속 어때요?",
    "주말에 영화 보러 갈까요?",
    "생일 축하해요! 좋은 하루 보내세요.",
    "회의 자료 이메일로 보내드렸습니다.",
    "내일 오전 10시 미팅 잊지 마세요.",
    "점심 메뉴 추천 좀 해주세요.",
    "감사합니다. 잘 받았습니다.",
    "프로젝트 진행 상황 공유드립니다.",
    "다음 주 월요일 휴가 신청했어요.",
    "[네이버] 로그인 알림. 서울에서 로그인하셨습니다.",
    "[카카오톡] 새 메시지가 도착했습니다.",
    "[배민] 주문하신 음식이 배달 중입니다.",
    "[쿠팡] 오늘 도착 예정입니다.",
    "택배가 현관 앞에 놓여있어요.",
    "회원님, 이번 달 요금은 35,000원입니다.",
    "약속 시간 30분 전입니다.",
    "비밀번호가 변경되었습니다.",
    "출석체크 완료! 포인트 100점 적립.",
    "이벤트에 응모하셨습니다.",
]

# 데이터 생성 변수
BANKS = ["국민은행", "신한은행", "우리은행", "하나은행", "농협은행", "기업은행"]
CARDS = ["삼성카드", "신한카드", "현대카드", "KB국민카드", "롯데카드", "우리카드"]
DELIVERY = ["CJ대한통운", "한진택배", "로젠택배", "우체국택배", "쿠팡로켓배송"]
COMPANIES = ["삼성전자", "LG전자", "SK텔레콤", "KT", "네이버", "카카오"]
SHOPPING = ["쿠팡", "네이버쇼핑", "11번가", "G마켓", "옥션"]

PHISHING_URLS = [
    "http://bit.ly/abc123",
    "http://tinyurl.com/xyz789",
    "http://me2.do/5aBcD",
    "http://han.gl/qwerty",
    "http://short.link/phish",
    "http://naver.co.kr",  # 타이포스쿼팅
    "http://kakoa.com",    # 타이포스쿼팅
    "http://kbstar.co",    # 타이포스쿼팅
]

def generate_amount():
    """랜덤 금액 생성"""
    amounts = [
        "50,000", "100,000", "300,000", "500,000", "1,000,000",
        "2,500,000", "5,000,000", "10,000,000"
    ]
    return random.choice(amounts)

def generate_phishing_message():
    """피싱 메시지 생성"""
    category = random.choice(list(PHISHING_TEMPLATES.keys()))
    template = random.choice(PHISHING_TEMPLATES[category])

    message = template.format(
        은행=random.choice(BANKS),
        카드사=random.choice(CARDS),
        택배=random.choice(DELIVERY),
        회사=random.choice(COMPANIES),
        쇼핑몰=random.choice(SHOPPING),
        금액=generate_amount(),
        url=random.choice(PHISHING_URLS)
    )

    return message, category

def generate_normal_message():
    """정상 메시지 생성"""
    return random.choice(NORMAL_TEMPLATES), "normal"

def generate_dataset(num_phishing=5000, num_normal=5000):
    """전체 데이터셋 생성"""
    print(f"데이터셋 생성 시작: 피싱 {num_phishing}개, 정상 {num_normal}개")

    data = []

    # 피싱 메시지 생성
    print("피싱 메시지 생성 중...")
    for i in range(num_phishing):
        message, category = generate_phishing_message()
        data.append({
            "message": message,
            "is_phishing": 1,
            "category": category,
            "sender": generate_sender(is_phishing=True)
        })

        if (i + 1) % 1000 == 0:
            print(f"  {i + 1}/{num_phishing} 완료")

    # 정상 메시지 생성
    print("정상 메시지 생성 중...")
    for i in range(num_normal):
        message, category = generate_normal_message()
        data.append({
            "message": message,
            "is_phishing": 0,
            "category": category,
            "sender": generate_sender(is_phishing=False)
        })

        if (i + 1) % 1000 == 0:
            print(f"  {i + 1}/{num_normal} 완료")

    # 데이터프레임 생성 및 셔플
    df = pd.DataFrame(data)
    df = df.sample(frac=1, random_state=42).reset_index(drop=True)

    print(f"\n총 {len(df)}개 메시지 생성 완료")
    print(f"피싱: {df['is_phishing'].sum()}개")
    print(f"정상: {len(df) - df['is_phishing'].sum()}개")

    return df

def generate_sender(is_phishing=False):
    """발신자 번호 생성"""
    if is_phishing:
        # 의심스러운 발신자
        patterns = [
            f"+{random.randint(1, 99)}{random.randint(1000000000, 9999999999)}",  # 국제번호
            f"{random.randint(1000, 9999)}",  # 짧은 번호
            f"[Web발신]{random.randint(1000000000, 9999999999)}",  # 웹발신
            f"010{random.randint(10000000, 99999999)}",  # 정상처럼 보이는 번호
        ]
        return random.choice(patterns)
    else:
        # 정상 발신자
        patterns = [
            f"010{random.randint(10000000, 99999999)}",  # 휴대폰
            f"02-{random.randint(1000, 9999)}-{random.randint(1000, 9999)}",  # 서울 유선
            f"1588-{random.randint(1000, 9999)}",  # 대표번호
            "카카오톡",
            "네이버",
            "배달의민족",
        ]
        return random.choice(patterns)

def save_dataset(df, output_path="phishing_dataset.csv"):
    """데이터셋 저장"""
    df.to_csv(output_path, index=False, encoding='utf-8-sig')
    print(f"\n데이터셋 저장 완료: {output_path}")

    # 통계 출력
    print("\n=== 데이터셋 통계 ===")
    print(f"전체 메시지 수: {len(df)}")
    print(f"피싱 메시지: {df['is_phishing'].sum()} ({df['is_phishing'].sum()/len(df)*100:.1f}%)")
    print(f"정상 메시지: {len(df) - df['is_phishing'].sum()} ({(len(df) - df['is_phishing'].sum())/len(df)*100:.1f}%)")
    print("\n카테고리별 분포:")
    print(df['category'].value_counts())

    # 샘플 출력
    print("\n=== 피싱 메시지 샘플 ===")
    print(df[df['is_phishing'] == 1].sample(5)[['message', 'category']].to_string(index=False))

    print("\n=== 정상 메시지 샘플 ===")
    print(df[df['is_phishing'] == 0].sample(5)[['message', 'category']].to_string(index=False))

if __name__ == "__main__":
    print("=" * 60)
    print("한국어 SMS 피싱 데이터셋 생성기")
    print("=" * 60)

    # 데이터셋 생성
    df = generate_dataset(num_phishing=5000, num_normal=5000)

    # 저장
    save_dataset(df, "phishing_dataset.csv")

    print("\n생성 완료!")
