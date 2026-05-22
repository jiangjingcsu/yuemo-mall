# 项目结构速查

> 项目目录结构参考。快速定位文件位置，按需加载上下文。
> 基础设施详情: `.harness/docs/infrastructure.md`
> 技术栈: `.harness/memory/tech-stack.md`

```
yuemo-mall/
├── .gitlab-ci.yml              # CI/CD 流水线（4 阶段：build → test → docker → deploy）
├── CLAUDE.md                   # AI Agent Harness 入口配置
├── .harness/                   # Harness 配置目录
│   ├── agents/                 # 领域 Agent 定义（backend / frontend / architect / reviewer / security / devops / tester）
│   ├── constraints/            # 硬约束红线（production / database / deployment / infra / payment / ai-boundaries）
│   ├── docs/                   # 参考文档（business-domain / data-flow / infrastructure / api-conventions 等）
│   ├── evaluation/             # 评估管线（code-quality / security-check / hallucination-check / risk-analysis 等）
│   ├── executors/              # 执行器（code / review / analysis / refactor / deploy / workflow）
│   ├── memory/                 # 长期记忆（decisions / incidents / anti-patterns / known-issues / tech-stack 等）
│   ├── observability/          # 可观测性策略
│   ├── rules/                  # 治理规则（database-governance / redis-governance / module-boundary / security 等）
│   ├── runtime/                # 运行时引擎（execution-engine / agent-router / context-router / workflow-router 等）
│   ├── skills/                 # 可复用技能（sql-review / transaction-analysis / ddd-design / cache-design 等）
│   └── workflows/              # 工作流（feature-development / bug-fix / code-review / refactor / architecture-design）
├── yuemo-backend/
│   ├── pom.xml                 # 父 POM（Spring Boot 3.2.12, Java 17）
│   ├── Dockerfile              # 后端镜像（eclipse-temurin:17-jre-alpine）
│   ├── settings.xml            # Maven 镜像配置
│   ├── yuemo-server/           # 单体入口模块（打包目标）
│   │   └── src/main/resources/
│   │       ├── application.yml           # Spring Boot 配置（HikariCP / Lettuce / RocketMQ）
│   │       ├── logback-spring.xml        # 日志配置
│   │       └── db/migration/             # Flyway 迁移脚本（V1~V4）
│   ├── yuemo-gateway/          # 网关模块（Sentinel 限流 + JWT 鉴权）
│   │   └── src/main/resources/
│   │       └── lua/                      # 限流 Lua 脚本（rate_limit.lua）
│   ├── yuemo-admin/            # 后台管理模块
│   ├── yuemo-common/           # 公共模块（core / security / mybatis）
│   ├── yuemo-modules/          # 业务模块（user / product / order / payment / cart / promotion）
│   ├── docker/
│   │   ├── docker-compose.yml  # 后端部署编排（双副本）
│   │   └── nginx/              # Nginx 配置（负载均衡 + 限流）
│   └── sql/                    # 数据库初始化脚本和工具（非 Flyway 迁移）
├── yuemo-frontend/
│   ├── package.json            # 前端依赖（Vite 6 + React 19 + Ant Design 5）
│   ├── vite.config.ts          # Vite 构建配置
│   ├── tsconfig.json           # TypeScript 配置
│   ├── index.html              # SPA 入口 HTML
│   ├── .env                    # 环境变量（通用）
│   ├── .env.development        # 环境变量（开发）
│   ├── .env.production         # 环境变量（生产）
│   ├── .env.test               # 环境变量（测试）
│   ├── src/                    # 前端源码
│   │   ├── main.tsx            # 应用入口
│   │   ├── App.tsx             # 路由配置
│   │   ├── api/                # API 请求层（user / product / order / payment / cart / coupon）
│   │   ├── components/         # 公共组件（product/）
│   │   ├── layouts/            # 布局组件（MainLayout）
│   │   ├── pages/              # 页面组件（user / product / order / payment / cart / coupon / admin）
│   │   ├── stores/             # Redux Toolkit stores（userSlice / cartSlice）
│   │   ├── utils/              # 工具函数
│   │   │   └── request.ts      # axios 封装
│   │   ├── index.css           # 全局样式
│   │   └── assets/             # 静态资源
│   └── docker/
│       ├── Dockerfile          # 前端镜像（多阶段构建 → nginx:stable-alpine）
│       ├── docker-compose.yml  # 前端部署编排
│       └── nginx.conf          # 前端 Nginx 配置
└── .claude/
    └── settings.local.json     # Claude Code 权限配置
```

## 后端任务上下文

当任务涉及后端代码时，按需读取：

| 文件 | 何时读取 |
|---|---|
| `yuemo-backend/yuemo-server/src/main/resources/application.yml` | 修改配置/环境变量时 |
| `yuemo-backend/yuemo-server/src/main/resources/logback-spring.xml` | 修改日志配置时 |
| `yuemo-backend/yuemo-server/src/main/resources/db/migration/` | 新增/修改数据库迁移时 |
| `yuemo-backend/Dockerfile` | 修改构建/部署流程时 |
| `yuemo-backend/docker/docker-compose.yml` | 修改服务编排时 |
| `yuemo-backend/docker/nginx/nginx.conf` | 修改反向代理/负载均衡时 |
| 对应模块的 `pom.xml` | 修改该模块依赖时 |

## 前端任务上下文

当任务涉及前端代码时，按需读取：

| 文件 | 何时读取 |
|---|---|
| `yuemo-frontend/vite.config.ts` | 修改构建配置时 |
| `yuemo-frontend/tsconfig.json` | 修改 TypeScript 配置时 |
| `yuemo-frontend/src/App.tsx` | 修改路由/布局时 |
| `yuemo-frontend/src/api/` | 修改 API 接口层时 |
| `yuemo-frontend/.env*` | 修改环境变量时 |
| `yuemo-frontend/docker/Dockerfile` | 修改前端部署时 |
| `yuemo-frontend/docker/docker-compose.yml` | 修改前端服务编排时 |
