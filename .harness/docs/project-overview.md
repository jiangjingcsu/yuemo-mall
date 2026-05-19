# 项目概览

## 基本信息

| 维度 | 值 |
|---|---|
| 项目名 | yuemo-mall（月魔商城） |
| 架构 | 单体分层（Spring Boot 3.2 + React 19） |
| 后端 | Java 17 / Maven 多模块 / Spring Boot 3.2.12 |
| 前端 | TypeScript / Vite 6 / React 19 / Ant Design 5 |
| CI/CD | GitLab CI → gitlab-runner (Docker Executor) |
| 部署 | Docker Compose（后端双副本 + Nginx 负载均衡） |

## 后端技术栈

- Spring Boot 3.2.12
- MyBatis-Plus（ORM）
- Redis（缓存）
- RocketMQ（消息队列）
- Sentinel（限流/熔断）
- Knife4j（API 文档 / OpenAPI 3）
- JWT（认证）

## 后端模块

```
yuemo-backend/
├── yuemo-server/      # 单体入口模块（Spring Boot 打包目标）
├── yuemo-gateway/     # 网关模块（Sentinel 限流 + JWT 鉴权）
├── yuemo-admin/       # 后台管理模块
├── yuemo-common/      # 公共模块
│   ├── core           # 核心工具类
│   ├── security       # 安全相关
│   └── mybatis        # MyBatis 配置
└── yuemo-modules/     # 业务模块
    ├── user           # 用户模块
    ├── product        # 商品模块
    ├── order          # 订单模块
    ├── payment        # 支付模块
    ├── cart           # 购物车模块
    └── promotion      # 促销模块
```

## 前端技术栈

- Vite 6
- React 19
- TypeScript
- Ant Design 5
- Redux Toolkit
- Axios

## 部署架构

```
用户 → Nginx (负载均衡) → 后端副本1 (port 8080)
                        → 后端副本2 (port 8081)
                        → 前端 (port 80)
         ↓
    Redis (缓存)
    RocketMQ (消息队列)
    Sentinel (限流)
```
