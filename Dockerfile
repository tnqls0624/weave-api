# Stage 1: Build
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 파일만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 및 ML 모델 복사
COPY src ./src
COPY models ./models
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 애플리케이션 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 빌드된 JAR 파일 및 ML 모델 복사
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/models ./models

# 로그, 키, 모델 디렉토리 생성 (root로 생성 후 spring 유저에게 권한 부여)
RUN mkdir -p /app/logs /var/log/weave-api /app/keys && \
    chown -R spring:spring /app/logs /var/log/weave-api /app/keys /app/models

# 사용자 전환
USER spring:spring

# 포트 노출
EXPOSE 8080 7070

# JVM 옵션 설정 (ML 모델 사용을 위해 메모리 증가)
ENV JAVA_OPTS="\
  -Xms256m \
  -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/./urandom"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
