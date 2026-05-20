# 基础设施拓扑

## 服务器

```
192.168.1.54  GitLab（代码仓库 + CI/CD 调度）
192.168.1.53  gitlab-runner + Harbor（Docker Executor 执行构建/打包；私有镜像仓库）
192.168.1.55  生产服务器（Docker Compose 运行服务）
192.168.1.56  基础设施服务器（MySQL / Redis / RocketMQ / Sentinel Dashboard）
```

## 部署架构

```
用户请求
  ↓
前端 Nginx (192.168.1.55:8080)
  ├── /            → SPA 静态资源 (Vue)
  ├── /api/*       → proxy_pass → 192.168.1.56 (后端 Nginx)
  ├── /doc.html    → proxy_pass → 192.168.1.56 (Swagger UI)
  └── /v3/api-docs → proxy_pass → 192.168.1.56 (OpenAPI JSON)

后端 Nginx (192.168.1.56:80, server_name api.yuemo.com)
  ├── /api/user/login    → mall_backend (限流 5r/s burst=10)
  ├── /api/user/register → mall_backend (限流 5r/s burst=10)
  ├── /api/order/*       → mall_backend (限流 10r/s burst=20)
  ├── /api/*             → mall_backend (默认路由)
  └── /health            → 200 OK

mall_backend upstream (负载均衡，轮询 + 健康检查)
  ├── yuemo-mall-1:8080 (weight=1, max_fails=3, fail_timeout=30s)
  └── yuemo-mall-2:8080 (weight=1, max_fails=3, fail_timeout=30s)

后端服务 (Spring Boot 3.2 / JDK 17 / ZGC)
  ↓
  MySQL      (192.168.1.56:3306, HikariCP max=20)
  Redis      (192.168.1.56:6379, Lettuce max-active=50)
  RocketMQ   (192.168.1.56:9876, NameServer)
  Sentinel   (192.168.1.56:8080, Dashboard)
```

## Docker Compose 服务

### 后端 `yuemo-backend/docker/docker-compose.yml`

| 服务 | 容器名 | 镜像 | 宿主机端口 | 网络 |
|---|---|---|---|---|
| yuemo-mall-1 | yuemo-mall-1 | `192.168.1.53/yuemo-mall/backend:${IMAGE_TAG}` | 8081→8080 | backend-net |
| yuemo-mall-2 | yuemo-mall-2 | `192.168.1.53/yuemo-mall/backend:${IMAGE_TAG}` | 8082→8080 | backend-net |

环境变量（通过 `.env` 或 docker-compose 环境变量注入）：
- `DB_HOST` / `DB_USER` / `DB_PASS`
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`
- `ROCKETMQ_NS`
- `SENTINEL_DASHBOARD`
- `JWT_SECRET`
- `IMAGE_TAG`（CI 部署时设为 `${CI_COMMIT_SHORT_SHA}`）

日志卷：`./logs/yuemo-mall-{1,2} → /app/logs`

### 前端 `yuemo-frontend/docker/docker-compose.yml`

| 服务 | 容器名 | 镜像 | 宿主机端口 | 网络 |
|---|---|---|---|---|
| frontend-server | frontend-server | `192.168.1.53/yuemo-mall/frontend:${IMAGE_TAG}` | 8080→80 | frontend-net |

### Docker 网络

- `backend-net`（external）— 后端副本 + 后端 Nginx 通信
- `frontend-net`（external）— 前端容器

## CI/CD 流水线

```
main 分支推送
  ↓
┌─────────────────────────────────────────────────┐
│  Stage: build                                   │
│  ├── build:backend  (maven:3.9 + JDK 17)       │
│  │   → yuemo-server/target/*.jar                │
│  └── build:frontend (node:20-alpine)            │
│      → yuemo-frontend/dist/                     │
├─────────────────────────────────────────────────┤
│  Stage: docker                                  │
│  ├── docker:backend  (docker:27)                │
│  │   → push 192.168.1.53/yuemo-mall/backend     │
│  └── docker:frontend (docker:27)                │
│      → push 192.168.1.53/yuemo-mall/frontend    │
├─────────────────────────────────────────────────┤
│  Stage: deploy                                  │
│  ├── deploy:backend  (sshpass → 192.168.1.55)   │
│  │   → scp 配置 → docker compose up → 健康检查  │
│  └── deploy:frontend (sshpass → 192.168.1.55)   │
│      → scp 配置 → docker compose up → 健康检查  │
└─────────────────────────────────────────────────┘
```

触发规则：仅 `main` 分支，且对应子目录有变更时触发。

部署路径：`/opt/software/yuemo-mall/`

版本记录：部署成功后写入 `${DEPLOY_PATH}/.backend-version` / `.frontend-version`。

## 凭据管理

服务器凭据参见 memory 中的 `server-credentials`。

CI/CD 凭据通过 GitLab CI/CD Variables 管理：
- `SSH_PASSWORD` — 生产服务器 SSH 登录密码
- `HARBOR_USERNAME` / `HARBOR_PASSWORD` — Harbor 镜像仓库凭据
- `DB_PASS` — 数据库密码
- `JWT_SECRET` — JWT 签名密钥
