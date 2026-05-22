# 项目概览

> 项目入口文档，提供全局视图。详细内容按交叉引用深入。
> 交叉引用: docs/glossary.md（术语）、constraints/infra.md（基础设施拓扑）、constraints/deployment.md（部署流程）

---

## 基本信息

| 维度 | 值 |
|---|---|
| 项目名 | yuemo-mall（月魔商城） |
| 架构 | 模块化单体（Modular Monolith）— Maven 多模块 + Gateway 网关 |
| 后端 | Java 17 / Maven 多模块 / Spring Boot 3.2.12 |
| 前端 | TypeScript 5.8 / Vite 6.3 / React 19.1 / Ant Design 5.24 |
| 数据库 | MySQL 8.0 / InnoDB / utf8mb4 |
| 缓存 | Redis 7.x |
| 消息队列 | RocketMQ 5.x |
| CI/CD | GitLab CI → gitlab-runner (Docker Executor) → Harbor Registry |
| 部署 | Docker Compose（后端双副本 + Nginx 负载均衡） |

---

## 后端技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.2.12 | 应用框架 |
| MyBatis-Plus | 3.5.9 | ORM |
| RocketMQ Starter | 2.3.5 | 消息队列 |
| Sentinel | 1.8.9 | 限流/熔断 |
| JJWT | 0.12.6 | JWT 认证 |
| Knife4j | 4.5.0 | API 文档 / OpenAPI 3 |
| Hutool | 5.8.34 | 工具库 |
| MapStruct | 1.6.3 | 对象映射 |

---

## 后端模块

```
yuemo-backend/
├── yuemo-server/          # 单体入口模块（Spring Boot 打包目标）
├── yuemo-gateway/         # 网关模块（Sentinel 限流 + JWT 鉴权 + 路由）
├── yuemo-admin/           # 后台管理模块
├── yuemo-common/          # 公共模块
│   ├── common-core        # 核心工具、统一响应(Result<T>)、错误码(ResultCode)、BaseEntity
│   ├── common-security    # JWT 认证、Redis Token 管理、密码加密(BCrypt)
│   └── common-mybatis     # MyBatis-Plus 配置、自动填充、逻辑删除
└── yuemo-modules/         # 业务模块
    ├── yuemo-user         # 用户模块（登录、注册、用户信息、余额管理）
    ├── yuemo-product      # 商品模块（商品、分类、SKU、品牌、规格、库存）
    ├── yuemo-order        # 订单模块（订单、订单项、订单日志）
    ├── yuemo-payment      # 支付模块（余额支付、策略模式多支付方式）
    ├── yuemo-cart         # 购物车模块（Redis 缓存 + DB 持久化）
    └── yuemo-promotion    # 促销模块（优惠券）
```

---

## 前端技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Vite | 6.3.x | 构建工具 |
| React | 19.1.x | UI 框架 |
| TypeScript | 5.8.x | 类型安全 |
| Ant Design | 5.24.x | UI 组件库 |
| @ant-design/icons | 5.6.x | 图标库 |
| Redux Toolkit | 2.7.x | 状态管理 |
| react-redux | 9.2.x | React-Redux 绑定 |
| react-router-dom | 7.5.x | 路由 |
| Axios | 1.9.x | HTTP 客户端 |
| dayjs | 1.11.x | 日期处理 |
| ESLint | — | 代码检查 |

---

## 部署架构

```
                        ┌──────────────────────────────────────────┐
                        │          192.168.1.55 生产服务器           │
                        │                                          │
  用户 ──── Nginx ──────┤──→ yuemo-mall-1 (宿主机:8081 → 容器:8080) │
   (HTTPS)   (LB)       │──→ yuemo-mall-2 (宿主机:8082 → 容器:8080) │
                        │──→ frontend-server (宿主机:8080, Nginx)   │
                        └──────────────┬───────────────────────────┘
                                       │
                        ┌──────────────┴───────────────────────────┐
                        │         192.168.1.56 基础设施服务器        │
                        │                                          │
                        │  MySQL 8.0 (:3306)                       │
                        │  Redis 7.x (:6379)                       │
                        │  RocketMQ 5.x (:9876 NameServer)         │
                        │  Sentinel (:8080 Dashboard)              │
                        └──────────────────────────────────────────┘

                        ┌──────────────────────────────────────────┐
                        │          192.168.1.53 构建服务器           │
                        │  gitlab-runner + Harbor Registry          │
                        └──────────────────────────────────────────┘

                        ┌──────────────────────────────────────────┐
                        │          192.168.1.54 代码仓库             │
                        │  GitLab (代码仓库 + CI/CD 调度)            │
                        └──────────────────────────────────────────┘
```

> 详细拓扑见 constraints/infra.md，部署流程见 constraints/deployment.md
