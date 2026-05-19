# CLAUDE.md — 月魔商城 AI Agent 配置

> Agent = Model + Harness。本文件是 Harness 入口，通过渐进式披露组织上下文。

---

## 1. 项目速览

| 维度 | 值 |
|---|---|
| 项目 | yuemo-mall（月魔商城） |
| 后端 | Java 17 / Maven 多模块 / Spring Boot 3.2.12 |
| 前端 | TypeScript / Vite 6 / React 19 / Ant Design 5 |
| CI/CD | GitLab CI → gitlab-runner (Docker Executor) |
| 部署 | Docker Compose（后端双副本 + Nginx 负载均衡） |

---

## 2. 上下文加载优先级

每次任务按以下优先级加载上下文。**禁止跳过**。

### 必读文件

| 优先级 | 文件 | 用途 |
|---|---|---|
| 1 | `CLAUDE.md`（本文件） | Harness 入口 |
| 2 | `.gitlab-ci.yml` | CI/CD 流水线 |
| 3 | `yuemo-backend/pom.xml` | 后端依赖和模块 |
| 4 | `yuemo-frontend/package.json` | 前端依赖和脚本 |

### 按任务类型按需加载

- **后端任务** → [.harness/docs/project-structure.md](.harness/docs/project-structure.md) 查看后端目录结构，然后读取相关源码
- **前端任务** → [.harness/docs/project-structure.md](.harness/docs/project-structure.md) 查看前端目录结构，然后读取相关源码
- **CI/CD 任务** → [.harness/rules/cicd-constraints.md](.harness/rules/cicd-constraints.md)
- **安全相关** → [.harness/rules/security.md](.harness/rules/security.md)
- **编码规范** → [.harness/rules/backend-coding.md](.harness/rules/backend-coding.md) / [.harness/rules/frontend-coding.md](.harness/rules/frontend-coding.md)

---

## 3. 规则与约束

所有详细规则位于 `.harness/rules/` 目录，按主题拆分：

| 规则文件 | 内容 |
|---|---|
| [rules/backend-coding.md](.harness/rules/backend-coding.md) | Java/Spring Boot 编码规范 |
| [rules/frontend-coding.md](.harness/rules/frontend-coding.md) | TypeScript/React 编码规范 |
| [rules/cicd-constraints.md](.harness/rules/cicd-constraints.md) | CI/CD 流水线约束 |
| [rules/security.md](.harness/rules/security.md) | 安全边界 |
| [rules/tools-permissions.md](.harness/rules/tools-permissions.md) | 工具权限与禁止操作 |

核心原则速记：
- 不添加不必要的注释
- 变量命名清晰，避免缩写
- 提交信息格式：`<类型>: <描述>`
- 上下文预算：单次任务不超过 80K tokens，大文件只读相关段落

---

## 4. 参考文档

详细参考文档位于 `.harness/docs/` 目录：

| 文档 | 内容 |
|---|---|
| [docs/project-overview.md](.harness/docs/project-overview.md) | 项目架构全景 |
| [docs/project-structure.md](.harness/docs/project-structure.md) | 完整目录结构速查 |
| [docs/infrastructure.md](.harness/docs/infrastructure.md) | 基础设施拓扑 |
| [docs/error-recovery.md](.harness/docs/error-recovery.md) | 常见错误与恢复策略 |
| [docs/api-conventions.md](.harness/docs/api-conventions.md) | API 约定与模式 |

---

## 5. 自定义 Skills

项目自定义 Skills 位于 `.harness/skills/` 目录。详见 [.harness/skills/README.md](.harness/skills/README.md)。

---

## 6. 验证循环

代码修改后必须执行验证，不通过则自动修复（最多 3 次）。

**后端验证：**
```bash
cd yuemo-backend && mvn compile -q && mvn test -pl <修改的模块> -q
```

**前端验证：**
```bash
cd yuemo-frontend && npx tsc --noEmit && npm run build
```

3 次仍失败 → 暂停，向用户报告错误和已尝试的修复。

---

## 7. 关键约束速记

- 禁止直接修改生产数据库
- 禁止将密码/密钥硬编码到代码中
- 禁止修改 `pom.xml` 中的 Spring Boot 版本（3.2.x）和 Java 版本（17），除非用户明确要求
- 禁止 `git push --force`（除非用户明确要求）
- 修改生产配置前必须向用户确认
- SSH 远程命令需逐条确认
