# Docker Setup - Banking API

## Quick Start

```bash
# Development (deps only - Postgres + Redis; run app locally with mvn)
docker-compose -f docker/docker-compose.dev.yml up -d

# Full stack (API + PostgreSQL + Redis + pgAdmin)
docker-compose -f docker/docker-compose.yml up -d

# Production
docker-compose -f docker/docker-compose.prod.yml up -d
```

## Files Overview

- **Dockerfile** - Multi-stage build for production
- **Dockerfile.dev** - Development image with live reload
- **Dockerfile.prod** - Lightweight production image
- **docker-compose.yml** - Full stack (API + PostgreSQL + Redis + pgAdmin)
- **docker-compose.dev.yml** - Development setup
- **docker-compose.prod.yml** - Production setup
- **.dockerignore** - Files to exclude from build

## Building Images

```bash
# Build for development
docker build -t banking-api:dev -f docker/Dockerfile.dev .

# Build for production
docker build -t banking-api:latest -f docker/Dockerfile .

# Build multi-arch for production
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t banking-api:latest \
  -f docker/Dockerfile .
```

## Environment Variables

Create `.env` file (optional — defaults are sensible for dev):

```
POSTGRES_DB=banking_api
POSTGRES_USER=banking_user
POSTGRES_PASSWORD=SecurePassword123!
REDIS_PASSWORD=RedisPassword123!
JWT_SECRET=your-super-secret-jwt-key-min-32-characters
```

**Development with docker-compose.dev.yml:**
When running the app locally (e.g., `mvn spring-boot:run -Dspring-boot.run.profiles=dev`), the default values in `application-dev.yml` will match the Docker services:
- Postgres: `localhost:5432` (or set `SPRING_DATASOURCE_URL` env var)
- Redis: `localhost:6379` (or set `SPRING_DATA_REDIS_HOST/PORT` env vars)

## Health Checks

### Using docker-compose.dev.yml (deps only)
```bash
# Database
docker-compose -f docker/docker-compose.dev.yml exec postgres psql -U banking_user -d banking_api -c "SELECT 1;"

# Redis
docker-compose -f docker/docker-compose.dev.yml exec redis redis-cli ping
```

### Using docker-compose.yml (full stack)
```bash
# API health
curl http://localhost:8080/api/v1/health

# Database
docker-compose exec postgres psql -U banking_user -d banking_api -c "SELECT 1;"

# Redis
docker-compose exec redis redis-cli ping
```

## Troubleshooting

### Port already in use
```bash
docker-compose down
# Kill the process or use different ports
```

### Database connection failed
```bash
docker-compose logs postgres
```

### Clear everything
```bash
docker-compose down -v
docker system prune -a
```

---

See also: [Deployment Guide](../docs/engineering/Banking_API_Deployment_Guide.md)
