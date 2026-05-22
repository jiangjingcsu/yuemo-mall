# 基础设施拓扑

> 项目基础设施参考文档。记录服务器、网络、部署架构和 CI/CD 流水线的当前状态。
> 关联约束: `.harness/constraints/infra.md` / `deployment.md` / `production.md`
> 技术栈: `.harness/memory/tech-stack.md`

---

## 服务器

```
192.168.1.54  GitLab（代码仓库 + CI/CD 调度）
192.168.1.53  gitlab-runner + Harbor（Docker Executor 执行构建/打包；私有镜像仓库）
192.168.1.55  生产服务器（Docker Compose 运行服务）
192.168.1.56  基础设施服务器（MySQL / Redis / RocketMQ / Sentinel Dashboard）
```

---

## 部署架构

```
用户请求
  ↓
前端 Nginx (192.168.1.55:8080)
  ├── /            → SPA 静态资源 (React)
  ├── /api/*       → proxy_pass → 192.168.1.56 (后端 Nginx)
  ├── /doc.html    → proxy_pass → 192.168.1.56 (Swagger UI)
  ├── /v3/api-docs → proxy_pass → 192.168.1.56 (OpenAPI JSON)
  ├── /webjars/    → proxy_pass → 192.168.1.56 (Swagger 静态资源)
  └── 静态资源(.js/.css/.png等) → 缓存 30 天 (Cache-Control: public, immutable)

后端 Nginx (192.168.1.56:80, server_name api.yuemo.com)
  ├── /api/user/login    → mall_backend (限流 5r/s burst=10)
  ├── /api/user/register → mall_backend (限流 5r/s burst=10)
  ├── /api/order/*       → mall_backend (限流 10r/s burst=20)
  ├── /api/*             → mall_backend (默认路由)
  ├── /health            → 200 OK
  └── 默认 server_name _ → 444 (拒绝非法域名访问)

mall_backend upstream (负载均衡，轮询 + 健康检查 + keepalive)
  ├── yuemo-mall-1:8080 (weight=1, max_fails=3, fail_timeout=30s)
  └── yuemo-mall-2:8080 (weight=1, max_fails=3, fail_timeout=30s)
  keepalive 64

后端服务 (Spring Boot 3.2 / JDK 17 / ZGC)
  ↓
  MySQL      (192.168.1.56:3306, HikariCP max=20, min-idle=5, connection-timeout=15s)
  Redis      (192.168.1.56:6379, Lettuce max-active=50, max-idle=20, min-idle=10)
  RocketMQ   (192.168.1.56:9876, NameServer)
  Sentinel   (192.168.1.56:8080, Dashboard)
```

---

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

⚠️ **安全风险**: docker-compose.yml 中环境变量含明文默认密码，违反 `constraints/production.md` 约束。生产环境必须通过 `.env` 文件注入，禁止使用默认值。

其他配置：
- `restart: always` — 容器异常退出自动重启
- `HEALTHCHECK` — Dockerfile 中定义，`/actuator/health` 每 30s 检查一次
- 日志卷：`./logs/yuemo-mall-{1,2} → /app/logs`

### 前端 `yuemo-frontend/docker/docker-compose.yml`

| 服务 | 容器名 | 镜像 | 宿主机端口 | 网络 |
|---|---|---|---|---|
| frontend-server | frontend-server | `192.168.1.53/yuemo-mall/frontend:${IMAGE_TAG}` | 8080→80 | frontend-net |

构建参数：
- `API_PROXY_TARGET` — 前端 Nginx 代理目标（默认 `http://192.168.1.56`，构建时通过 `sed` 替换）

其他配置：
- `restart: always`
- `HEALTHCHECK` — Dockerfile 中定义，`wget http://localhost:80/` 每 30s 检查一次

### Docker 网络

- `backend-net`（external）— 后端副本 + 后端 Nginx 通信
- `frontend-net`（external）— 前端容器

---

## 连接池配置

### HikariCP（MySQL）

| 参数 | 值 | 说明 |
|---|---|---|
| minimum-idle | 5 | 最小空闲连接 |
| maximum-pool-size | 20 | 最大连接数 |
| idle-timeout | 300000 (5min) | 空闲超时 |
| connection-timeout | 15000 (15s) | 获取连接超时 |
| max-lifetime | 1800000 (30min) | 连接最大存活 |
| initialization-fail-timeout | -1 | 初始化失败持续重试 |

### Lettuce（Redis）

| 参数 | 值 | 说明 |
|---|---|---|
| max-active | 50 | 最大活跃连接 |
| max-idle | 20 | 最大空闲连接 |
| min-idle | 10 | 最小空闲连接 |

---

## CI/CD 流水线

```
main 分支推送
  ↓
┌─────────────────────────────────────────────────┐
│  Stage: build                                   │
│  ├── build:backend  (maven:3.9 + JDK 17)       │
│  │   → yuemo-server/target/*.jar                │
│  │   ⚠️ 当前使用 -DskipTests 跳过测试            │
│  └── build:frontend (node:20-alpine)            │
│      → yuemo-frontend/dist/                     │
├─────────────────────────────────────────────────┤
│  Stage: test                                    │
│  ├── test:backend  (maven:3.9 + JDK 17)        │
│  │   → mvn test                                 │
│  └── test:frontend (node:20-alpine)             │
│      → npm run lint                             │
├─────────────────────────────────────────────────┤
│  Stage: docker                                  │
│  ├── docker:backend  (docker:27)                │
│  │   needs: build:backend + test:backend        │
│  │   → push 192.168.1.53/yuemo-mall/backend     │
│  └── docker:frontend (docker:27)                │
│      needs: build:frontend + test:frontend      │
│      → push 192.168.1.53/yuemo-mall/frontend    │
├─────────────────────────────────────────────────┤
│  Stage: deploy                                  │
│  ├── deploy:backend  (alpine:3.20 → 192.168.1.55)│
│  │   → scp 配置 → docker compose up → 健康检查  │
│  │   ⚠️ 当前使用 --force-recreate 非滚动更新      │
│  └── deploy:frontend (alpine:3.20 → 192.168.1.55)│
│      → scp 配置 → docker compose up → 健康检查  │
└─────────────────────────────────────────────────┘
```

触发规则：仅 `main` 分支，且对应子目录有变更时触发。

缓存策略：
- `maven-cache` → `.m2/repository/`（后端构建/测试共享）
- `npm-cache` → `yuemo-frontend/node_modules/`（前端构建/测试共享）

镜像标签：
- `{CI_COMMIT_SHORT_SHA}` — 不可变版本标签
- `latest` — 指向最新部署版本

部署路径：`/opt/software/yuemo-mall/`

版本记录：部署成功后写入 `${DEPLOY_PATH}/.backend-version` / `.frontend-version`。

### 已知偏差

| 偏差 | 当前行为 | 应改为 | 优先级 |
|---|---|---|---|
| build:backend 使用 -DskipTests | 跳过单元测试 | 移除 -DskipTests，由 test 阶段执行 | 高 |
| deploy:backend 使用 --force-recreate | 全量重建所有副本 | 逐副本滚动更新 | 高 |
| deploy:backend 健康检查用 /doc.html | 404 也视为成功 | 改用 /actuator/health 严格验证 | 中 |

---

## 凭据管理

CI/CD 凭据通过 GitLab CI/CD Variables 管理：
- `SSH_PASSWORD` — 生产服务器 SSH 登录密码
- `HARBOR_USERNAME` / `HARBOR_PASSWORD` — Harbor 镜像仓库凭据
- `DB_PASS` — 数据库密码
- `JWT_SECRET` — JWT 签名密钥
- `API_PROXY_TARGET` — 前端 Nginx 代理目标地址

⚠️ 敏感凭据禁止硬编码到代码或 docker-compose.yml 中，详见 `constraints/production.md`。
