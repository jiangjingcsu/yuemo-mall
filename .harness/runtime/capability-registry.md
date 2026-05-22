# 能力注册表

> 轻量索引。每种能力的权威定义（输入/输出/约束/依赖）在对应的 Agent + Rule 文件中。
> 能力组合顺序见 `agent-router.md` §能力组合。能力边界见 `constraints/ai-boundaries.md`。

---

## 能力 → 权威文件 映射

| 能力 | 执行 Agent | 治理规则 | 约束文件 | 实操技能 |
|---|---|---|---|---|
| 代码生成 | backend-agent / frontend-agent | backend-coding / frontend-coding / architecture-governance / module-boundary / design-pattern-governance / agent-behavior | ai-boundaries | — |
| 代码修改 | backend-agent / frontend-agent | architecture-governance / design-pattern-governance / agent-behavior | ai-boundaries | — |
| Bug 定位 | backend-agent / frontend-agent | agent-behavior | ai-boundaries | root-cause-analysis |
| 代码审查 | reviewer-agent | 全部 rules/ / code-smell-governance | — | code-verify / performance-review / transaction-analysis / drift-detection |
| 数据库设计 | backend-agent | database-governance | database | sql-review |
| 缓存设计 | backend-agent | redis-governance | — | cache-design / redis-design |
| API 设计 | backend-agent | api-governance | — | api-design |
| MQ 设计 | backend-agent | mq-governance | — | — |
| 领域建模 | architect-agent | ddd-governance / domain-modeling | — | ddd-design / domain-modeling / requirement-breakdown |
| 架构评估 | architect-agent | module-boundary / ddd-governance / architecture-decision / extensibility-governance | — | microservice-readiness / refactor-analysis / drift-detection |
| CI/CD / 部署 | devops-agent | cicd-constraints | deployment / infra / production | — |
| 安全审查 | security-agent | security | production / payment | — |
| 编译验证 | backend-agent / frontend-agent | — | — | code-verify |
| 测试运行 | tester-agent | — | — | — |
| 性能优化 | architect-agent | architecture-governance / module-boundary | — | performance-review |
| 构建修复 | backend-agent / frontend-agent | — | — | code-verify |
| 代码重构 | backend-agent / frontend-agent | architecture-governance / design-pattern-governance | ai-boundaries | refactor-analysis |

---

## 工具级权限

> 权威定义在 `rules/tools-permissions.md`，本文件不重复。
