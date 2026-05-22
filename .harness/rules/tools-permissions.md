# 工具权限与约束

## 允许的工具

| 工具 | 用途 | 约束 |
|---|---|---|
| 文件读写/编辑 | 编辑项目代码 | 禁止修改 `.git/` 目录；禁止修改二进制文件（图片/字体/编译产物）；批量删除文件需用户确认 |
| `mvn` | 后端编译/测试 | 仅在 `yuemo-backend/` 下执行；允许 `compile`/`test`/`package`/`clean`；项目无 Maven Wrapper，依赖系统 mvn |
| `mvn flyway:*` | 数据库迁移管理 | 仅在 `yuemo-backend/` 下执行；允许 `flyway:info`/`flyway:validate`（只读）；`flyway:migrate` 需用户确认；禁止 `flyway:clean`/`flyway:undo` |
| `npm` | 前端安装/构建/lint | 仅在 `yuemo-frontend/` 下执行；允许 `install`/`run dev`/`run build`/`run lint`/`run preview`；首次须先 `npm install` |
| `git` | 版本控制 | 禁止 `push --force`；禁止直接 push 到 `main` 分支（除非用户明确要求）；禁止 `git reset --hard`（除非用户明确要求）；禁止 `git clean -fd` |
| `docker` / `docker compose` | 容器操作 | 仅限本地开发环境；`compose down` 需用户确认；配置文件在 `yuemo-backend/docker/` 和 `yuemo-frontend/docker/` |
| `ssh` | 远程服务器操作 | 需用户确认后才执行 |
| MySQL MCP | 数据库查询 | 仅允许 SELECT；禁止 DDL（DROP/ALTER/TRUNCATE）和 DML 写操作（INSERT/UPDATE/DELETE）；仅限查询开发环境数据库 `yuemo_mall`；禁止查询 `information_schema`/`mysql`/`performance_schema` 等系统库 |
| Redis MCP | 缓存查询与调试 | 仅允许 `get`/`list`（只读）；禁止 `set`/`delete`（写操作需用户确认）；禁止操作以下键空间：`token:blacklist:*`/`user:role:*`（鉴权）、`payment:callback:*`（支付幂等）、`mq:consumed:*`（消息幂等）、`coupon:receive:*`（优惠券去重） |
| WebSearch / WebFetch | 查阅文档 | 仅用于查阅技术文档，禁止访问内网地址 |

## 禁止的操作

> 安全硬红线统一定义在 `constraints/ai-boundaries.md`，以下仅列出工具级禁止项。

- 禁止修改 `.git/` 目录
- 禁止删除/修改 `yuemo-backend/sql/` 下的数据库初始化文件（→ constraints/ai-boundaries.md）
- 禁止删除/修改 `yuemo-backend/yuemo-server/src/main/resources/db/migration/` 下已执行的 Flyway 迁移文件（新增迁移文件允许）
- 禁止修改 `pom.xml` 中的 Spring Boot 版本（3.2.12）和 Java 版本（17）（→ constraints/ai-boundaries.md）
- 禁止在未运行测试和 lint 的情况下声称修改完成
- 禁止修改 `.harness/rules/` 下的规则文件，除非用户明确要求
- 禁止修改 `.harness/constraints/` 下的约束文件，除非用户明确要求
- 禁止将 `.env` 文件（非 `.env.example`）的内容输出到对话中（可能包含密钥）
- 禁止修改 `.env.production` 中的 API 地址和密钥配置（需用户确认）
- `.harness/memory/` 下的文件可以追加，但禁止删除已有记录
- `.harness/docs/` 下的文件可以按需更新

## 修改完成检查清单

| 项目类型 | 前置条件 | 必须执行 |
|---|---|---|
| 后端 Java | 系统已安装 Maven 3.9+ | `mvn compile`（在 `yuemo-backend/` 下）；`mvn test` 如需数据库则跳过并说明 |
| 前端 TS/React | 已执行 `npm install` | `npm run build` + `npm run lint`（在 `yuemo-frontend/` 下） |

检查失败处理:
- 编译失败：必须修复，不可跳过
- 测试失败：优先修复；若为环境问题（如数据库未启动），须在输出中明确说明
- Lint 失败：优先修复；若为既有问题，须标注非本次修改引入

## 上下文窗口预算

- 单次任务上下文不超过 80K tokens
- 读取源码文件时，优先读取方法签名和类结构，避免读取完整实现
- 超过 500 行的文件，只读取相关段落
