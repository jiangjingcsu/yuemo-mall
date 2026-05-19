# 基础设施拓扑

## 服务器

```
192.168.1.54  GitLab（代码仓库 + CI/CD 调度）
192.168.1.53  gitlab-runner（Docker Executor，执行构建/打包/部署）
192.168.1.55  生产服务器（Docker Compose 运行服务）
```

## 部署架构

```
用户请求
  ↓
Nginx (负载均衡 + 反向代理)
  ├── 后端副本 1 (port 8080)
  ├── 后端副本 2 (port 8081)
  └── 前端 (port 80)
      ↓
  Redis (缓存)
  RocketMQ (消息队列)
  Sentinel (限流/熔断)
  MySQL (数据库)
```

## Docker Compose 服务

后端 `docker-compose.yml` 包含：
- 后端服务（双副本）
- Nginx（负载均衡）
- Redis
- RocketMQ（NameServer + Broker）
- Sentinel Dashboard

前端 `docker-compose.yml` 包含：
- Nginx（静态文件服务）

## 凭据管理

服务器凭据参见 memory 中的 `server-credentials`。
CI/CD 凭据通过 GitLab CI/CD Variables 管理：
- `SSH_PRIVATE_KEY`
- `DB_PASS`
- `JWT_SECRET`
