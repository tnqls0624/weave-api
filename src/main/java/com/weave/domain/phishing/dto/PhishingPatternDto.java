package com.weave.domain.phishing.dto;

import com.weave.domain.phishing.entity.PhishingPattern;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

/**
 * 피싱 패턴 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingPatternDto {

  private String id;

  @NotBlank(message = "패턴 이름은 필수입니다")
  @Size(max = 100, message = "패턴 이름은 100자 이내여야 합니다")
  private String name;

  @NotBlank(message = "설명은 필수입니다")
  @Size(max = 500, message = "설명은 500자 이내여야 합니다")
  private String description;

  @NotBlank(message = "카테고리는 필수입니다")
  @Pattern(regexp = "financial|government|delivery|shopping|other",
      message = "유효한 카테고리를 선택해주세요")
  private String category;

  @NotBlank(message = "패턴 타입은 필수입니다")
  @Pattern(regexp = "regex|keyword", message = "패턴 타입은 regex 또는 keyword여야 합니다")
  private String type;

  @NotEmpty(message = "최소 하나 이상의 패턴이 필요합니다")
  @Size(max = 50, message = "패턴은 최대 50개까지 등록 가능합니다")
  private List<String> patterns;

  @NotBlank(message = "위험 수준은 필수입니다")
  @Pattern(regexp = "high|medium|low", message = "위험 수준은 high, medium, low 중 하나여야 합니다")
  private String riskLevel;

  @NotNull(message = "가중치는 필수입니다")
  @DecimalMin(value = "0.0", message = "가중치는 0 이상이어야 합니다")
  @DecimalMax(value = "1.0", message = "가중치는 1 이하여야 합니다")
  private Double weight;

  @NotBlank(message = "언어는 필수입니다")
  @Pattern(regexp = "ko|en|zh|ja", message = "지원되는 언어를 선택해주세요")
  private String language;

  private Boolean isActive;
  private Integer matchCount;
  private Integer falsePositiveCount;
  private Double accuracy;
  private Date lastUsedAt;
  private String createdBy;
  private String updatedBy;
  private Date createdAt;
  private Date updatedAt;

  /**
   * Entity to DTO 변환
   */
  public static PhishingPatternDto from(PhishingPattern pattern) {
    if (pattern == null) {
      return null;
    }

    return PhishingPatternDto.builder()
        .id(pattern.getId() != null ? pattern.getId().toString() : null)
        .name(pattern.getName())
        .description(pattern.getDescription())
        .category(pattern.getCategory())
        .type(pattern.getType())
        .patterns(pattern.getPatterns())
        .riskLevel(pattern.getRiskLevel())
        .weight(pattern.getWeight())
        .language(pattern.getLanguage())
        .isActive(pattern.getIsActive())
        .matchCount(pattern.getMatchCount())
        .falsePositiveCount(pattern.getFalsePositiveCount())
        .accuracy(pattern.getAccuracy())
        .lastUsedAt(pattern.getLastUsedAt())
        .createdBy(pattern.getCreatedBy())
        .updatedBy(pattern.getUpdatedBy())
        .createdAt(pattern.getCreatedAt())
        .updatedAt(pattern.getUpdatedAt())
        .build();
  }

  /**
   * DTO to Entity 변환
   */
  public PhishingPattern toEntity() {
    PhishingPattern pattern = new PhishingPattern();

    if (this.id != null) {
      pattern.setId(new org.bson.types.ObjectId(this.id));
    }

    pattern.setName(this.name);
    pattern.setDescription(this.description);
    pattern.setCategory(this.category);
    pattern.setType(this.type);
    pattern.setPatterns(this.patterns);
    pattern.setRiskLevel(this.riskLevel);
    pattern.setWeight(this.weight);
    pattern.setLanguage(this.language);
    pattern.setIsActive(this.isActive != null ? this.isActive : true);
    pattern.setMatchCount(this.matchCount != null ? this.matchCount : 0);
    pattern.setFalsePositiveCount(this.falsePositiveCount != null ? this.falsePositiveCount : 0);
    pattern.setAccuracy(this.accuracy != null ? this.accuracy : 1.0);
    pattern.setLastUsedAt(this.lastUsedAt);
    pattern.setCreatedBy(this.createdBy);
    pattern.setUpdatedBy(this.updatedBy);
    pattern.setCreatedAt(this.createdAt != null ? this.createdAt : new Date());
    pattern.setUpdatedAt(this.updatedAt != null ? this.updatedAt : new Date());

    return pattern;
  }
}