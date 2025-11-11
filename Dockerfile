# Stage 1: Build
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 파일만 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 애플리케이션 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 로그 및 키 디렉토리 생성 (root로 생성 후 spring 유저에게 권한 부여)
RUN mkdir -p /app/logs /var/log/weave-api /app/keys && \
    chown -R spring:spring /app/logs /var/log/weave-api /app/keys

# 사용자 전환
USER spring:spring

# 포트 노출
EXPOSE 8080 7070

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
