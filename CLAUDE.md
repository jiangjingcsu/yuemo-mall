# CLAUDE.md — 月魔商城 AI Runtime Bootloader

> **定位**: Runtime Bootloader。只负责引导 AI 去哪里，不负责实现细节。
> 实现层: `.harness/`

---

## 1. 项目速览

| 维度 | 值 |
|---|---|
| 项目 | yuemo-mall（月魔商城） |
| 后端 | Java 17 / Maven 多模块 / Spring Boot 3.2.12 |
| 前端 | TypeScript / Vite 6 / React 19 / Ant Design 5 |
| 部署 | Docker Compose / GitLab CI |

---

## 2. Philosophy

每次任务遵守的原则：

1. **Progressive Disclosure First** — 渐进式加载上下文，按需深入，不一次性加载全部
2. **Graph First Context Resolution** — 优先通过 `code-review-graph` MCP 解析代码结构，而非扫描 markdown（⚠️ MCP 待实现，当前降级为 SearchCodebase + Grep）
3. **Minimal Context Loading** — 只加载任务所需的最小上下文，单次不超过 80K tokens
4. **Constraints Before Execution** — 执行前先验证硬约束，违反即停止
5. **Evaluation Before Output** — 输出前通过评估管线，不通过则自动修复（最多 3 次）
6. **Memory Prevents Drift** — 读取 `.harness/memory/` 避免重复踩坑
7. **Workflow Before Action** — 复杂任务先匹配工作流再执行
8. **Drift Detection Before Execution** — 执行前检查 `.harness/` 文档与代码实际状态是否一致（`skills/drift-detection/SKILL.md`），L3 漂移停止执行
9. **No Full Repository Scanning** — 禁止全量扫描仓库
10. **Context Isolation** — 大文件只读相关段落，不全文加载
11. **Risk-Aware Execution** — 高风险操作（生产变更、force push）必须逐条确认

---

## 3. Lifecycle

每个任务经过 6 个阶段（与 execution-engine.md 对齐）：

```
1. Task Intake      — 解析意图、确定任务类型、路由到 workflow + agent
2. Pre-check        — 加载 executor、验证 constraints 红线、漂移检测（drift-detection）、不通过即终止
3. Context Loading  — 按 context-router.md 策略加载、Token 预算检查
4. Execution        — 按 executor 步骤执行、每步验证、失败重试（最多 3 次）
5. Evaluation       — 代码质量 / 架构合规 / 幻觉检测 / 安全 / 风险评估
6. Output           — 综合评估报告：PASS → 继续 / WARN → 建议修复 / FAIL → BLOCK
```

实现: `.harness/runtime/execution-engine.md`

---

## 4. Routing

任务类型到入口的轻量映射。详细路由规则在 `.harness/runtime/` 中。

| 任务类型 | Workflow | Agent | 补充规则 |
|---|---|---|---|
| 后端开发 | feature-development.md | yuemo-backend | backend-coding.md |
| 前端开发 | feature-development.md | yuemo-frontend | frontend-coding.md |
| 架构设计 | architecture-design.md | yuemo-architect | — |
| 代码审查 | code-review.md | yuemo-reviewer | evaluation/ |
| 安全审查 | code-review.md | yuemo-security | constraints/ |
| Bug 修复 | bug-fix.md | yuemo-backend / yuemo-frontend | — |
| 重构 | refactor.md | yuemo-architect + yuemo-backend/yuemo-frontend | — |
| CI/CD / 部署 | — | yuemo-devops | constraints/deployment.md |
| 测试 | — | yuemo-tester | — |
| 数据库变更 | — | yuemo-backend | constraints/database.md + database-governance.md |
| 缓存/Redis | — | yuemo-backend | redis-governance.md |
| 消息队列 | — | yuemo-backend | mq-governance.md |
| API 设计 | — | yuemo-backend | skills/api-design + api-governance.md |
| DDD 建模 | — | yuemo-architect | skills/ddd-design + ddd-governance.md |
| 业务理解 | — | yuemo-architect | docs/business-domain.md + data-flow.md |

> 完整路由决策：`.harness/runtime/agent-router.md`（Agent 路由）+ `.harness/runtime/workflow-router.md`（意图→工作流映射）

---

## 5. Priority Rules

冲突时按以下优先级仲裁：

```
constraints > workflows
security > convenience
evaluation > generation
production safety > speed
graph context > markdown assumptions
minimal context > full loading
```

**冲突场景示例**：

| 场景 | 低优先级选项 | 高优先级选项 | 仲裁结果 |
|---|---|---|---|
| workflow 要求快速交付，但代码违反 module-boundary | 跳过边界检查（convenience） | 遵守模块边界（constraints） | **遵守边界** |
| 用户要求直接生成代码，但未读 memory | 直接生成（speed） | 先读 memory 避免踩坑（constraints） | **先读 memory** |
| 代码生成完成但 evaluation 未通过 | 直接输出（generation） | 修复后重新评估（evaluation） | **修复后重评** |
| 重构方案涉及生产数据库变更 | 直接执行（speed） | 逐条确认（production safety） | **逐条确认** |
| context-router 建议加载 5 个文件，但 Token 预算不足 | 全部加载 | 按优先级裁剪（minimal context） | **裁剪加载** |

---

## 6. Boundaries

**绝对禁止**:

| 类别 | 禁止项 |
|---|---|
| 上下文 | 全量扫描仓库 · 加载所有 markdown · 无界上下文扩展 |
| 流程 | 跳过评估管线 · 忽略约束文件 · 未经规划直接执行复杂任务 |
| 架构 | 单一 Agent 处理所有任务 · 忽略 graph context（当 MCP 可用时） |
| 生产 & 安全 | → constraints/ai-boundaries.md（完整硬红线定义） |
| Git | `git push --force`（未经明确允许） |

---

## 7. Entry Points

CLAUDE.md 只负责"告诉 AI 去哪里"。实现在下沉层：

| 入口 | 路径 | 职责 |
|---|---|---|
| **Runtime Engine** | `.harness/runtime/` | 生命周期编排、上下文加载策略、状态管理、Agent 调度 |
| **Executors** | `.harness/executors/` | 执行器实现：代码生成、审查、重构、分析、部署、工作流管线 |
| **Graph Intelligence** | `code-review-graph` MCP | 动态上下文解析、依赖分析、调用关系、影响范围（⚠️ 待实现） |
| **Workflows** | `.harness/workflows/` | 执行工作流、SOP、任务管线 |
| **Agents** | `.claude/agents/` | 领域 Agent 定义（7 个：backend/frontend/devops/architect/reviewer/tester/security） |
| **Agent 角色提示词** | `.harness/agents/` | 详细角色定义、权限边界、协作规则（Agent 运行时加载） |
| **Evaluation** | `.harness/evaluation/` | 代码质量、幻觉检测、安全评估、风险分析、输出验证 |
| **Constraints** | `.harness/constraints/` | 硬约束、生产红线、数据库规范、部署安全 |
| **Governance** | `.harness/rules/` | 编码规范、架构治理、设计模式、DDD、API 规范 |
| **Memory** | `.harness/memory/` | 架构决策、事故教训、技术债务、演进路线 |
| **Docs** | `.harness/docs/` | 业务领域、数据流、项目结构、中间件参考 |
| **Skills** | `.harness/skills/` | 可复用领域技能（DDD/SQL/缓存/API 设计等） |
| **Observability** | `.harness/observability/` | 可观测性策略 |
