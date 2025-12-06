package com.weave.global;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  // 인증/인가
  INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "A001", "Invalid username or password"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A002", "Unauthorized"),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "A003", "Access denied"),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A004", "Invalid token"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A005", "Token expired"),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "Invalid refresh token"),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A007", "Expired refresh token"),

  // 로그인
  PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "A008", "Password mismatch"),
  SOCIAL_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "A009", "Social login failed"),

  // 사용자/권한/역할
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "Email already exists"),
  ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "Role not found"),
  DEFAULT_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "R002", "Default role not found"),
  MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "Menu not found"),


  // 공통/검증/서버
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "C001", "Validation failed"),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "C002", "Bad request"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "Internal server error"),
  WORKSPACE_NOT_FOUND(HttpStatus.BAD_REQUEST, "C003", "Workspace not found"),
  SCHEDULE_NOT_FOUND(HttpStatus.BAD_REQUEST, "C004", "Schedule not found"),
  WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "Workspace access denied"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C006", "Duplicate resource"),
  INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "C007", "Invalid invite code"),
  CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "C008", "Cannot use your own invite code"),
  ALREADY_JOINED_WORKSPACE(HttpStatus.CONFLICT, "C009", "Already joined this workspace"),
  NOT_WORKSPACE_MASTER(HttpStatus.FORBIDDEN, "C010", "Only workspace master can perform this action"),
  MASTER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "C011", "Workspace master cannot leave, delete the workspace instead"),
  CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "C012", "Cannot kick yourself from workspace"),
  ;


  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }
}