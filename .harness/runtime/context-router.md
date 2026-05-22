# 上下文路由器

> AI Agent 根据任务类型动态加载相关上下文的规则引擎。本文件是上下文加载策略的唯一权威定义。
> 包含：路由映射 + 加载优先级 + Token 预算控制 + 上下文刷新策略。

---

## 1. 路由规则

### 1.1 按任务类型

| 任务类型 | 必读文档 | 必读规则 | 可选 Skill |
|---|---|---|---|
| 后端新功能 | project-overview, business-domain, data-flow | architecture-governance, module-boundary, api-governance, architecture-decision, design-pattern-governance, extensibility-governance, domain-modeling | skills/requirement-breakdown/SKILL.md, skills/api-design/SKILL.md, skills/ddd-design/SKILL.md |
| 前端新页面 | project-structure, api-conventions | frontend-coding, module-boundary | skills/requirement-breakdown/SKILL.md |
| Bug 修复 | project-structure, error-recovery | architecture-governance, module-boundary, backend-coding 或 frontend-coding | skills/root-cause-analysis/SKILL.md |
| 重构 | project-overview, module-responsibility | architecture-governance, module-boundary, ddd-governance, code-smell-governance | skills/refactor-analysis/SKILL.md |
| 数据库变更 | project-structure | database-governance, module-boundary | skills/sql-review/SKILL.md |
| 缓存/Redis | data-flow | redis-governance | skills/redis-design/SKILL.md, skills/cache-design/SKILL.md |
| 消息队列 | data-flow | mq-governance | skills/transaction-analysis/SKILL.md |
| API 设计 | api-conventions | api-governance | skills/api-design/SKILL.md |
| 架构设计 | project-overview, business-domain, module-responsibility, data-flow | architecture-governance, module-boundary, ddd-governance, architecture-decision, design-pattern-governance, extensibility-governance | skills/ddd-design/SKILL.md, skills/domain-modeling/SKILL.md |
| 性能优化 | data-flow | database-governance, redis-governance | skills/performance-review/SKILL.md |
| 微服务评估 | module-responsibility, data-flow | module-boundary, ddd-governance | skills/microservice-readiness/SKILL.md |
| 代码审查/坏味道 | project-structure | code-smell-governance, design-pattern-governance | skills/code-verify/SKILL.md |
| 安全审查 | project-structure | security.md, architecture-governance | skills/code-verify/SKILL.md |
| CI/CD / 部署 | infrastructure | cicd-constraints, tools-permissions | — |
| 测试 | project-structure | backend-coding 或 frontend-coding | — |
| DDD 建模 | business-domain, module-responsibility | ddd-governance, domain-modeling | skills/ddd-design/SKILL.md |
| 所有任务 | CLAUDE.md | agent-behavior | — |

### 1.2 按模块

| 模块 | 额外上下文 |
|---|---|
| yuemo-user | business-domain.md §2 |
| yuemo-product | business-domain.md §3 |
| yuemo-order | business-domain.md §4, data-flow.md §1 |
| yuemo-payment | business-domain.md §5, data-flow.md §2-3 |
| yuemo-cart | business-domain.md §7, data-flow.md §5 |
| yuemo-promotion | business-domain.md §6 |
| yuemo-gateway | infrastructure.md, constraints/production.md（白名单/鉴权配置） |
| yuemo-admin | module-responsibility.md §2.11, rules/security.md §4（接口安全） |
| yuemo-server | project-structure.md（聚合模块结构） |
| common-core | project-structure.md（基础组件） |
| common-security | infrastructure.md（JWT 配置）, constraints/production.md（密钥管理） |
| common-mybatis | rules/database-governance.md（分页/自动填充配置） |

### 1.3 按场景补充

| 场景 | 补充文档 |
|---|---|
| 涉及中间件（Redis/RocketMQ/MySQL） | middleware.md |
| 遇到不熟悉的术语 | glossary.md |
| 涉及基础设施拓扑 | infrastructure.md |
| 涉及错误处理/恢复 | error-recovery.md |

---

## 2. 加载优先级

```yaml
L0 — 始终加载（每次任务）:
  - CLAUDE.md
  - context-router.md（本文件）
  - constraints/ai-boundaries.md（硬红线，违反即停止）
  - memory/decisions.md（架构决策，避免重蹈覆辙）
  - memory/anti-patterns.md（已知反模式，避免重复踩坑）

L1 — 高优先级（任务开始时加载）:
  - 对应 workflow 文件
  - 对应 executor 文件
  - 对应 agent 定义文件

L2 — 按需加载（执行过程中加载）:
  - 相关 rule 文件
  - 相关 constraint 文件:
      数据库变更 → constraints/database.md
      部署相关 → constraints/deployment.md, constraints/production.md
      支付相关 → constraints/payment.md
      基础设施 → constraints/infra.md
  - 相关 doc 文件
  - 目标模块源码（只读相关部分）
  - 漂移检测: skills/drift-detection/SKILL.md（任务执行前自动触发，见 execution-engine.md §2）

L3 — 延迟加载（需要时加载）:
  - memory/incidents.md
  - memory/known-issues.md
  - memory/architecture-history.md
  - memory/project-evolution.md
  - memory/tech-stack.md
  - skill 文件（遇到特定问题）
  - 非目标模块源码
```

---

## 3. 按任务类型加载路径

```yaml
新功能开发:
  L0 → feature-development.md → architect-agent.md →
  L2: constraints/ai-boundaries.md（已在L0）,
      business-domain.md, module-responsibility.md, data-flow.md,
      backend-coding.md|frontend-coding.md, architecture-governance.md,
      database-governance.md（如涉及DB）, constraints/database.md（如涉及DB）,
      redis-governance.md（如涉及缓存）, mq-governance.md（如涉及MQ）,
      api-governance.md（如涉及API）, constraints/payment.md（如涉及支付）,
      design-pattern-governance.md, domain-modeling.md, ddd-governance.md,
      module-boundary.md
  L3: incidents.md, known-issues.md,
      skills/requirement-breakdown/SKILL.md, skills/api-design/SKILL.md,
      skills/ddd-design/SKILL.md

Bug 修复:
  L0 → bug-fix.md →
  L2: 目标模块源码（重点）, api-conventions.md, error-recovery.md,
      module-boundary.md, backend-coding.md|frontend-coding.md
  L3: incidents.md, known-issues.md, skills/root-cause-analysis/SKILL.md

重构:
  L0 → refactor.md → architect-agent.md →
  L2: code-smell-governance.md, design-pattern-governance.md,
      architecture-governance.md, domain-modeling.md, module-boundary.md
  L3: architecture-history.md, skills/refactor-analysis/SKILL.md

代码审查:
  L0 → code-review.md → reviewer-agent.md →
  L2: 审查的代码文件, 所有相关 rule 文件
  L3: security.md（如涉及敏感代码）, skills/code-verify/SKILL.md

架构设计:
  L0 → architecture-design.md → architect-agent.md →
  L2: 所有 doc 文件, 所有 rule 文件, 所有 constraint 文件
  L3: 所有 memory 文件, skills/ddd-design/SKILL.md,
      skills/domain-modeling/SKILL.md

安全审查:
  L0 → code-review.md → security-agent.md →
  L2: security.md, architecture-governance.md,
      constraints/payment.md（如涉及支付）
  L3: incidents.md, skills/code-verify/SKILL.md

部署:
  L0 → devops-agent.md →
  L2: constraints/deployment.md, constraints/production.md, constraints/infra.md,
      cicd-constraints.md, tools-permissions.md, infrastructure.md
  L3: incidents.md

数据库变更:
  L0 → backend-agent.md →
  L2: constraints/database.md, database-governance.md, module-boundary.md
  L3: skills/sql-review/SKILL.md

缓存/Redis:
  L0 → backend-agent.md →
  L2: redis-governance.md, data-flow.md
  L3: skills/redis-design/SKILL.md, skills/cache-design/SKILL.md

消息队列:
  L0 → backend-agent.md →
  L2: mq-governance.md, data-flow.md
  L3: skills/transaction-analysis/SKILL.md
```

---

## 4. Token 预算控制

```yaml
预算分配（80K）:
  L0: 12K — CLAUDE.md + context-router + constraints/ai-boundaries + memory/decisions + memory/anti-patterns
  L1: 10K — workflow + executor + agent
  L2: 33K — rules + constraints + docs + 源码
  L3: 15K — memory(其余) + skills
  余量: 10K — 动态上下文

超出预算时:
  1. 优先卸载 L3 内容
  2. 使用摘要而非完整文件（memory 文件只读标题+状态）
  3. 大文件只读相关段落（源码只读目标方法）
  4. 使用 Grep 搜索而非完整阅读
```

---

## 5. 上下文刷新

```yaml
刷新时机:
  - 任务切换时（新功能 → Bug 修复）
  - 模块切换时（后端 → 前端）
  - 执行阶段变化时（编码 → 审查 → 部署）

刷新方式:
  - 清理非当前阶段需要的 L2/L3 上下文
  - 加载新阶段需要的上下文
  - 保留 L0/L1（始终需要）
  - constraints 按任务类型按需加载/卸载（ai-boundaries 始终保留）
```
