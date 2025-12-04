package com.weave.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

  // Common
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 메서드입니다"),
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "엔티티를 찾을 수 없습니다"),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 오류가 발생했습니다"),
  INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 타입입니다"),
  HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근이 거부되었습니다"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "C007", "리소스를 찾을 수 없습니다"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C008", "중복된 리소스입니다"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "C009", "권한이 없습니다"),

  // User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
  EMAIL_DUPLICATION(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다"),
  INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 일치하지 않습니다"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U004", "인증되지 않은 사용자입니다"),

  // Workspace
  WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "워크스페이스를 찾을 수 없습니다"),
  WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "W002", "워크스페이스 접근 권한이 없습니다"),
  WORKSPACE_MEMBER_EXISTS(HttpStatus.CONFLICT, "W003", "이미 워크스페이스 멤버입니다"),
  WORKSPACE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "W004", "워크스페이스 멤버가 아닙니다"),

  // Phishing
  PHISHING_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "피싱 신고를 찾을 수 없습니다"),
  PHISHING_PATTERN_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "피싱 패턴을 찾을 수 없습니다"),
  PHISHING_DETECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P003", "피싱 탐지에 실패했습니다"),
  INVALID_PHISHING_PATTERN(HttpStatus.BAD_REQUEST, "P004", "잘못된 피싱 패턴입니다"),
  PHISHING_STATISTICS_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "피싱 통계를 찾을 수 없습니다"),
  PHISHING_DUPLICATE_REPORT(HttpStatus.CONFLICT, "P006", "이미 신고된 피싱입니다"),

  // SMS
  SMS_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "SMS를 찾을 수 없습니다"),
  SMS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S002", "SMS 접근 권한이 없습니다"),

  // WebSocket
  WEBSOCKET_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WS001", "WebSocket 연결 오류가 발생했습니다"),
  WEBSOCKET_MESSAGE_ERROR(HttpStatus.BAD_REQUEST, "WS002", "WebSocket 메시지 처리 중 오류가 발생했습니다"),

  // Notification
  NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N001", "알림 전송에 실패했습니다"),
  FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "N002", "FCM 토큰을 찾을 수 없습니다"),

  // File
  FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 업로드에 실패했습니다"),
  FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "파일 다운로드에 실패했습니다"),
  INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "F003", "잘못된 파일 형식입니다");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}