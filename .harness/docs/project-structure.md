# 项目结构速查

```
yuemo-mall/
├── .gitlab-ci.yml              # CI/CD 流水线（3 阶段：build → docker → deploy）
├── CLAUDE.md                   # AI Agent Harness 入口配置
├── .harness/                   # Harness 配置目录
│   ├── rules/                  # 项目规则与约束
│   ├── docs/                   # 参考文档
│   └── skills/                 # 自定义 Skills
├── yuemo-backend/
│   ├── pom.xml                 # 父 POM（Spring Boot 3.2.12, Java 17）
│   ├── Dockerfile              # 后端镜像（eclipse-temurin:17-jre-alpine）
│   ├── settings.xml            # Maven 镜像配置
│   ├── yuemo-server/           # 单体入口模块（打包目标）
│   ├── yuemo-gateway/          # 网关模块（Sentinel 限流 + JWT 鉴权）
│   ├── yuemo-admin/            # 后台管理模块
│   ├── yuemo-common/           # 公共模块（core / security / mybatis）
│   ├── yuemo-modules/          # 业务模块（user / product / order / payment / cart / promotion）
│   ├── docker/
│   │   ├── docker-compose.yml  # 后端部署编排（双副本 + Nginx + Redis + RocketMQ + Sentinel）
│   │   └── nginx/              # Nginx 配置（负载均衡）
│   └── sql/                    # 数据库初始化和迁移脚本
├── yuemo-frontend/
│   ├── package.json            # 前端依赖（Vite 6 + React 19 + Ant Design 5）
│   ├── vite.config.ts          # Vite 构建配置
│   ├── tsconfig.json           # TypeScript 配置
│   ├── src/                    # 前端源码
│   │   ├── components/         # 公共组件
│   │   ├── pages/              # 页面组件
│   │   ├── hooks/              # 自定义 Hooks
│   │   ├── stores/             # Redux Toolkit stores
│   │   ├── utils/              # 工具函数
│   │   │   └── request.ts      # axios 封装
│   │   ├── types/              # 类型定义
│   │   └── assets/             # 静态资源
│   └── docker/
│       ├── Dockerfile          # 前端镜像（nginx:1.24-alpine）
│       └── docker-compose.yml  # 前端部署编排
└── .claude/
    └── settings.local.json     # Claude Code 权限配置
```

## 后端任务上下文

当任务涉及后端代码时，按需读取：

| 文件 | 何时读取 |
|---|---|
| `yuemo-backend/yuemo-server/src/main/resources/application.yml` | 修改配置/环境变量时 |
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
| `yuemo-frontend/docker/Dockerfile` | 修改前端部署时 |
| `yuemo-frontend/docker/docker-compose.yml` | 修改前端服务编排时 |
