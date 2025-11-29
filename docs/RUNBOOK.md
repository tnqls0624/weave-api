# Weave API 운영 Runbook

## 목차
1. [배포 가이드](#1-배포-가이드)
2. [롤백 절차](#2-롤백-절차)
3. [모니터링](#3-모니터링)
4. [장애 대응](#4-장애-대응)
5. [백업 및 복구](#5-백업-및-복구)
6. [스케일링](#6-스케일링)

---

## 1. 배포 가이드

### 1.1 배포 전략 선택

| 전략 | 사용 시점 | 위험도 | 롤백 시간 |
|------|----------|--------|----------|
| **Rolling** | 일반적인 배포, 소규모 변경 | 낮음 | 1-2분 |
| **Blue-Green** | 대규모 변경, Zero-downtime 필수 | 중간 | 즉시 |
| **Canary** | 위험한 변경, 새로운 기능 테스트 | 낮음 | 즉시 |

### 1.2 GitHub Actions 배포 (권장)

1. **GitHub Actions UI에서 배포**
   - Repository > Actions > "CD - Deploy to Production"
   - "Run workflow" 클릭
   - 옵션 선택:
     - `environment`: staging 또는 production
     - `strategy`: rolling, blue-green, canary
     - `rollback_on_error`: true (권장)

2. **배포 상태 확인**
   - GitHub Actions 화면에서 실시간 로그 확인
   - Slack `#alerts` 채널에서 알림 확인

### 1.3 수동 배포 (긴급 시)

```bash
# SSH 접속
ssh ec2-user@your-server

# 환경변수 설정
export ECR_REGISTRY="your-account.dkr.ecr.ap-northeast-2.amazonaws.com"
export ECR_REPOSITORY="weave-api/prod"
export IMAGE_TAG="your-commit-sha"

# Rolling 배포
./scripts/deploy-rolling.sh

# Blue-Green 배포
./scripts/deploy-blue-green.sh

# Canary 배포
./scripts/deploy-canary.sh
```

### 1.4 배포 확인 체크리스트

- [ ] Health check 통과 (`/actuator/health` → 200 OK)
- [ ] MongoDB 연결 정상
- [ ] Redis 연결 정상
- [ ] 주요 API 엔드포인트 테스트
- [ ] Grafana 대시보드에서 메트릭 확인
- [ ] 에러율 정상 범위 내 (< 1%)

---

## 2. 롤백 절차

### 2.1 자동 롤백

배포 중 헬스체크 실패 시 자동으로 롤백됩니다.
- CD 파이프라인의 `rollback_on_error: true` 설정 확인

### 2.2 수동 롤백

```bash
# SSH 접속 후
./scripts/rollback.sh production

# 또는 직접 Docker 명령어
docker service update --rollback weave_api
```

### 2.3 특정 버전으로 롤백

```bash
# 특정 이미지 버전으로 롤백
docker service update \
  --image your-account.dkr.ecr.ap-northeast-2.amazonaws.com/weave-api/prod:specific-sha \
  weave_api
```

### 2.4 Blue-Green 롤백

```bash
# 현재 활성 환경 확인
cat /tmp/active_deployment.txt

# 트래픽 전환 (blue ↔ green)
./scripts/rollback.sh
```

---

## 3. 모니터링

### 3.1 대시보드 접근

| 서비스 | URL | 용도 |
|--------|-----|------|
| **Grafana** | http://grafana.weave.local:3000 | 메트릭 대시보드 |
| **Jaeger** | http://jaeger.weave.local:16686 | 분산 트레이싱 |
| **Traefik** | http://traefik.weave.local:8081 | 로드밸런서 상태 |
| **Prometheus** | http://prometheus:9090 | 메트릭 쿼리 |

### 3.2 주요 메트릭

```promql
# API 응답 시간 (95th percentile)
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)

# 에러율
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count[5m]))

# JVM 힙 메모리 사용률
jvm_memory_used_bytes{area="heap"}
/ jvm_memory_max_bytes{area="heap"}

# 활성 연결 수
http_server_connections_active
```

### 3.3 로그 확인

```bash
# Docker 로그 확인
docker service logs weave_api --tail 100 -f

# Loki에서 로그 쿼리 (Grafana)
{service="weave-api"} |= "ERROR"
{service="weave-api"} | json | level="ERROR"
```

### 3.4 Alert 규칙

| Alert | 조건 | 심각도 | 대응 |
|-------|------|--------|------|
| `HighErrorRate` | 에러율 > 5% (5분) | Critical | 롤백 고려 |
| `SlowResponseTime` | P95 > 2초 (5분) | Warning | 원인 조사 |
| `APIServiceDown` | UP == 0 (1분) | Critical | 즉시 대응 |
| `HighMemoryUsage` | 힙 > 90% (5분) | Warning | 스케일 업 고려 |
| `MongoDBDown` | UP == 0 (1분) | Critical | DB 확인 |

---

## 4. 장애 대응

### 4.1 서비스 다운

```bash
# 1. 서비스 상태 확인
docker service ls
docker service ps weave_api

# 2. 컨테이너 로그 확인
docker service logs weave_api --tail 200

# 3. 서비스 재시작
docker service update --force weave_api

# 4. 복구 안되면 롤백
./scripts/rollback.sh production
```

### 4.2 높은 에러율

```bash
# 1. 에러 로그 확인
docker service logs weave_api 2>&1 | grep -i error | tail -50

# 2. Jaeger에서 실패한 요청 트레이싱 확인
# http://jaeger.weave.local:16686

# 3. 최근 변경 사항 확인
git log --oneline -10

# 4. 필요시 롤백
./scripts/rollback.sh production
```

### 4.3 느린 응답 시간

```bash
# 1. 리소스 사용량 확인
docker stats

# 2. MongoDB 슬로우 쿼리 확인
docker exec mongodb mongosh --eval "db.currentOp({secs_running: {$gt: 1}})"

# 3. JVM 힙 덤프 (필요시)
docker exec api jcmd 1 GC.heap_dump /tmp/heap.hprof

# 4. 스케일 업 고려
docker service scale weave_api=2
```

### 4.4 데이터베이스 연결 실패

```bash
# MongoDB 상태 확인
docker service ps weave_mongodb
docker exec mongodb mongosh --eval "db.adminCommand('ping')"

# Redis 상태 확인
docker service ps weave_redis
docker exec redis redis-cli ping

# 네트워크 확인
docker network inspect weave-network
```

### 4.5 메모리 부족 (OOM)

```bash
# 1. 현재 메모리 사용량 확인
docker stats --no-stream

# 2. JVM 힙 설정 확인
docker exec api java -XX:+PrintFlagsFinal -version | grep -i heap

# 3. GC 로그 확인
docker logs api 2>&1 | grep -i "gc\|memory"

# 4. 메모리 제한 증가
# docker-compose.prod.yml 수정 후 재배포
```

---

## 5. 백업 및 복구

### 5.1 백업 상태 확인

```bash
# 최신 백업 확인
ls -la /home/ec2-user/backups/mongodb/

# Cron 설정 확인
crontab -l | grep backup

# 백업 로그 확인
tail -50 /home/ec2-user/logs/cron-backup.log
```

### 5.2 수동 백업

```bash
./scripts/mongodb-backup.sh
```

### 5.3 복구 절차

```bash
# 1. 최신 백업 확인
LATEST_BACKUP=$(ls -t /home/ec2-user/backups/mongodb/*.tar.gz | head -1)
echo "Latest backup: $LATEST_BACKUP"

# 2. 백업 압축 해제
cd /home/ec2-user/backups/mongodb
tar -xzf $LATEST_BACKUP

# 3. MongoDB 복구
BACKUP_DIR=$(basename $LATEST_BACKUP .tar.gz)
docker exec -i mongodb mongorestore \
  --username=root \
  --password=$MONGODB_ROOT_PASSWORD \
  --authenticationDatabase=admin \
  --drop \
  /data/restore/$BACKUP_DIR/lovechedule

# 4. 복구 확인
docker exec mongodb mongosh \
  --username=root \
  --password=$MONGODB_ROOT_PASSWORD \
  --authenticationDatabase=admin \
  --eval "db.stats()"
```

---

## 6. 스케일링

### 6.1 수평 스케일링 (Horizontal)

```bash
# API 인스턴스 증가
docker service scale weave_api=3

# 상태 확인
docker service ps weave_api
```

### 6.2 수직 스케일링 (Vertical)

`docker-compose.prod.yml` 수정:

```yaml
api:
  deploy:
    resources:
      limits:
        cpus: '2'        # 1 → 2
        memory: 2G       # 1G → 2G
      reservations:
        cpus: '1'
        memory: 1G
```

재배포:
```bash
docker stack deploy -c docker-compose.yml weave --with-registry-auth
```

### 6.3 자동 스케일링 (권장하지 않음)

Docker Swarm은 기본적으로 자동 스케일링을 지원하지 않습니다.
필요시 외부 도구(Orbiter, Docker Swarm Autoscaler) 검토.

---

## 부록

### A. 유용한 명령어

```bash
# 서비스 상태
docker service ls
docker service ps weave_api

# 컨테이너 로그
docker service logs weave_api -f --tail 100

# 리소스 사용량
docker stats

# 네트워크 확인
docker network ls
docker network inspect weave-network

# 볼륨 확인
docker volume ls

# 이미지 정리
docker system prune -a --volumes
```

### B. 연락처

| 역할 | 담당자 | 연락처 |
|------|--------|--------|
| DevOps | - | Slack: #devops |
| Backend | - | Slack: #backend |
| On-call | - | PagerDuty |

### C. 관련 문서

- [CI/CD 파이프라인 설정](.github/workflows/)
- [Docker Compose 설정](docker-compose.prod.yml)
- [모니터링 설정](monitoring/)
- [백업 스크립트](scripts/)
