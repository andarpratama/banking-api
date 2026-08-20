# Kubernetes manifests — Banking API

GitOps-ready Kustomize layout for the Banking API stack (Spring Boot app + PostgreSQL 17 + Redis 7).

**Do not** `kubectl apply -f k8s/` recursively. Overlay patches and example secrets are not standalone apply targets. Use Kustomize (`kubectl apply -k`).

## Layout

```
k8s/
├── base/                 # Shared resources
├── overlays/
│   ├── dev/              # 1 replica, profile=dev, namespace banking
│   ├── staging/          # 2 replicas, Ingress, namespace banking-staging
│   └── prod/             # 3 replicas, HPA + Ingress, namespace banking-prod
└── README.md
```

| Overlay | Namespace | API replicas | Ingress | HPA |
|---------|-----------|--------------|---------|-----|
| `overlays/dev` | `banking` | 1 | no (ClusterIP + port-forward) | no |
| `overlays/staging` | `banking-staging` | 2 | `api.staging.banking.local` | no |
| `overlays/prod` | `banking-prod` | 3 (min) / 10 (max) | `api.banking.example` | CPU 70% / memory 80% |

## Prerequisites

- Kubernetes 1.24+
- `kubectl` (Kustomize is built in since 1.14)
- A default StorageClass (Kind, Minikube, Docker Desktop all provide one)
- Container image `banking-api:<tag>` available on the cluster

## Build and load the image (local Kind)

```bash
docker build -f docker/Dockerfile -t banking-api:latest .
kind load docker-image banking-api:latest --name banking   # if using Kind
```

Minikube:

```bash
minikube image load banking-api:latest
```

## Deploy

```bash
# Client-side validation (no cluster required)
kubectl apply -k k8s/overlays/dev --dry-run=client
kubectl apply -k k8s/overlays/staging --dry-run=client
kubectl apply -k k8s/overlays/prod --dry-run=client

# Development
kubectl apply -k k8s/overlays/dev
kubectl rollout status deployment/banking-api -n banking

# Staging / production (separate clusters recommended)
kubectl apply -k k8s/overlays/staging
kubectl apply -k k8s/overlays/prod
```

## Local smoke test

```bash
kubectl get pods -n banking
kubectl get svc -n banking

kubectl port-forward svc/banking-api 8080:8080 -n banking
curl -sf http://localhost:8080/api/v1/health
curl -sf http://localhost:8080/api/v1/health/live
curl -sf http://localhost:8080/api/v1/health/ready

kubectl logs -n banking -l app=banking-api --tail=100
```

Flyway runs on API startup against `postgres-service`. Redis is reached at `redis-service:6379`.

## Secrets

`k8s/base/secret.env` holds **placeholder** values so Kustomize builds work out of the box. They are not production credentials.

| Key | Used as |
|-----|---------|
| `db-password` | `POSTGRES_PASSWORD` and `SPRING_DATASOURCE_PASSWORD` |
| `redis-password` | Redis `--requirepass` and `SPRING_DATA_REDIS_PASSWORD` |
| `jwt-secret` | `JWT_SECRET` (min 32 characters) |

Replace before any shared cluster:

```bash
# Option A — edit secret.env, then re-apply the overlay
# Option B — Sealed Secrets (see k8s/base/secret-sealed.yaml)
kubectl create secret generic banking-secrets \
  --namespace banking \
  --from-literal=db-password='...' \
  --from-literal=redis-password='...' \
  --from-literal=jwt-secret='...' \
  --dry-run=client -o yaml \
  | kubeseal -o yaml > k8s/base/secret-sealed.yaml
```

Then add `secret-sealed.yaml` to the overlay `resources:` list and remove `secretGenerator` from `k8s/base/kustomization.yaml`.

**Never commit real passwords or JWT signing keys.**

## Probes and QoS

| Workload | Liveness | Readiness | QoS |
|----------|----------|-----------|-----|
| `banking-api` | `GET /api/v1/health/live` | `GET /api/v1/health/ready` | Burstable |
| `postgres` | `pg_isready` | `pg_isready` | Guaranteed |
| `redis` | `redis-cli ping` | `redis-cli ping` | Guaranteed |

HPA in prod needs metrics-server. Rolling updates use `maxUnavailable: 0`. PDB allows at most one API pod disruption.

## Storage

PVCs omit `storageClassName` (cluster default). For cloud disks, patch `volumeClaimTemplates` in the overlay and optionally add a StorageClass — see comments in `k8s/base/storage-class.yaml`.

## Rollback

```bash
kubectl rollout undo deployment/banking-api -n banking
kubectl rollout status deployment/banking-api -n banking
```
