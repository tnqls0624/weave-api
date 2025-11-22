package com.weave.domain.phishing.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Python ML 추론 서버와 통신하는 서비스
 * FastAPI 기반 추론 서버에 HTTP 요청을 보내어 피싱 탐지 수행
 */
@Slf4j
@Service
public class PhishingMLInferenceService {

  @Value("${phishing.ml.inference.url:http://localhost:8000}")
  private String inferenceUrl;

  @Value("${phishing.ml.inference.enabled:false}")
  private boolean enabled;

  @Value("${phishing.ml.inference.timeout:5000}")
  private int timeout;

  private RestTemplate restTemplate;

  @PostConstruct
  public void initialize() {
    if (!enabled) {
      log.info("ML 추론 서버가 비활성화되어 있습니다. 로컬 휴리스틱 분석만 사용합니다.");
      return;
    }

    // RestTemplate 초기화 (타임아웃 설정)
    this.restTemplate = new RestTemplateBuilder()
        .setConnectTimeout(java.time.Duration.ofMillis(timeout))
        .setReadTimeout(java.time.Duration.ofMillis(timeout))
        .build();

    log.info("ML 추론 서버 연결 설정 완료: {}", inferenceUrl);

    // 헬스체크
    checkHealth();
  }

  /**
   * 추론 서버 헬스체크
   */
  public boolean checkHealth() {
    if (!enabled) {
      return false;
    }

    try {
      String healthUrl = inferenceUrl + "/health";
      ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
          healthUrl,
          HealthResponse.class
      );

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        HealthResponse health = response.getBody();
        log.info("ML 추론 서버 상태: {}, 모델 로드: {}, 어휘 크기: {}",
            health.getStatus(),
            health.isModelLoaded(),
            health.getVocabularySize()
        );
        return health.isModelLoaded();
      }

      return false;
    } catch (Exception e) {
      log.warn("ML 추론 서버 헬스체크 실패: {}. 로컬 모델로 폴백합니다.", e.getMessage());
      return false;
    }
  }

  /**
   * 피싱 탐지 요청
   *
   * @param sender           발신자
   * @param message          메시지
   * @param sensitivityLevel 민감도 (high/medium/low)
   * @return 탐지 결과
   */
  public PredictionResult predict(String sender, String message, String sensitivityLevel) {
    if (!enabled) {
      throw new IllegalStateException("ML 추론 서버가 비활성화되어 있습니다");
    }

    try {
      String predictUrl = inferenceUrl + "/predict";

      // 요청 바디 생성
      PredictionRequest request = new PredictionRequest();
      request.setSender(sender);
      request.setMessage(message);
      request.setSensitivityLevel(sensitivityLevel != null ? sensitivityLevel : "medium");

      // HTTP 헤더 설정
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<PredictionRequest> httpEntity = new HttpEntity<>(request, headers);

      // POST 요청 전송
      log.debug("ML 추론 요청: sender={}, messageLength={}, sensitivity={}",
          sender, message.length(), sensitivityLevel);

      ResponseEntity<PredictionResponse> response = restTemplate.exchange(
          predictUrl,
          HttpMethod.POST,
          httpEntity,
          PredictionResponse.class
      );

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        PredictionResponse prediction = response.getBody();

        log.info("ML 추론 완료: isPhishing={}, riskScore={}, riskLevel={}",
            prediction.isPhishing(),
            prediction.getRiskScore(),
            prediction.getRiskLevel()
        );

        // 응답을 공통 결과 형식으로 변환
        PredictionResult result = new PredictionResult();
        result.setPhishing(prediction.isPhishing());
        result.setRiskScore(prediction.getRiskScore());
        result.setRiskLevel(prediction.getRiskLevel());
        result.setConfidence(prediction.getConfidence());
        result.setSource("python-ml-server");
        result.setModelVersion(prediction.getModelVersion());

        return result;
      } else {
        throw new RuntimeException("추론 서버 응답 오류: " + response.getStatusCode());
      }

    } catch (Exception e) {
      log.error("ML 추론 서버 호출 실패: {}", e.getMessage(), e);
      throw new RuntimeException("ML 추론 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 추론 서버 사용 가능 여부 확인
   */
  public boolean isAvailable() {
    return enabled && checkHealth();
  }

  /**
   * 추론 요청 DTO
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PredictionRequest {

    private String sender;
    private String message;

    @JsonProperty("sensitivity_level")
    private String sensitivityLevel;
  }

  /**
   * 추론 응답 DTO
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PredictionResponse {

    @JsonProperty("is_phishing")
    private boolean isPhishing;

    @JsonProperty("risk_score")
    private double riskScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    private double confidence;

    @JsonProperty("model_version")
    private String modelVersion;
  }

  /**
   * 헬스체크 응답 DTO
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HealthResponse {

    private String status;

    @JsonProperty("model_loaded")
    private boolean modelLoaded;

    @JsonProperty("model_path")
    private String modelPath;

    @JsonProperty("vocabulary_size")
    private int vocabularySize;
  }

  /**
   * 공통 탐지 결과
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PredictionResult {

    private boolean phishing;
    private double riskScore;
    private String riskLevel;
    private double confidence;
    private String source;  // "python-ml-server" or "local-model"
    private String modelVersion;
  }
}
