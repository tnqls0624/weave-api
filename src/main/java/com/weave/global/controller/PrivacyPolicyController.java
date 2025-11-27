package com.weave.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/privacy-policy")
@Tag(name = "POLICY", description = "개인정보처리방침 API")
public class PrivacyPolicyController {

  @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
  @Operation(summary = "개인정보처리방침 조회", description = "개인정보처리방침 HTML을 반환합니다.")
  public String getPrivacyPolicy() {
    return """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>개인정보처리방침 - 모두의캘린더</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    padding: 20px;
                    background-color: #fff;
                }
                .container {
                    max-width: 800px;
                    margin: 0 auto;
                }
                h1 {
                    font-size: 24px;
                    font-weight: 700;
                    color: #111;
                    margin-bottom: 8px;
                    text-align: center;
                }
                .effective-date {
                    text-align: center;
                    color: #666;
                    font-size: 14px;
                    margin-bottom: 32px;
                }
                h2 {
                    font-size: 18px;
                    font-weight: 600;
                    color: #222;
                    margin-top: 28px;
                    margin-bottom: 12px;
                    padding-bottom: 8px;
                    border-bottom: 2px solid #007AFF;
                }
                h3 {
                    font-size: 16px;
                    font-weight: 600;
                    color: #333;
                    margin-top: 20px;
                    margin-bottom: 8px;
                }
                p {
                    margin-bottom: 12px;
                    font-size: 15px;
                }
                ul, ol {
                    margin-left: 20px;
                    margin-bottom: 12px;
                }
                li {
                    margin-bottom: 8px;
                    font-size: 15px;
                }
                .highlight {
                    background-color: #f0f7ff;
                    padding: 16px;
                    border-radius: 8px;
                    margin: 16px 0;
                }
                .contact-info {
                    background-color: #f9f9f9;
                    padding: 20px;
                    border-radius: 12px;
                    margin-top: 24px;
                }
                .contact-info h3 {
                    margin-top: 0;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 16px 0;
                }
                th, td {
                    border: 1px solid #ddd;
                    padding: 12px;
                    text-align: left;
                    font-size: 14px;
                }
                th {
                    background-color: #f5f5f5;
                    font-weight: 600;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>개인정보처리방침</h1>
                <p class="effective-date">시행일: 2024년 1월 1일</p>

                <p>모두의캘린더(이하 "회사")는 이용자의 개인정보를 중요시하며, 「개인정보 보호법」을 준수하고 있습니다. 회사는 개인정보처리방침을 통하여 이용자가 제공하는 개인정보가 어떠한 용도와 방식으로 이용되고 있으며, 개인정보 보호를 위해 어떠한 조치가 취해지고 있는지 알려드립니다.</p>

                <h2>제1조 (개인정보의 수집 및 이용 목적)</h2>
                <p>회사는 다음의 목적을 위하여 개인정보를 처리합니다. 처리하고 있는 개인정보는 다음의 목적 이외의 용도로는 이용되지 않으며, 이용 목적이 변경되는 경우에는 별도의 동의를 받는 등 필요한 조치를 이행할 예정입니다.</p>
                <ul>
                    <li><strong>회원 가입 및 관리:</strong> 회원제 서비스 이용에 따른 본인확인, 개인식별, 불량회원의 부정 이용 방지, 가입의사 확인, 연령확인, 불만처리 등 민원처리, 고지사항 전달</li>
                    <li><strong>서비스 제공:</strong> 일정 관리, 워크스페이스 기능 제공, 위치 공유 서비스, 푸시 알림 발송</li>
                    <li><strong>서비스 개선:</strong> 서비스 이용 통계 분석, 서비스 개선 및 신규 서비스 개발</li>
                </ul>

                <h2>제2조 (수집하는 개인정보 항목)</h2>
                <p>회사는 서비스 제공을 위해 다음과 같은 개인정보를 수집합니다.</p>

                <table>
                    <thead>
                        <tr>
                            <th>수집 시점</th>
                            <th>수집 항목</th>
                            <th>수집 방법</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>회원가입 시</td>
                            <td>이메일, 이름, 프로필 사진(선택), 생년월일(선택)</td>
                            <td>소셜 로그인(카카오, 애플)</td>
                        </tr>
                        <tr>
                            <td>서비스 이용 시</td>
                            <td>위치정보(선택), 기기정보(푸시 토큰)</td>
                            <td>앱 사용 중 자동 수집</td>
                        </tr>
                    </tbody>
                </table>

                <h2>제3조 (개인정보의 보유 및 이용기간)</h2>
                <p>회사는 법령에 따른 개인정보 보유·이용기간 또는 정보주체로부터 개인정보를 수집 시에 동의 받은 개인정보 보유·이용기간 내에서 개인정보를 처리·보유합니다.</p>
                <ul>
                    <li><strong>회원 정보:</strong> 회원 탈퇴 시까지 (탈퇴 후 즉시 파기)</li>
                    <li><strong>위치 정보:</strong> 수집 후 24시간 경과 시 자동 삭제</li>
                    <li><strong>서비스 이용 기록:</strong> 1년간 보관 후 파기</li>
                </ul>

                <h2>제4조 (개인정보의 제3자 제공)</h2>
                <p>회사는 원칙적으로 이용자의 개인정보를 제3자에게 제공하지 않습니다. 다만, 다음의 경우에는 예외로 합니다.</p>
                <ul>
                    <li>이용자가 사전에 동의한 경우</li>
                    <li>법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우</li>
                </ul>

                <h2>제5조 (개인정보의 파기)</h2>
                <p>회사는 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 지체 없이 해당 개인정보를 파기합니다.</p>
                <ul>
                    <li><strong>전자적 파일:</strong> 복원이 불가능한 방법으로 영구 삭제</li>
                    <li><strong>종이 문서:</strong> 분쇄기로 분쇄하거나 소각</li>
                </ul>

                <h2>제6조 (정보주체의 권리·의무 및 행사방법)</h2>
                <p>이용자는 개인정보주체로서 다음과 같은 권리를 행사할 수 있습니다.</p>
                <ul>
                    <li>개인정보 열람 요구</li>
                    <li>오류 등이 있을 경우 정정 요구</li>
                    <li>삭제 요구</li>
                    <li>처리정지 요구</li>
                </ul>
                <p>위 권리 행사는 앱 내 설정 메뉴 또는 아래 연락처를 통해 가능합니다.</p>

                <h2>제7조 (개인정보의 안전성 확보 조치)</h2>
                <p>회사는 개인정보의 안전성 확보를 위해 다음과 같은 조치를 취하고 있습니다.</p>
                <ul>
                    <li><strong>관리적 조치:</strong> 내부관리계획 수립·시행, 직원 교육</li>
                    <li><strong>기술적 조치:</strong> 개인정보처리시스템 등의 접근권한 관리, 암호화 기술 적용, 보안프로그램 설치</li>
                    <li><strong>물리적 조치:</strong> 전산실, 자료보관실 등의 접근통제</li>
                </ul>

                <h2>제8조 (위치정보의 보호)</h2>
                <p>회사는 「위치정보의 보호 및 이용 등에 관한 법률」에 따라 위치정보를 보호합니다.</p>
                <ul>
                    <li>위치정보 수집은 이용자의 명시적 동의 하에만 이루어집니다.</li>
                    <li>수집된 위치정보는 워크스페이스 멤버 간 위치 공유 목적으로만 사용됩니다.</li>
                    <li>이용자는 언제든지 위치정보 수집을 중단할 수 있습니다.</li>
                </ul>

                <h2>제9조 (개인정보 보호책임자)</h2>
                <div class="contact-info">
                    <h3>개인정보 보호책임자</h3>
                    <p><strong>담당자:</strong> 모두의캘린더 개인정보보호팀</p>
                    <p><strong>이메일:</strong> privacy@weave-calendar.com</p>
                </div>

                <h2>제10조 (개인정보처리방침 변경)</h2>
                <p>이 개인정보처리방침은 시행일로부터 적용되며, 법령 및 방침에 따른 변경내용의 추가, 삭제 및 정정이 있는 경우에는 변경사항의 시행 7일 전부터 공지사항을 통하여 고지할 것입니다.</p>

                <div class="highlight">
                    <p><strong>본 방침은 2024년 1월 1일부터 시행됩니다.</strong></p>
                </div>
            </div>
        </body>
        </html>
        """;
  }
}
