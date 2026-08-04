# Docker Setup - Banking API

## Quick Start

```bash
# Development
docker-compose -f docker/docker-compose.dev.yml up -d

# Production
docker-compose -f docker/docker-compose.prod.yml up -d

# Full stack (with PostgreSQL + Redis)
docker-compose -f docker/docker-compose.yml up -d
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

Create `.env` file:

```
POSTGRES_DB=banking_api
POSTGRES_USER=banking_user
POSTGRES_PASSWORD=SecurePassword123!
REDIS_PASSWORD=RedisPassword123!
JWT_SECRET=your-super-secret-jwt-key-min-32-characters
```

## Health Checks

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
