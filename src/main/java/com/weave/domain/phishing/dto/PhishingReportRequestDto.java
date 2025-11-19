package com.weave.domain.phishing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 피싱 신고 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingReportRequestDto {

  /**
   * SMS 고유 ID
   */
  @NotBlank(message = "SMS ID는 필수입니다")
  private String smsId;

  /**
   * 발신자
   */
  @NotBlank(message = "발신자 정보는 필수입니다")
  private String sender;

  /**
   * 메시지 내용
   */
  @NotBlank(message = "메시지 내용은 필수입니다")
  private String message;

  /**
   * 위험도 점수 (0.0 ~ 1.0)
   */
  @NotNull(message = "위험도 점수는 필수입니다")
  @Min(value = 0, message = "위험도 점수는 0 이상이어야 합니다")
  @Max(value = 1, message = "위험도 점수는 1 이하여야 합니다")
  private Double riskScore;

  /**
   * 위험 수준
   */
  @NotBlank(message = "위험 수준은 필수입니다")
  private String riskLevel;

  /**
   * 탐지 이유
   */
  private List<String> detectionReasons;

  /**
   * 피싱 유형
   */
  private String phishingType;

  /**
   * 위치 정보
   */
  private LocationDto location;

  /**
   * 디바이스 정보
   */
  private DeviceInfoDto deviceInfo;

  /**
   * 워크스페이스 ID (선택사항)
   */
  private String workspaceId;

  /**
   * SMS 수신 시각
   */
  @NotNull(message = "타임스탬프는 필수입니다")
  private Long timestamp;

  /**
   * 자동 차단 여부
   */
  private Boolean autoBlocked;

  /**
   * 위치 정보 DTO
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class LocationDto {
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String country;
  }

  /**
   * 디바이스 정보 DTO
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DeviceInfoDto {
    private String platform;
    private String version;
    private String model;
    private String manufacturer;
    private String osVersion;
  }
}