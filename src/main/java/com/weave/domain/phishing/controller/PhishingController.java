package com.weave.domain.phishing.controller;

import com.weave.domain.phishing.dto.PhishingPatternDto;
import com.weave.domain.phishing.dto.PhishingReportRequestDto;
import com.weave.domain.phishing.dto.PhishingReportResponseDto;
import com.weave.domain.phishing.dto.PhishingStatisticsDto;
import com.weave.domain.phishing.service.PhishingDetectionService;
import com.weave.domain.phishing.service.PhishingGuardService;
import com.weave.domain.phishing.service.PhishingPatternService;
import com.weave.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피싱 가드 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/phishing")
@RequiredArgsConstructor
@Tag(name = "Phishing Guard", description = "SMS 피싱 가드 API")
@SecurityRequirement(name = "bearerAuth")
public class PhishingController {

  private final PhishingGuardService phishingGuardService;
  private final PhishingDetectionService detectionService;
  private final PhishingPatternService patternService;

  /**
   * 피싱 신고 접수
   */
  @PostMapping("/report")
  @Operation(summary = "피싱 신고", description = "SMS 피싱 메시지를 신고합니다.")
  public ResponseEntity<ApiResponse<PhishingReportResponseDto>> reportPhishing(
      @Valid @RequestBody PhishingReportRequestDto request,
      Principal principal) {

    log.info("피싱 신고 API 호출 - 사용자: {}", principal.getName());

    PhishingReportResponseDto response = phishingGuardService.reportPhishing(
        principal.getName(), request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response));
  }

  /**
   * 내 피싱 신고 목록 조회
   */
  @GetMapping("/reports/me")
  @Operation(summary = "내 피싱 신고 목록", description = "본인이 신고한 피싱 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<Page<PhishingReportResponseDto>>> getMyReports(
      @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
      Principal principal) {

    Page<PhishingReportResponseDto> reports = phishingGuardService.getReports(
        principal.getName(), pageable);

    return ResponseEntity.ok(ApiResponse.ok(reports));
  }

  /**
   * 워크스페이스 피싱 신고 조회
   */
  @GetMapping("/reports/workspace/{workspaceId}")
  @Operation(summary = "워크스페이스 피싱 신고", description = "워크스페이스의 피싱 신고 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<Page<PhishingReportResponseDto>>> getWorkspaceReports(
      @PathVariable String workspaceId,
      @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<PhishingReportResponseDto> reports = phishingGuardService.getWorkspaceReports(
        workspaceId, pageable);

    return ResponseEntity.ok(ApiResponse.ok(reports));
  }

  /**
   * 피싱 신고 상세 조회
   */
  @GetMapping("/reports/{reportId}")
  @Operation(summary = "피싱 신고 상세", description = "피싱 신고 상세 정보를 조회합니다.")
  public ResponseEntity<ApiResponse<PhishingReportResponseDto>> getReport(
      @PathVariable String reportId) {

    PhishingReportResponseDto report = phishingGuardService.getReport(reportId);
    return ResponseEntity.ok(ApiResponse.ok(report));
  }

  /**
   * 피싱 신고 피드백 추가
   */
  @PutMapping("/reports/{reportId}/feedback")
  @Operation(summary = "피싱 신고 피드백", description = "피싱 신고에 사용자 피드백을 추가합니다.")
  public ResponseEntity<ApiResponse<PhishingReportResponseDto>> addFeedback(
      @PathVariable String reportId,
      @RequestBody Map<String, String> request,
      Principal principal) {

    String feedback = request.get("feedback");
    PhishingReportResponseDto updated = phishingGuardService.addUserFeedback(
        reportId, principal.getName(), feedback);

    return ResponseEntity.ok(ApiResponse.ok(updated));
  }

  /**
   * 내 피싱 통계 조회
   */
  @GetMapping("/statistics/me")
  @Operation(summary = "내 피싱 통계", description = "본인의 피싱 탐지 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<PhishingStatisticsDto>> getMyStatistics(
      Principal principal) {

    PhishingStatisticsDto stats = phishingGuardService.getStatistics(principal.getName());
    return ResponseEntity.ok(ApiResponse.ok(stats));
  }

  /**
   * 워크스페이스 피싱 통계 조회
   */
  @GetMapping("/statistics/workspace/{workspaceId}")
  @Operation(summary = "워크스페이스 피싱 통계", description = "워크스페이스의 피싱 탐지 통계를 조회합니다.")
  public ResponseEntity<ApiResponse<PhishingStatisticsDto>> getWorkspaceStatistics(
      @PathVariable String workspaceId) {

    PhishingStatisticsDto stats = phishingGuardService.getWorkspaceStatistics(workspaceId);
    return ResponseEntity.ok(ApiResponse.ok(stats));
  }

  /**
   * 근처 피싱 알림 조회
   */
  @GetMapping("/reports/nearby")
  @Operation(summary = "근처 피싱 알림", description = "현재 위치 근처의 피싱 알림을 조회합니다.")
  public ResponseEntity<ApiResponse<List<PhishingReportResponseDto>>> getNearbyReports(
      @RequestParam @Parameter(description = "위도") double latitude,
      @RequestParam @Parameter(description = "경도") double longitude,
      @RequestParam(defaultValue = "5000") @Parameter(description = "반경(미터)") double radius) {

    List<PhishingReportResponseDto> reports = phishingGuardService.getNearbyReports(
        latitude, longitude, radius);

    return ResponseEntity.ok(ApiResponse.ok(reports));
  }

  /**
   * SMS 피싱 검사 (수동)
   */
  @PostMapping("/detect")
  @Operation(summary = "피싱 검사", description = "SMS 메시지의 피싱 여부를 검사합니다.")
  public ResponseEntity<ApiResponse<PhishingDetectionResult>> detectPhishing(
      @Valid @RequestBody PhishingDetectionRequest request) {

    log.info("피싱 검사 요청 - 발신자: {}", request.getSender());

    PhishingDetectionResult result = detectionService.detectPhishing(
        request.getSender(),
        request.getMessage(),
        request.getSensitivityLevel()
    );

    return ResponseEntity.ok(ApiResponse.ok(result));
  }

  /**
   * 피싱 패턴 목록 조회 (관리자용)
   */
  @GetMapping("/patterns")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "피싱 패턴 목록", description = "[관리자] 피싱 탐지 패턴 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<PhishingPatternDto>>> getPatterns(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String language,
      @RequestParam(defaultValue = "true") boolean activeOnly) {

    List<PhishingPatternDto> patterns = patternService.getPatterns(category, language, activeOnly);
    return ResponseEntity.ok(ApiResponse.ok(patterns));
  }

  /**
   * 피싱 패턴 추가 (관리자용)
   */
  @PostMapping("/patterns")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "피싱 패턴 추가", description = "[관리자] 새로운 피싱 탐지 패턴을 추가합니다.")
  public ResponseEntity<ApiResponse<PhishingPatternDto>> createPattern(
      @Valid @RequestBody PhishingPatternDto request,
      Principal principal) {

    log.info("피싱 패턴 추가 - 관리자: {}", principal.getName());

    PhishingPatternDto created = patternService.createPattern(request, principal.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(created));
  }

  /**
   * 피싱 패턴 수정 (관리자용)
   */
  @PutMapping("/patterns/{patternId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "피싱 패턴 수정", description = "[관리자] 피싱 탐지 패턴을 수정합니다.")
  public ResponseEntity<ApiResponse<PhishingPatternDto>> updatePattern(
      @PathVariable String patternId,
      @Valid @RequestBody PhishingPatternDto request,
      Principal principal) {

    PhishingPatternDto updated = patternService.updatePattern(patternId, request,
        principal.getName());
    return ResponseEntity.ok(ApiResponse.ok(updated));
  }

  /**
   * 피싱 패턴 삭제 (관리자용)
   */
  @DeleteMapping("/patterns/{patternId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "피싱 패턴 삭제", description = "[관리자] 피싱 탐지 패턴을 삭제합니다.")
  public ResponseEntity<ApiResponse<Void>> deletePattern(
      @PathVariable String patternId) {

    patternService.deletePattern(patternId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  /**
   * 고위험 미처리 신고 조회 (관리자용)
   */
  @GetMapping("/reports/high-risk/pending")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "고위험 미처리 신고", description = "[관리자] 고위험 미처리 피싱 신고를 조회합니다.")
  public ResponseEntity<ApiResponse<List<PhishingReportResponseDto>>> getHighRiskPendingReports() {

    List<PhishingReportResponseDto> reports = phishingGuardService.getHighRiskPendingReports();
    return ResponseEntity.ok(ApiResponse.ok(reports));
  }

  /**
   * 피싱 신고 상태 변경 (관리자용)
   */
  @PutMapping("/reports/{reportId}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "신고 상태 변경", description = "[관리자] 피싱 신고 상태를 변경합니다.")
  public ResponseEntity<ApiResponse<PhishingReportResponseDto>> updateReportStatus(
      @PathVariable String reportId,
      @RequestBody Map<String, String> request) {

    String status = request.get("status");
    String adminNote = request.get("adminNote");

    PhishingReportResponseDto updated = phishingGuardService.updateReportStatus(
        reportId, status, adminNote);

    return ResponseEntity.ok(ApiResponse.ok(updated));
  }

  /**
   * 피싱 탐지 요청 DTO
   */
  public static class PhishingDetectionRequest {

    @NotBlank
    private String sender;

    @NotBlank
    private String message;

    private String sensitivityLevel = "medium"; // high, medium, low

    // Getters and setters
    public String getSender() {
      return sender;
    }

    public void setSender(String sender) {
      this.sender = sender;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getSensitivityLevel() {
      return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
      this.sensitivityLevel = sensitivityLevel;
    }
  }

  /**
   * 피싱 탐지 결과 DTO
   */
  public static class PhishingDetectionResult {

    private boolean isPhishing;
    private double riskScore;
    private String riskLevel;
    private List<String> detectionReasons;
    private String phishingType;
    private double confidence;

    // Getters and setters
    public boolean isPhishing() {
      return isPhishing;
    }

    public void setPhishing(boolean phishing) {
      isPhishing = phishing;
    }

    public double getRiskScore() {
      return riskScore;
    }

    public void setRiskScore(double riskScore) {
      this.riskScore = riskScore;
    }

    public String getRiskLevel() {
      return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
      this.riskLevel = riskLevel;
    }

    public List<String> getDetectionReasons() {
      return detectionReasons;
    }

    public void setDetectionReasons(List<String> detectionReasons) {
      this.detectionReasons = detectionReasons;
    }

    public String getPhishingType() {
      return phishingType;
    }

    public void setPhishingType(String phishingType) {
      this.phishingType = phishingType;
    }

    public double getConfidence() {
      return confidence;
    }

    public void setConfidence(double confidence) {
      this.confidence = confidence;
    }
  }
}