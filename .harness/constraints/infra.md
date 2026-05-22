# 基础设施约束

> 基础设施变更红线。涉及服务器、网络、Docker、CI/CD 的操作约束。
> 交叉引用: constraints/ai-boundaries.md（总纲）、constraints/production.md（生产红线）、constraints/deployment.md（部署流程）
>
> **职责边界**：本文件关注基础设施**拓扑**（服务器/网络/Docker Compose 结构/请求链路），deployment.md 关注部署**流程**（构建→推送→部署→验证→回滚）。两者在"单副本下线"等场景有交叉，以本文件为拓扑权威，deployment.md 为流程权威。

---

## 绝对禁止

```yaml
禁止:
  # 基础设施操作
  - 删除生产数据卷（docker volume rm）
  - 关闭防火墙/安全组
  - 暴露数据库端口到公网
  - 硬编码服务器 IP/密码/密钥到代码中
  - 在生产服务器运行未测试/未知的脚本
  - 删除 GitLab CI Runner 配置

  # 版本锁定
  - 修改 Spring Boot 主版本（3.2.x）或 Java 版本（17）

  # 来自 production.md 的基础设施相关硬红线
  - 直接修改运行中的 docker-compose.yml（生产环境）
  - 修改 JWT 密钥（会导致所有 Token 失效）
  - 关闭 HTTPS/TLS
  - 降低密码加密强度
  - 开启 DEBUG 日志级别到生产
  - 单副本下线（至少保持 1 个副本运行）
```

---

## 基础设施拓扑（当前）

```yaml
服务器:
  192.168.1.55 — 生产服务器（Docker Compose 运行服务）
  192.168.1.56 — 基础设施服务器（MySQL / Redis / RocketMQ / Sentinel / 后端 Nginx）
  192.168.1.53 — gitlab-runner + Harbor（构建执行 + 私有镜像仓库）
  192.168.1.54 — GitLab（代码仓库 + CI/CD 调度）

服务:
  MySQL 8.0 — 192.168.1.56:3306
  Redis 7.x — 192.168.1.56:6379
  RocketMQ 5.x — 192.168.1.56:9876
  Sentinel 1.8.x — 192.168.1.56:8080（Dashboard）
  后端 Nginx — 192.168.1.56（负载均衡，非 Docker 部署）
  前端 Nginx — 192.168.1.55:8080（静态托管 + API 代理）
  后端 — 双副本（yuemo-mall-1:8081, yuemo-mall-2:8082）
  前端 — Nginx 静态托管（192.168.1.55:8080）

Docker 网络:
  backend-net — external network，后端服务 + 后端 Nginx 通信
  frontend-net — external network，前端服务通信

镜像仓库:
  Harbor — 192.168.1.53/yuemo-mall/
    - backend: 192.168.1.53/yuemo-mall/backend
    - frontend: 192.168.1.53/yuemo-mall/frontend

部署:
  Docker Compose 编排
  GitLab CI → gitlab-runner (Docker Executor)

⚠️ 已知偏差:
  - 后端 Nginx 以非 Docker 方式运行在 192.168.1.56，配置文件通过 CI scp 部署
```

---

## 请求链路

```yaml
外部请求 → 192.168.1.55:8080（前端 Nginx）
  ├── /            → SPA 静态资源（React）
  ├── /api/*       → proxy_pass http://192.168.1.56（后端 Nginx 负载均衡）
  ├── /doc.html    → proxy_pass http://192.168.1.56（Knife4j 文档）
  └── /v3/api-docs → proxy_pass http://192.168.1.56（OpenAPI 规范）

后端 Nginx（192.168.1.56）→ upstream 负载均衡
  ├── yuemo-mall-1:8080 (weight=1)
  └── yuemo-mall-2:8080 (weight=1)
```

---

## 必须确认的操作

```yaml
以下操作必须经用户逐条确认:
  - SSH 登录生产服务器
  - 修改 docker-compose.yml
  - 修改 Nginx 配置
  - 修改 CI/CD 流水线（.gitlab-ci.yml）
  - 重启 Docker 容器
  - 修改防火墙规则
  - 更新 SSL 证书
  - 修改域名/DNS 解析
  - 修改生产环境变量
```

---

## Docker Compose 约束

```yaml
修改 docker-compose.yml 时:
  - 不减少服务副本数（除非维护窗口）
  - 不修改健康检查端点（除非对应代码已就绪）
  - 不修改端口映射（可能导致冲突）
  - 环境变量通过 .env 文件注入，不硬编码
  - 敏感变量禁止留默认值（DB_PASS/REDIS_PASSWORD/JWT_SECRET 等）
  - 健康检查必须配置（后端 /actuator/health，前端 /health）
  - 必须配置资源限制（memory limit / cpu limit）
  - 必须配置重启策略（restart: unless-stopped 或 on-failure）
  - 日志驱动配置 json-file + max-size/max-file 限制
```
