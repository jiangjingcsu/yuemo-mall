# 工具权限与约束

## 允许的工具

| 工具 | 用途 | 约束 |
|---|---|---|
| 文件读写/编辑 | 编辑项目代码 | 禁止修改 `.git/` 目录 |
| `mvn` | 后端编译/测试 | 仅在 `yuemo-backend/` 下执行；允许 `compile`/`test`/`package`/`clean` |
| `npm` | 前端安装/构建/lint | 仅在 `yuemo-frontend/` 下执行；允许 `install`/`run dev`/`run build`/`run lint` |
| `git` | 版本控制 | 禁止 `push --force`；禁止直接 push 到 `main` 分支（除非用户明确要求） |
| `docker` / `docker compose` | 容器操作 | 仅限本地开发环境；`compose down` 需用户确认 |
| `ssh` | 远程服务器操作 | 需用户确认后才执行 |
| MySQL MCP | 数据库查询 | 仅允许 SELECT；禁止 DDL（DROP/ALTER/TRUNCATE）和 DML 写操作（INSERT/UPDATE/DELETE） |
| WebSearch / WebFetch | 查阅文档 | 仅用于查阅技术文档，禁止访问内网地址 |

## 禁止的操作

- 禁止直接修改生产数据库（安全边界详见 `security.md`）
- 禁止将密码/密钥硬编码到代码中（安全边界详见 `security.md`）
- 禁止删除 `yuemo-backend/sql/` 下的数据库迁移文件
- 禁止修改 `pom.xml` 中的 Spring Boot 版本（3.2.12）和 Java 版本（17），除非用户明确要求
- 禁止在未运行测试和 lint 的情况下声称修改完成
- 禁止修改 `.harness/rules/` 下的规则文件，除非用户明确要求

## 修改完成检查清单

| 项目类型 | 必须执行 |
|---|---|
| 后端 Java | `mvn compile` + `mvn test`（在 `yuemo-backend/` 下） |
| 前端 TS/React | `npm run build` + `npm run lint`（在 `yuemo-frontend/` 下） |

## 上下文窗口预算

- 单次任务上下文不超过 80K tokens
- 读取源码文件时，优先读取方法签名和类结构，避免读取完整实现
- 超过 500 行的文件，只读取相关段落
