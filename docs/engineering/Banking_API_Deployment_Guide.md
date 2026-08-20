# Deployment Guide - Banking API

**Version:** 1.0.0  
**Date:** 2026-08-04  
**Target Environments:** Local Development, Docker, Kubernetes (dev / staging / prod)

---

## 1. Prerequisites

### 1.1 System Requirements

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 21 | LTS version |
| Maven | 3.9+ | Build tool |
| Docker | 24+ | Containerization |
| Docker Compose | 2.20+ | Multi-container orchestration |
| PostgreSQL | 17 | Database (local only) |
| Redis | 7+ | Cache (local only) |
| Git | 2.40+ | Version control |
| kubectl | 1.24+ | Kubernetes CLI (Kustomize built-in) |

### 1.2 Installation

#### macOS (Homebrew)
```bash
brew install java21-temurin maven docker docker-compose postgresql@17 redis
```

#### Ubuntu/Debian
```bash
sudo apt-get update && sudo apt-get install -y \
  openjdk-21-jdk \
  maven \
  docker.io \
  docker-compose \
  postgresql \
  redis-server

sudo usermod -aG docker $USER
```

#### Windows (WSL2)
```bash
# Use Ubuntu on WSL2 and follow Ubuntu instructions
# Or use Windows Package Manager
winget install Microsoft.OpenJDK.21 Maven Docker.DockerDesktop
```

---

## 2. Local Development Setup

### 2.1 Clone Repository

```bash
git clone https://github.com/your-org/banking-api.git
cd banking-api
```

### 2.2 Environment Variables

Create `.env.local` file in project root:

```properties
# Application
APP_NAME=Banking API
APP_PORT=8080
APP_ENVIRONMENT=local

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=banking_api
DB_USER=banking_user
DB_PASSWORD=SecurePassword123!
DB_POOL_SIZE=10

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_TIMEOUT=2000

# JWT
JWT_SECRET=your-super-secret-jwt-key-min-32-characters-required
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=604800

# Logging
LOG_LEVEL=DEBUG
LOG_FILE=logs/application.log

# Actuator
ACTUATOR_ENABLED=true
ACTUATOR_ENDPOINTS=health,metrics,prometheus
```

### 2.3 PostgreSQL Local Setup

```bash
# macOS
brew services start postgresql@17

# Ubuntu
sudo systemctl start postgresql

# Create user and database
psql -U postgres << EOF
CREATE USER banking_user WITH PASSWORD 'SecurePassword123!';
CREATE DATABASE banking_api OWNER banking_user;
GRANT ALL PRIVILEGES ON DATABASE banking_api TO banking_user;
EOF

# Verify connection
psql -U banking_user -d banking_api -h localhost
```

### 2.4 Redis Local Setup

```bash
# macOS
brew services start redis

# Ubuntu
sudo systemctl start redis-server

# Test connection
redis-cli ping
```

### 2.5 Build and Run

```bash
# Build project
mvn clean build

# Run Flyway migrations
mvn flyway:migrate

# Run application
mvn spring-boot:run

# OR run from IDE
# Right-click BankingApplication.java → Run
```

### 2.6 Verify Local Setup

```bash
# Check application is running
curl -i http://localhost:8080/api/v1/health

# Expected output:
# HTTP/1.1 200 OK
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 2.7 Common Issues

#### PostgreSQL won't start
```bash
# Kill conflicting process
lsof -i :5432
kill -9 <PID>

# Restart
brew services restart postgresql@17
```

#### Redis connection refused
```bash
# Check if Redis is running
redis-cli ping

# If not, start it
brew services start redis
```

#### Port already in use
```bash
# Find and kill process on port 8080
lsof -i :8080
kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

---

## 3. Docker & Docker Compose

### 3.1 Docker Images

#### Dockerfile for Banking API

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN useradd -m -u 1000 bankingapp

# Copy JAR from builder
COPY --from=builder /app/target/banking-api-*.jar banking-api.jar

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD java -cp banking-api.jar org.springframework.boot.loader.JarLauncher \
      && curl -f http://localhost:8080/api/v1/health || exit 1

USER bankingapp
EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "banking-api.jar"]
```

#### docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:17-alpine
    container_name: banking-postgres
    environment:
      POSTGRES_USER: banking_user
      POSTGRES_PASSWORD: SecurePassword123!
      POSTGRES_DB: banking_api
      POSTGRES_INITDB_ARGS: "-c shared_preload_libraries=pg_stat_statements"
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U banking_user -d banking_api"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - banking_network
    command:
      - "postgres"
      - "-c"
      - "max_connections=100"

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: banking-redis
    command: redis-server --appendonly yes --requirepass RedisPassword123!
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - banking_network

  # Banking API Application
  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: banking-api
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/banking_api
      SPRING_DATASOURCE_USERNAME: banking_user
      SPRING_DATASOURCE_PASSWORD: SecurePassword123!
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: RedisPassword123!
      JWT_SECRET: your-super-secret-jwt-key-min-32-characters-required
      JWT_EXPIRATION: 3600
      JWT_REFRESH_EXPIRATION: 604800
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_COM_COMPANY_BANKING: DEBUG
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - banking_network
    restart: unless-stopped

  # pgAdmin (Database GUI) - Optional
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: banking-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@example.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres
    networks:
      - banking_network
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local

networks:
  banking_network:
    driver: bridge
```

### 3.2 Docker Compose Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop services
docker-compose down

# Stop and remove volumes (careful!)
docker-compose down -v

# Rebuild after code changes
docker-compose up -d --build

# Check service status
docker-compose ps

# Execute command in container
docker-compose exec api bash
```

### 3.3 Verify Docker Setup

```bash
# Wait for services to be healthy (30-40 seconds)
docker-compose ps

# Check API health
curl http://localhost:8080/api/v1/health

# Check database connection
docker-compose exec postgres psql -U banking_user -d banking_api -c "SELECT 1;"

# Check Redis
docker-compose exec redis redis-cli -a RedisPassword123! ping
```

---

## 4. Database Migrations

### 4.1 Flyway Configuration

File: `src/main/resources/application.properties`

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baselineOnMigrate=true
spring.flyway.outOfOrder=false
spring.flyway.placeholderReplacement=true
spring.flyway.sqlMigrationPrefix=V
spring.flyway.sqlMigrationSeparator=__
spring.flyway.sqlMigrationSuffixes=.sql
```

### 4.2 Migration Files Structure

```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Create_users_table.sql
├── V3__Create_customers_table.sql
├── V4__Create_accounts_table.sql
├── V5__Create_transactions_table.sql
├── V6__Create_indexes.sql
├── V7__Insert_roles.sql
└── V8__Create_audit_logs_table.sql
```

### 4.3 Running Migrations

```bash
# Maven
mvn flyway:migrate

# Via Spring Boot
# Runs automatically on application startup

# Clean and remigrate (CAUTION: destroys data)
mvn flyway:clean flyway:migrate

# Check migration status
mvn flyway:info
```

### 4.4 Sample Migration File

File: `src/main/resources/db/migration/V1__Initial_schema.sql`

```sql
-- Create UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    customer_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_customer_number ON customers(customer_number);
CREATE INDEX idx_customers_user_id ON customers(user_id);

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    reference_id UUID,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_reference_id ON transactions(reference_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_account_date ON transactions(account_id, created_at);

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    action VARCHAR(100) NOT NULL,
    status_code INTEGER NOT NULL,
    ip_address VARCHAR(45),
    payload_hash VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_customer_id ON notifications(customer_id);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrator with full system access'),
    ('CUSTOMER', 'Regular customer with limited access')
ON CONFLICT (name) DO NOTHING;
```

---

## 5. Production Deployment

### 5.1 Production Checklist

- [ ] Environment variables configured for production
- [ ] Database backed up
- [ ] SSL/TLS certificates installed
- [ ] JWT secret rotated and secured
- [ ] Database connection pooling optimized
- [ ] Redis persistence enabled
- [ ] Logging configured (external log aggregation)
- [ ] Monitoring and alerting setup
- [ ] Rate limiting configured
- [ ] Security headers configured
- [ ] CORS whitelist configured
- [ ] Blue-green deployment ready

### 5.2 Production Docker Compose

```yaml
version: '3.8'

services:
  api:
    image: your-registry/banking-api:latest
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: ${DB_URL}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '1'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
```

### 5.3 Kubernetes Deployment

Manifests are Kustomize overlays under [`k8s/`](../../k8s/). **Use `kubectl apply -k`** (or `kubectl apply -k k8s/` for the default dev overlay). Do not recursively `kubectl apply -f k8s/` — overlay patches and SealedSecret placeholders are not standalone resources.

| Overlay | Namespace | API replicas | Notes |
|---------|-----------|--------------|--------|
| `k8s/overlays/dev` | `banking` | 1 | `SPRING_PROFILES_ACTIVE=dev`, ClusterIP + port-forward |
| `k8s/overlays/staging` | `banking-staging` | 2 | Ingress `api.staging.banking.local` |
| `k8s/overlays/prod` | `banking-prod` | 3–10 | Ingress + TLS + HPA |

#### Prerequisites

- Kubernetes 1.24+ with a **default StorageClass** (Kind, Minikube, Docker Desktop)
- Image `banking-api:<tag>` loaded into the cluster
- Optional: metrics-server (prod HPA), ingress-nginx (staging/prod Ingress)

```bash
docker build -f docker/Dockerfile -t banking-api:latest .
# Kind: kind load docker-image banking-api:latest
# Minikube: minikube image load banking-api:latest
```

#### Deploy to development

```bash
kubectl apply -k k8s/overlays/dev
kubectl get pods -n banking
kubectl get svc -n banking
kubectl rollout status deployment/banking-api -n banking

kubectl port-forward svc/banking-api 8080:8080 -n banking
curl -sf http://localhost:8080/api/v1/health
curl -sf http://localhost:8080/api/v1/health/live
curl -sf http://localhost:8080/api/v1/health/ready

kubectl logs -n banking -l app=banking-api --tail=100
```

Flyway runs when the API pod starts (Postgres DNS: `postgres-service`). Redis: `redis-service:6379`.

#### Deploy to staging / production

```bash
kubectl apply -k k8s/overlays/staging
kubectl apply -k k8s/overlays/prod

kubectl get pods -n banking-prod
kubectl get hpa -n banking-prod
kubectl rollout status deployment/banking-api -n banking-prod
```

#### Secrets

`k8s/base/secret.env` is **placeholder-only** so `kustomize build` works locally. Replace before any shared cluster. Production: [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) — see `k8s/base/secret-sealed.yaml`. Do not commit real passwords or JWT keys.

#### Probes, PDB, HPA

- Liveness: `GET /api/v1/health/live` (process up)
- Readiness: `GET /api/v1/health/ready` (PostgreSQL + Redis)
- Rolling update: `maxUnavailable: 0`; PDB `maxUnavailable: 1`
- Prod HPA: CPU 70% / memory 80%, min 3 / max 10 replicas (requires metrics-server)

#### Monitoring & debugging

```bash
kubectl logs -f deployment/banking-api -n banking
kubectl describe deployment banking-api -n banking
kubectl top pods -n banking
```

#### Upgrading / rollback

```bash
kubectl set image deployment/banking-api banking-api=banking-api:v1.1.0 -n banking
kubectl rollout status deployment/banking-api -n banking
kubectl rollout undo deployment/banking-api -n banking
```

More detail: [`k8s/README.md`](../../k8s/README.md).

---

## 6. Monitoring & Health Checks

### 6.1 Health Endpoint

```bash
curl http://localhost:8080/api/v1/health

# Response
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "PostgreSQL", "version": "17" }
    },
    "redis": {
      "status": "UP",
      "details": { "version": "7.0" }
    }
  }
}
```

### 6.2 Metrics Endpoint

```bash
curl http://localhost:8080/actuator/metrics

# View specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### 6.3 Prometheus Integration

File: `src/main/resources/application.properties`

```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

Access Prometheus metrics: `http://localhost:8080/actuator/prometheus`

### 6.4 Observability — OpenTelemetry & Jaeger

Distributed traces are exported over **OTLP gRPC** to a local Jaeger all-in-one container. The OpenTelemetry Java SDK no longer ships a native Jaeger exporter; Jaeger accepts OTLP on ports `4317` (gRPC) and `4318` (HTTP).

#### Local Jaeger

```bash
docker compose -f docker/docker-compose.dev.yml up -d
# Jaeger UI: http://localhost:16686
# OTLP gRPC: localhost:4317
```

Run the API with the `dev` profile (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`). The SDK is created in `ObservabilityConfig` and Spring Boot starter auto-instrumentation covers HTTP and JDBC. You do not add spans in controllers or domain code.

#### Sampling (dev vs prod)

| Environment | Property / env | Default |
|-------------|----------------|---------|
| Dev | `app.observability.sampling-rate` / `OTEL_TRACES_SAMPLER_ARG` | `0.1` (10%) |
| Prod | same, via `application-prod.yml` | `0.05` (5%) |

Sampler is **parent-based + trace-id ratio**: a sampled parent is always followed; otherwise the ratio applies. To debug a single session locally, set `OTEL_TRACES_SAMPLER_ARG=1.0` (or `OTEL_SAMPLER_ARG`) so every request appears in Jaeger.

Disable export without code changes:

```bash
OTEL_ENABLED=false
# or
otel.sdk.disabled=true   # used automatically in unit/IT tests
```

#### Jaeger UI

1. Open [http://localhost:16686](http://localhost:16686).
2. **Service** → `banking-api`.
3. **Find Traces** — recent HTTP operations (e.g. `POST /api/v1/auth/login`).
4. Open a trace to see the span tree: servlet/HTTP → application work → JDBC.

**Example — debug a transfer:** login, then `POST /api/v1/accounts/{id}/transfer`. In Jaeger, look for the transfer HTTP span and child JDBC spans (debit/credit). A red span indicates an error recorded on that span. Compare timestamps to JSON logs: each log line includes `trace_id` (and `requestId` from `X-Request-Id`) so you can grep Docker/console logs for the same id.

#### Environment

See `.env.example`: `OTEL_EXPORTER_OTLP_ENDPOINT` (alias `OTEL_JAEGER_ENDPOINT`), `OTEL_TRACES_SAMPLER_ARG` (alias `OTEL_SAMPLER_ARG`), `OTEL_BACKEND`. No API keys are required for local Jaeger.

#### Grafana dashboards (latency + error rate)

Prometheus scrapes `GET /actuator/prometheus` every 10s (JWT and rate limits are skipped for that path). Grafana on **http://localhost:3001** (user `admin` / password `admin`) loads provisioned dashboards:

| Dashboard | UID | What it shows |
|-----------|-----|----------------|
| HTTP Latency | `banking-http-latency` | p50 / p95 / p99 overall, p99 by URI |
| HTTP Error Rate | `banking-http-error-rate` | 5xx ratio gauge, 2xx/4xx/5xx rate, 5xx by URI |
| Service Level Objectives | `banking-slo` | availability / 5xx rate / p99 vs documented SLO targets |
| Error Budget | `banking-error-budget` | remaining 24h budget, burn vs 1×/6×/14.4×, hours to exhaustion |

```bash
docker compose -f docker/docker-compose.dev.yml up -d
# API must be running on the host so Prometheus can scrape host.docker.internal:8080
curl -sf http://localhost:8080/actuator/prometheus | head
# Grafana: http://localhost:3001  →  Dashboards → Banking API
# Prometheus targets: http://localhost:9090/targets
# Prometheus alerts: http://localhost:9090/alerts
# Alertmanager (no paging): http://localhost:9093
```

Histogram buckets are enabled for `http.server.requests` so `histogram_quantile` works; SLO buckets include 200 ms / 300 ms for latency error-budget ratios. Formal SLOs (availability 99.9%, error rate &lt; 0.1%, p99 &lt; 300 ms, money-path p99 &lt; 200 ms) are defined in [Banking_API_SLO.md](Banking_API_SLO.md). Error-budget remaining and Google SRE MWMB burn-rate alerts are in [Banking_API_Error_Budget.md](Banking_API_Error_Budget.md). Prometheus evaluates recording + alert rules (`http://localhost:9090/rules`, `/alerts`); Grafana `banking-slo` / `banking-error-budget` plot SLI vs target and remaining budget. Alertmanager (`http://localhost:9093`) is local-only — no paging.

```
┌─ HTTP Latency (Grafana :3001) ─────────────────────────────┐
│  p50 ──╮                                                   │
│  p95 ──┼── timeseries (seconds)                            │
│  p99 ──╯                                                   │
│  p99 by URI (table legend)                                 │
└────────────────────────────────────────────────────────────┘
┌─ HTTP Error Rate ──────────────────────────────────────────┐
│  [gauge 5xx %]   [2xx / 4xx / 5xx request rate]            │
│  5xx rate by URI                                           │
└────────────────────────────────────────────────────────────┘
┌─ Service Level Objectives (banking-slo) ───────────────────┐
│  [avail vs 99.9%]  [5xx vs 0.1%]  [p99 vs 300 ms]          │
│  timeseries + money-path p99 vs 200 ms                     │
└────────────────────────────────────────────────────────────┘
┌─ Error Budget (banking-error-budget) ──────────────────────┐
│  [remaining 24h]  [burn 5m]  [hours to exhaust]            │
│  burn vs 1× / 6× / 14.4×  ·  latency remaining             │
└────────────────────────────────────────────────────────────┘
```

Correlate a slow request: copy `trace_id` from JSON logs → Jaeger Search → compare span duration with the Grafana p99 spike at the same timestamp.

JSON sources (versioned, not click-ops): `docker/observability/grafana/dashboards/`.

#### Datadog (optional OTLP backend)

The app does **not** embed the Datadog Java tracer. Set the same OTLP exporter at a Datadog Agent (or intake):

```bash
OTEL_BACKEND=datadog
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317   # Datadog Agent OTLP gRPC
OTEL_DEPLOYMENT_ENVIRONMENT=staging                 # or DD_ENV
# DD_API_KEY=...   # only for Datadog intake, never for a local Agent; never commit
```

`deployment.environment` is added as a resource attribute. When `OTEL_BACKEND=datadog` **and** `DD_API_KEY` is set, the exporter sends header `dd-api-key`. Logs only record whether the key is `set` or `absent`.

Prod profile still **does not** expose `/actuator/prometheus` (`management.endpoints.web.exposure.include: health`). Local scrape is for Grafana only.

### 6.5 Centralized logging — ELK (dev)

Local log aggregation is **opt-in** so default `docker compose up` stays light. Elasticsearch 8.15 + Logstash + Kibana sit behind compose profile `elk`. The API already writes JSON (T-053); with profile `dev` or `prod` it also ships those events over **UDP 5000** to Logstash (`LogstashUdpSocketAppender` + `AsyncAppender neverBlock`). If ELK is down, packets are dropped and the API is unaffected.

```bash
docker compose -f docker/docker-compose.dev.yml --profile elk up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
curl -sf http://localhost:8080/api/v1/health
# Kibana: http://localhost:5601
# Elasticsearch: http://localhost:9200/_cluster/health
```

1. Open [http://localhost:5601](http://localhost:5601).
2. **Stack Management → Data Views → Create data view**.
3. Index pattern: `banking-api-*`. Timestamp field: `@timestamp`.
4. **Discover** — filter by `requestId` or `trace_id` (same values as Jaeger / `X-Request-Id`).

Host/port overrides: `LOGSTASH_HOST` / `LOGSTASH_PORT` (see `.env.example`). Pipeline: `docker/observability/logstash/pipeline/logstash.conf`.

If Elasticsearch fails to start on Linux/WSL, raise `vm.max_map_count` (Elastic requires 262144): `sudo sysctl -w vm.max_map_count=262144`.

### 6.6 Resilience — notification circuit breaker

Outbound notification HTTP is wrapped with **Resilience4j** instance `notification`. The default provider is still the logging stub (`NOTIFICATION_PROVIDER=log`); money flows do not call a vendor.

To POST JSON to a vendor:

```bash
NOTIFICATION_PROVIDER=http
NOTIFICATION_HTTP_BASE_URL=http://localhost:8099
# optional:
# NOTIFICATION_HTTP_PATH=/v1/notifications
# NOTIFICATION_HTTP_CONNECT_TIMEOUT=1s
# NOTIFICATION_HTTP_READ_TIMEOUT=2s
```

Behavior:

- Timeouts, connection errors, and 5xx open the circuit after the failure-rate threshold (see `resilience4j.circuitbreaker` in `application.yml`).
- 4xx is logged and ignored (payload bug; vendor is reachable).
- While **OPEN**, calls skip HTTP and fall back to the logging stub. Transfer / other money use cases still succeed.
- Circuit breaker state is **not** part of Spring health (OPEN must not fail K8s readiness). Metrics still go to `/actuator/prometheus` (`resilience4j.circuitbreaker.*`).

Never put credentials in `NOTIFICATION_HTTP_BASE_URL`.

### 6.7 Chaos experiments (local Compose)

Hypothesis-driven fault injection against **local Docker Compose only** (Postgres stop, Redis stop, notification vendor 5xx/timeout). Liveness must stay 200 while readiness returns 503 if Postgres or Redis is down. An open notification circuit breaker must not fail readiness.

Playbook + runner: [Banking_API_Chaos_Engineering_Playbook.md](Banking_API_Chaos_Engineering_Playbook.md)

```bash
docker compose -f docker/docker-compose.dev.yml -f docker/docker-compose.chaos.yml --profile chaos up -d
./scripts/chaos/run-experiments.sh
```

Do not run these experiments against staging or production.

### 6.8 PostgreSQL replication (local Compose)

Async streaming replica (hot standby on `localhost:5433`). The API and Flyway keep using the primary on `localhost:5432`. This is not failover (T-102), not PITR (T-103), and not analytics read-routing (T-104).

Guide + verify: [Banking_API_Postgres_Replication.md](Banking_API_Postgres_Replication.md)

```bash
docker compose -f docker/docker-compose.dev.yml \
  -f docker/docker-compose.replication.yml \
  --profile replication up -d
./scripts/postgres/verify-replication.sh
```

The Kubernetes Postgres StatefulSet stays a **single primary** (`replicas: 1`). Extra StatefulSet replicas are not streaming standbys.

---

## 7. Backup & Recovery

### 7.1 PostgreSQL Backup

A streaming replica (T-101) is **not** a backup. Use `pg_dump` / PITR (T-103) for recovery.

```bash
# Backup
docker-compose exec postgres pg_dump -U banking_user -d banking_api > backup.sql

# Restore
docker-compose exec -T postgres psql -U banking_user -d banking_api < backup.sql
```

### 7.2 Redis Backup

```bash
# Manual save
docker-compose exec redis redis-cli -a RedisPassword123! BGSAVE

# Backup persistent file
docker cp banking-redis:/data/dump.rdb ./redis-backup.rdb
```

---

## 8. Troubleshooting

### 8.1 Common Issues

#### Application won't start
```bash
# Check logs
docker-compose logs api

# Ensure database is running
docker-compose logs postgres

# Check port 8080 is not in use
lsof -i :8080
```

#### Database connection errors
```bash
# Test connection manually
docker-compose exec postgres psql -U banking_user -d banking_api -h postgres

# Check environment variables
docker-compose exec api env | grep SPRING_DATASOURCE
```

#### Migrations fail
```bash
# Check migration status
docker-compose exec api bash -c 'mvn flyway:info'

# View migration logs
docker-compose logs api | grep Flyway
```

---

## 9. CI/CD Integration

### 9.1 GitHub Actions Deployment

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build Docker image
        run: docker build -t banking-api:${{ github.sha }} .
      
      - name: Push to registry
        run: docker push banking-api:${{ github.sha }}
      
      - name: Deploy to production
        run: |
          ssh deploy@prod-server 'docker pull banking-api:${{ github.sha }}'
          ssh deploy@prod-server 'docker-compose -f /app/docker-compose.yml up -d'
```

---

## 10. Quick Start Commands

```bash
# Local development
mvn clean build && mvn spring-boot:run

# Docker Compose (all services)
docker-compose up -d

# Kubernetes (dev overlay)
kubectl apply -k k8s/overlays/dev

# Run tests
mvn clean verify

# Check health
curl http://localhost:8080/api/v1/health

# View logs
docker-compose logs -f api

# Stop everything
docker-compose down
```

---
