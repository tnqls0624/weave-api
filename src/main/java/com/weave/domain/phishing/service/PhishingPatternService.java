package com.weave.domain.phishing.service;

import com.weave.domain.phishing.dto.PhishingPatternDto;
import com.weave.domain.phishing.entity.PhishingPattern;
import com.weave.domain.phishing.repository.PhishingPatternRepository;
import com.weave.global.exception.BusinessException;
import com.weave.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피싱 패턴 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhishingPatternService {

  private final PhishingPatternRepository patternRepository;
  private final PhishingNotificationService notificationService;

  /**
   * 패턴 목록 조회
   */
  @Cacheable(value = "phishingPatterns", key = "#category + '_' + #language + '_' + #activeOnly")
  public List<PhishingPatternDto> getPatterns(String category, String language,
      boolean activeOnly) {
    List<PhishingPattern> patterns;

    if (category != null && language != null) {
      patterns = patternRepository.findByCategoryAndLanguageAndIsActive(category, language,
          activeOnly);
    } else if (category != null) {
      patterns = patternRepository.findByCategoryAndIsActive(category, activeOnly);
    } else if (language != null) {
      patterns = patternRepository.findByLanguageAndIsActive(language, activeOnly);
    } else {
      patterns = activeOnly ? patternRepository.findByIsActiveTrue() : patternRepository.findAll();
    }

    return patterns.stream()
        .map(PhishingPatternDto::from)
        .collect(Collectors.toList());
  }

  /**
   * 패턴 상세 조회
   */
  public PhishingPatternDto getPattern(String patternId) {
    PhishingPattern pattern = patternRepository.findById(new ObjectId(patternId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    return PhishingPatternDto.from(pattern);
  }

  /**
   * 패턴 생성
   */
  @Transactional
  @CacheEvict(value = "phishingPatterns", allEntries = true)
  public PhishingPatternDto createPattern(PhishingPatternDto dto, String createdBy) {
    log.info("Creating new phishing pattern: {} by {}", dto.getName(), createdBy);

    // 중복 체크
    if (patternRepository.existsByName(dto.getName())) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    }

    // 패턴 유효성 검증
    validatePattern(dto);

    PhishingPattern pattern = new PhishingPattern();
    pattern.setName(dto.getName());
    pattern.setDescription(dto.getDescription());
    pattern.setCategory(dto.getCategory());
    pattern.setType(dto.getType());
    pattern.setPatterns(dto.getPatterns());
    pattern.setRiskLevel(dto.getRiskLevel());
    pattern.setWeight(dto.getWeight());
    pattern.setLanguage(dto.getLanguage());
    pattern.setIsActive(true);
    pattern.setCreatedBy(new ObjectId(createdBy));
    pattern.setCreatedAt(new Date());
    pattern.setUpdatedAt(new Date());

    // 초기값 설정
    pattern.setMatchCount(0);
    pattern.setFalsePositiveCount(0);
    pattern.setAccuracy(1.0);

    PhishingPattern saved = patternRepository.save(pattern);

    // 관리자들에게 알림
    notifyAdmins("create", saved.getName());

    return PhishingPatternDto.from(saved);
  }

  /**
   * 패턴 수정
   */
  @Transactional
  @CacheEvict(value = "phishingPatterns", allEntries = true)
  public PhishingPatternDto updatePattern(String patternId, PhishingPatternDto dto,
      String updatedBy) {
    log.info("Updating phishing pattern: {} by {}", patternId, updatedBy);

    PhishingPattern pattern = patternRepository.findById(new ObjectId(patternId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    // 패턴 유효성 검증
    validatePattern(dto);

    // 업데이트
    if (dto.getName() != null) {
      pattern.setName(dto.getName());
    }
    if (dto.getDescription() != null) {
      pattern.setDescription(dto.getDescription());
    }
    if (dto.getCategory() != null) {
      pattern.setCategory(dto.getCategory());
    }
    if (dto.getType() != null) {
      pattern.setType(dto.getType());
    }
    if (dto.getPatterns() != null) {
      pattern.setPatterns(dto.getPatterns());
    }
    if (dto.getRiskLevel() != null) {
      pattern.setRiskLevel(dto.getRiskLevel());
    }
    if (dto.getWeight() != null) {
      pattern.setWeight(dto.getWeight());
    }
    if (dto.getLanguage() != null) {
      pattern.setLanguage(dto.getLanguage());
    }
    if (dto.getIsActive() != null) {
      pattern.setIsActive(dto.getIsActive());
    }

    pattern.setUpdatedBy(new ObjectId(updatedBy));
    pattern.setUpdatedAt(new Date());

    PhishingPattern saved = patternRepository.save(pattern);

    // 관리자들에게 알림
    notifyAdmins("update", saved.getName());

    return PhishingPatternDto.from(saved);
  }

  /**
   * 패턴 삭제
   */
  @Transactional
  @CacheEvict(value = "phishingPatterns", allEntries = true)
  public void deletePattern(String patternId) {
    log.info("Deleting phishing pattern: {}", patternId);

    PhishingPattern pattern = patternRepository.findById(new ObjectId(patternId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    String patternName = pattern.getName();
    patternRepository.deleteById(new ObjectId(patternId));

    // 관리자들에게 알림
    notifyAdmins("delete", patternName);
  }

  /**
   * 패턴 활성화/비활성화
   */
  @Transactional
  @CacheEvict(value = "phishingPatterns", allEntries = true)
  public PhishingPatternDto togglePattern(String patternId) {
    PhishingPattern pattern = patternRepository.findById(new ObjectId(patternId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    pattern.setIsActive(!pattern.getIsActive());
    pattern.setUpdatedAt(new Date());

    PhishingPattern saved = patternRepository.save(pattern);
    return PhishingPatternDto.from(saved);
  }

  /**
   * 패턴 정확도 업데이트
   */
  @Transactional
  public void updatePatternAccuracy(String patternId, boolean isFalsePositive) {
    PhishingPattern pattern = patternRepository.findById(new ObjectId(patternId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    if (isFalsePositive) {
      pattern.setFalsePositiveCount(pattern.getFalsePositiveCount() + 1);
    }
    pattern.updateAccuracy();

    // 정확도가 너무 낮으면 비활성화
    if (pattern.getAccuracy() < 0.5) {
      log.warn("Pattern {} accuracy too low ({}), deactivating", pattern.getName(),
          pattern.getAccuracy());
      pattern.setIsActive(false);
    }

    patternRepository.save(pattern);
  }

  /**
   * 고위험 패턴 조회
   */
  @Cacheable(value = "highRiskPatterns")
  public List<PhishingPatternDto> getHighRiskPatterns() {
    List<PhishingPattern> patterns = patternRepository.findByRiskLevel("high");
    return patterns.stream()
        .filter(p -> p.getAccuracy() > 0.7)  // 정확도 70% 이상만
        .map(PhishingPatternDto::from)
        .collect(Collectors.toList());
  }

  /**
   * 패턴 통계 조회
   */
  public Map<String, Object> getPatternStatistics() {
    List<PhishingPattern> allPatterns = patternRepository.findAll();

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalPatterns", allPatterns.size());
    stats.put("activePatterns", allPatterns.stream().filter(PhishingPattern::getIsActive).count());

    // 카테고리별 통계
    Map<String, Long> categoryStats = allPatterns.stream()
        .collect(Collectors.groupingBy(PhishingPattern::getCategory, Collectors.counting()));
    stats.put("byCategory", categoryStats);

    // 언어별 통계
    Map<String, Long> languageStats = allPatterns.stream()
        .collect(Collectors.groupingBy(PhishingPattern::getLanguage, Collectors.counting()));
    stats.put("byLanguage", languageStats);

    // 평균 정확도
    double avgAccuracy = allPatterns.stream()
        .mapToDouble(PhishingPattern::getAccuracy)
        .average()
        .orElse(0.0);
    stats.put("averageAccuracy", avgAccuracy);

    // 가장 많이 사용된 패턴 Top 5
    List<PhishingPattern> topUsed = patternRepository.findByIsActiveTrueOrderByMatchCountDesc();
    stats.put("topUsedPatterns", topUsed.stream()
        .limit(5)
        .map(p -> {
          Map<String, Object> item = new HashMap<>();
          item.put("name", p.getName());
          item.put("matchCount", p.getMatchCount());
          item.put("accuracy", p.getAccuracy());
          return item;
        })
        .collect(Collectors.toList()));

    return stats;
  }

  /**
   * 패턴 일괄 가져오기 (임포트)
   */
  @Transactional
  @CacheEvict(value = "phishingPatterns", allEntries = true)
  public List<PhishingPatternDto> importPatterns(List<PhishingPatternDto> patterns,
      String importedBy) {
    log.info("Importing {} patterns by {}", patterns.size(), importedBy);

    List<PhishingPattern> imported = new ArrayList<>();

    for (PhishingPatternDto dto : patterns) {
      // 중복 체크
      if (patternRepository.existsByName(dto.getName())) {
        log.warn("Pattern {} already exists, skipping", dto.getName());
        continue;
      }

      try {
        validatePattern(dto);

        PhishingPattern pattern = new PhishingPattern();
        pattern.setName(dto.getName());
        pattern.setDescription(dto.getDescription());
        pattern.setCategory(dto.getCategory());
        pattern.setType(dto.getType());
        pattern.setPatterns(dto.getPatterns());
        pattern.setRiskLevel(dto.getRiskLevel());
        pattern.setWeight(dto.getWeight());
        pattern.setLanguage(dto.getLanguage());
        pattern.setIsActive(true);
        pattern.setCreatedBy(new ObjectId(importedBy));
        pattern.setCreatedAt(new Date());
        pattern.setUpdatedAt(new Date());
        pattern.setMatchCount(0);
        pattern.setFalsePositiveCount(0);
        pattern.setAccuracy(1.0);

        imported.add(pattern);
      } catch (Exception e) {
        log.error("Failed to import pattern: {}", dto.getName(), e);
      }
    }

    List<PhishingPattern> saved = patternRepository.saveAll(imported);
    log.info("Successfully imported {} patterns", saved.size());

    return saved.stream()
        .map(PhishingPatternDto::from)
        .collect(Collectors.toList());
  }

  /**
   * 패턴 유효성 검증
   */
  private void validatePattern(PhishingPatternDto dto) {
    if (dto.getName() == null || dto.getName().trim().isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (dto.getPatterns() == null || dto.getPatterns().isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    // 정규식 패턴 검증
    if ("regex".equals(dto.getType())) {
      for (String pattern : dto.getPatterns()) {
        try {
          java.util.regex.Pattern.compile(pattern);
        } catch (Exception e) {
          throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
              "Invalid regex pattern: " + pattern);
        }
      }
    }

    // 가중치 범위 검증
    if (dto.getWeight() != null && (dto.getWeight() < 0 || dto.getWeight() > 1)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
          "Weight must be between 0 and 1");
    }
  }

  /**
   * 관리자 알림
   */
  private void notifyAdmins(String action, String patternName) {
    try {
      // TODO: 실제 관리자 ID 목록을 가져와야 함
      List<String> adminIds = Arrays.asList("admin1", "admin2");
      notificationService.sendPatternUpdateNotification(adminIds, action, patternName);
    } catch (Exception e) {
      log.error("Failed to notify admins about pattern update", e);
    }
  }
}