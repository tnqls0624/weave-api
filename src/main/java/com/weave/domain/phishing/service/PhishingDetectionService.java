package com.weave.domain.phishing.service;

import com.weave.domain.phishing.controller.PhishingController.PhishingDetectionResult;
import com.weave.domain.phishing.entity.PhishingPattern;
import com.weave.domain.phishing.repository.PhishingPatternRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 피싱 탐지 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhishingDetectionService {

  private final PhishingPatternRepository patternRepository;

  /**
   * 피싱 탐지 수행
   */
  public PhishingDetectionResult detectPhishing(String sender, String message, String sensitivityLevel) {
    log.info("피싱 탐지 시작 - 발신자: {}, 민감도: {}", sender, sensitivityLevel);

    PhishingDetectionResult result = new PhishingDetectionResult();
    List<String> detectionReasons = new ArrayList<>();
    double totalScore = 0.0;
    String phishingType = null;

    // 1. 패턴 매칭
    List<PhishingPattern> activePatterns = patternRepository.findByIsActiveTrue();

    for (PhishingPattern pattern : activePatterns) {
      if (matchesPattern(message, sender, pattern)) {
        totalScore += pattern.getWeight();
        detectionReasons.add(pattern.getDescription());

        if (phishingType == null) {
          phishingType = pattern.getCategory();
        }

        // 패턴 사용 카운트 증가
        pattern.setMatchCount(pattern.getMatchCount() + 1);
        pattern.setLastUsedAt(new java.util.Date());
        patternRepository.save(pattern);
      }
    }

    // 2. 휴리스틱 분석
    totalScore += analyzeHeuristics(sender, message, detectionReasons);

    // 3. URL 분석
    totalScore += analyzeUrls(message, detectionReasons);

    // 4. 민감도 조정
    totalScore = adjustBySensitivity(totalScore, sensitivityLevel);

    // 5. 결과 설정
    result.setRiskScore(Math.min(totalScore, 1.0));
    result.setRiskLevel(calculateRiskLevel(totalScore));
    result.setPhishing(isPhishing(totalScore, sensitivityLevel));
    result.setDetectionReasons(detectionReasons);
    result.setPhishingType(phishingType != null ? phishingType : "unknown");
    result.setConfidence(calculateConfidence(totalScore, detectionReasons.size()));

    log.info("피싱 탐지 완료 - 점수: {}, 레벨: {}, 피싱 여부: {}",
      result.getRiskScore(), result.getRiskLevel(), result.isPhishing());

    return result;
  }

  /**
   * 패턴 매칭 확인
   */
  private boolean matchesPattern(String message, String sender, PhishingPattern pattern) {
    if (pattern.getPatterns() == null || pattern.getPatterns().isEmpty()) {
      return false;
    }

    String combinedText = sender + " " + message;

    for (String patternStr : pattern.getPatterns()) {
      try {
        if ("regex".equals(pattern.getType())) {
          Pattern regex = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
          Matcher matcher = regex.matcher(combinedText);
          if (matcher.find()) {
            return true;
          }
        } else if ("keyword".equals(pattern.getType())) {
          if (combinedText.toLowerCase().contains(patternStr.toLowerCase())) {
            return true;
          }
        }
      } catch (Exception e) {
        log.error("패턴 매칭 오류: {}", e.getMessage());
      }
    }

    return false;
  }

  /**
   * 휴리스틱 분석
   */
  private double analyzeHeuristics(String sender, String message, List<String> reasons) {
    double score = 0.0;

    // 발신자 분석
    if (isSuspiciousSender(sender)) {
      score += 0.2;
      reasons.add("의심스러운 발신자");
    }

    // 긴급성 키워드
    if (containsUrgencyKeywords(message)) {
      score += 0.15;
      reasons.add("긴급성 유도 표현");
    }

    // 개인정보 요구
    if (containsPersonalInfoRequest(message)) {
      score += 0.3;
      reasons.add("개인정보 요구");
    }

    // 금액 언급
    if (containsMoneyReference(message)) {
      score += 0.15;
      reasons.add("금액 언급");
    }

    return score;
  }

  /**
   * URL 분석
   */
  private double analyzeUrls(String message, List<String> reasons) {
    double score = 0.0;

    // URL 패턴 찾기
    Pattern urlPattern = Pattern.compile(
      "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+",
      Pattern.CASE_INSENSITIVE
    );
    Matcher matcher = urlPattern.matcher(message);

    while (matcher.find()) {
      String url = matcher.group();

      // 단축 URL
      if (isShortUrl(url)) {
        score += 0.2;
        if (!reasons.contains("단축 URL 사용")) {
          reasons.add("단축 URL 사용");
        }
      }

      // 의심스러운 도메인
      if (isSuspiciousDomain(url)) {
        score += 0.25;
        if (!reasons.contains("의심스러운 도메인")) {
          reasons.add("의심스러운 도메인");
        }
      }
    }

    return score;
  }

  /**
   * 민감도 조정
   */
  private double adjustBySensitivity(double score, String sensitivity) {
    if ("high".equals(sensitivity)) {
      return score * 1.2;
    } else if ("low".equals(sensitivity)) {
      return score * 0.8;
    }
    return score;
  }

  /**
   * 위험 수준 계산
   */
  private String calculateRiskLevel(double score) {
    if (score >= 0.7) {
      return "high";
    } else if (score >= 0.4) {
      return "medium";
    }
    return "low";
  }

  /**
   * 피싱 여부 판단
   */
  private boolean isPhishing(double score, String sensitivity) {
    double threshold;
    switch (sensitivity) {
      case "high":
        threshold = 0.3;
        break;
      case "low":
        threshold = 0.6;
        break;
      default:
        threshold = 0.45;
    }
    return score >= threshold;
  }

  /**
   * 신뢰도 계산
   */
  private double calculateConfidence(double score, int reasonCount) {
    double confidence = score;
    confidence += reasonCount * 0.05;
    return Math.min(confidence, 1.0);
  }

  /**
   * 의심스러운 발신자 확인
   */
  private boolean isSuspiciousSender(String sender) {
    // 국제 전화번호
    if (sender.startsWith("+") && !sender.startsWith("+82")) {
      return true;
    }

    // 웹발신
    if (sender.contains("Web") || sender.contains("[Web]")) {
      return true;
    }

    // 너무 짧은 번호
    if (sender.matches("\\d+") && sender.length() < 7) {
      return true;
    }

    return false;
  }

  /**
   * 긴급성 키워드 확인
   */
  private boolean containsUrgencyKeywords(String message) {
    String[] keywords = {"긴급", "즉시", "당장", "오늘", "24시간", "마감", "서둘러"};
    for (String keyword : keywords) {
      if (message.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 개인정보 요구 확인
   */
  private boolean containsPersonalInfoRequest(String message) {
    String[] keywords = {"주민번호", "비밀번호", "계좌번호", "카드번호", "인증번호", "OTP"};
    for (String keyword : keywords) {
      if (message.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 금액 언급 확인
   */
  private boolean containsMoneyReference(String message) {
    return message.matches(".*\\d+[,\\d]*\\s*원.*") ||
           message.contains("입금") ||
           message.contains("송금") ||
           message.contains("환급");
  }

  /**
   * 단축 URL 확인
   */
  private boolean isShortUrl(String url) {
    String[] shorteners = {"bit.ly", "tinyurl", "me2.do", "han.gl", "vo.la"};
    for (String shortener : shorteners) {
      if (url.contains(shortener)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 의심스러운 도메인 확인
   */
  private boolean isSuspiciousDomain(String url) {
    // 타이포스쿼팅
    String[] typos = {"naver.co", "navor.com", "kakoa.com", "samsnug.com"};
    for (String typo : typos) {
      if (url.contains(typo)) {
        return true;
      }
    }

    // 의심스러운 TLD
    String[] suspiciousTlds = {".tk", ".ml", ".ga", ".cf"};
    for (String tld : suspiciousTlds) {
      if (url.contains(tld)) {
        return true;
      }
    }

    return false;
  }

  /**
   * 패턴 정확도 업데이트
   */
  public void updatePatternAccuracy(List<String> detectionReasons, boolean isFalsePositive) {
    if (detectionReasons == null || detectionReasons.isEmpty()) {
      return;
    }

    try {
      List<PhishingPattern> patterns = patternRepository.findByDescriptionIn(detectionReasons);

      for (PhishingPattern pattern : patterns) {
        if (isFalsePositive) {
          pattern.setFalsePositiveCount(pattern.getFalsePositiveCount() + 1);
        }
        pattern.updateAccuracy();
        patternRepository.save(pattern);
      }
    } catch (Exception e) {
      log.error("패턴 정확도 업데이트 실패", e);
    }
  }
}