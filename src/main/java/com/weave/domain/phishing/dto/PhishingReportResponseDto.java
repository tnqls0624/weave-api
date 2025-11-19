package com.weave.domain.phishing.dto;

import com.weave.domain.phishing.entity.PhishingReport;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 피싱 신고 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhishingReportResponseDto {

  private String id;
  private String smsId;
  private String userId;
  private String userEmail;
  private String workspaceId;
  private String sender;
  private String message;
  private Double riskScore;
  private String riskLevel;
  private List<String> detectionReasons;
  private String phishingType;
  private LocationDto location;
  private DeviceInfoDto deviceInfo;
  private String status;
  private Boolean autoBlocked;
  private String userFeedback;
  private Date timestamp;
  private Date verifiedAt;
  private Date createdAt;
  private Date updatedAt;

  /**
   * Entity to DTO 변환
   */
  public static PhishingReportResponseDto from(PhishingReport report) {
    LocationDto locationDto = null;
    if (report.getLocation() != null) {
      locationDto = LocationDto.builder()
        .latitude(report.getLocation().getLatitude())
        .longitude(report.getLocation().getLongitude())
        .address(report.getLocation().getAddress())
        .city(report.getLocation().getCity())
        .country(report.getLocation().getCountry())
        .build();
    }

    DeviceInfoDto deviceInfoDto = null;
    if (report.getDeviceInfo() != null) {
      deviceInfoDto = DeviceInfoDto.builder()
        .platform(report.getDeviceInfo().getPlatform())
        .version(report.getDeviceInfo().getVersion())
        .model(report.getDeviceInfo().getModel())
        .manufacturer(report.getDeviceInfo().getManufacturer())
        .osVersion(report.getDeviceInfo().getOsVersion())
        .build();
    }

    return PhishingReportResponseDto.builder()
      .id(report.getId() != null ? report.getId().toString() : null)
      .smsId(report.getSmsId())
      .userId(report.getUserId() != null ? report.getUserId().toString() : null)
      .userEmail(report.getUserEmail())
      .workspaceId(report.getWorkspaceId() != null ? report.getWorkspaceId().toString() : null)
      .sender(report.getSender())
      .message(report.getMessage())
      .riskScore(report.getRiskScore())
      .riskLevel(report.getRiskLevel())
      .detectionReasons(report.getDetectionReasons())
      .phishingType(report.getPhishingType())
      .location(locationDto)
      .deviceInfo(deviceInfoDto)
      .status(report.getStatus())
      .autoBlocked(report.getAutoBlocked())
      .userFeedback(report.getUserFeedback())
      .timestamp(report.getTimestamp())
      .verifiedAt(report.getVerifiedAt())
      .createdAt(report.getCreatedAt())
      .updatedAt(report.getUpdatedAt())
      .build();
  }

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