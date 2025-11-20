package com.weave.domain.phishing.service;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * TensorFlow 기반 피싱 탐지 ML 서비스
 */
@Slf4j
@Service
public class PhishingMLService {

  @Value("${phishing.model.path:models/phishing_detection_model}")
  private String modelPath;

  @Value("${phishing.model.enabled:false}")
  private boolean modelEnabled;

  private ZooModel<String, Float> model;
  private Predictor<String, Float> predictor;
  private Map<String, Integer> vocabulary;
  private static final int MAX_SEQUENCE_LENGTH = 200;
  private static final int VOCAB_SIZE = 10000;

  @PostConstruct
  public void initialize() {
    if (!modelEnabled) {
      log.info("피싱 ML 모델이 비활성화되어 있습니다. 휴리스틱 분석만 사용합니다.");
      return;
    }

    try {
      log.info("피싱 탐지 ML 모델 초기화 시작...");

      // 어휘 사전 로드
      this.vocabulary = loadVocabulary();

      // 모델 로드
      loadModel();

      log.info("피싱 탐지 ML 모델 초기화 완료");
    } catch (Exception e) {
      log.error("ML 모델 초기화 실패. 휴리스틱 분석만 사용합니다.", e);
      this.modelEnabled = false;
    }
  }

  /**
   * 모델 로드
   */
  private void loadModel() throws ModelNotFoundException, MalformedModelException, IOException {
    Path modelDir = Paths.get(modelPath);

    if (!modelDir.toFile().exists()) {
      log.warn("모델 경로가 존재하지 않습니다: {}. 기본 모델을 사용합니다.", modelPath);
      // 실제 환경에서는 사전 학습된 모델을 로드해야 함
      return;
    }

    // DJL Criteria로 TensorFlow 모델 로드
    Criteria<String, Float> criteria = Criteria.builder()
        .setTypes(String.class, Float.class)
        .optModelPath(modelDir)
        .optTranslator(new PhishingTranslator())
        .build();

    this.model = criteria.loadModel();
    this.predictor = model.newPredictor();
  }

  /**
   * 어휘 사전 로드
   */
  private Map<String, Integer> loadVocabulary() {
    // 실제로는 파일에서 로드하거나 DB에서 가져와야 함
    Map<String, Integer> vocab = new HashMap<>();

    // 한국어 피싱 관련 핵심 어휘
    String[] words = {
        "은행", "카드", "계좌", "비밀번호", "인증", "확인", "클릭", "링크",
        "입금", "송금", "대출", "택배", "배송", "도착", "당첨", "축하",
        "긴급", "중요", "안내", "통지", "만료", "정지", "차단", "해지",
        "국세청", "경찰", "검찰", "법원", "과태료", "벌금", "주민번호",
        "OTP", "보안", "개인정보", "신용", "한도", "승인", "결제"
    };

    for (int i = 0; i < words.length; i++) {
      vocab.put(words[i], i + 1);
    }

    return vocab;
  }

  /**
   * ML 모델로 피싱 점수 예측
   */
  public double predict(String message) {
    if (!modelEnabled || predictor == null) {
      log.debug("ML 모델이 비활성화되어 있습니다. 0을 반환합니다.");
      return 0.0;
    }

    try {
      Float score = predictor.predict(message);
      return score.doubleValue();
    } catch (TranslateException e) {
      log.error("ML 예측 실패", e);
      return 0.0;
    }
  }

  /**
   * 텍스트를 시퀀스로 변환
   */
  private int[] textToSequence(String text) {
    String[] tokens = text.toLowerCase().split("\\s+");
    int[] sequence = new int[MAX_SEQUENCE_LENGTH];

    for (int i = 0; i < Math.min(tokens.length, MAX_SEQUENCE_LENGTH); i++) {
      sequence[i] = vocabulary.getOrDefault(tokens[i], 0);
    }

    return sequence;
  }

  /**
   * 모델 사용 가능 여부
   */
  public boolean isModelAvailable() {
    return modelEnabled && predictor != null;
  }

  @PreDestroy
  public void cleanup() {
    if (predictor != null) {
      predictor.close();
    }
    if (model != null) {
      model.close();
    }
    log.info("피싱 ML 모델 리소스 정리 완료");
  }

  /**
   * DJL Translator - 텍스트를 모델 입력으로 변환
   */
  private class PhishingTranslator implements Translator<String, Float> {

    @Override
    public NDList processInput(TranslatorContext ctx, String input) {
      NDManager manager = ctx.getNDManager();

      // 텍스트를 시퀀스로 변환
      int[] sequence = textToSequence(input);

      // NDArray로 변환
      NDArray array = manager.create(sequence);
      array = array.expandDims(0); // 배치 차원 추가 [1, MAX_SEQUENCE_LENGTH]

      return new NDList(array);
    }

    @Override
    public Float processOutput(TranslatorContext ctx, NDList list) {
      NDArray output = list.singletonOrThrow();

      // 시그모이드 출력 값 (0~1 사이)
      float score = output.getFloat();

      return score;
    }

    @Override
    public Batchifier getBatchifier() {
      return Batchifier.STACK;
    }
  }

  /**
   * 모델 업데이트 (새 모델 배포 시 호출)
   */
  public void reloadModel() {
    try {
      log.info("피싱 탐지 모델 재로드 시작...");

      // 기존 리소스 정리
      cleanup();

      // 모델 재로드
      loadModel();

      log.info("피싱 탐지 모델 재로드 완료");
    } catch (Exception e) {
      log.error("모델 재로드 실패", e);
    }
  }

  /**
   * 배치 예측 (여러 메시지 한 번에 처리)
   */
  public Map<String, Double> predictBatch(java.util.List<String> messages) {
    Map<String, Double> results = new HashMap<>();

    for (String message : messages) {
      results.put(message, predict(message));
    }

    return results;
  }
}
