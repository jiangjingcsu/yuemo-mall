# 架构演进记录

> 记录项目架构的关键演进节点。避免 AI 提出已被否决的架构方案。
> 交叉引用: memory/decisions.md（技术决策详情）、memory/incidents.md（历史事故）、memory/project-evolution.md（未来演进路线）

---

## 演进时间线

### Phase 1: 基础单体（项目初期）

```yaml
状态: Spring Boot + Maven 多模块单体
模块: user, product, order, payment, cart, promotion
通信: 模块间直接 Service 调用
数据库: 共享 MySQL
缓存: Redis 基础使用
消息: RocketMQ 基础使用
事故: INC001(购物车重复插入)、INC002(Flyway Checksum 不匹配)、INC003(JWT 密钥不足)、INC004(双重 Nginx 代理)
```

### Phase 2: Harness 工程化（2026-05）

```yaml
状态: 引入 .harness/ AI Agent 治理层
新增:
  docs/: 业务领域、模块职责、数据流文档
  rules/: 18 个治理规则
    编码规范: backend-coding, frontend-coding
    架构治理: architecture-governance, module-boundary, ddd-governance, domain-modeling
    数据治理: database-governance, redis-governance, mq-governance
    API 治理: api-governance
    安全: security
    行为: agent-behavior, tools-permissions
    CI/CD: cicd-constraints
  skills/: 13 个专业技能
    api-design, cache-design, code-verify, ddd-design, domain-modeling,
    microservice-readiness, performance-review, redis-design, refactor-analysis,
    requirement-breakdown, root-cause-analysis, sql-review, transaction-analysis
  workflows/: 5 个标准工作流
    feature-development, bug-fix, code-review, architecture-design, refactor
  runtime/: 上下文路由、工作流路由、能力注册
```

### Phase 3: 架构决策治理（2026-05）

```yaml
状态: 引入架构决策和设计模式治理
新增:
  rules/architecture-decision.md: switch-case 治理
  rules/design-pattern-governance.md: 策略/工厂/状态/责任链强制使用
  rules/code-smell-governance.md: 代码坏味道识别
  rules/extensibility-governance.md: 高扩展模块治理
  rules/domain-modeling.md: 充血模型要求
```

### Phase 4: 企业级升级（当前 — 2026-05）

```yaml
状态: 升级为企业级 Harness Engineering Platform
新增:
  constraints/: 6 个红线约束（ai-boundaries, database, deployment, infra, payment, production）
  memory/: 7 个长期记忆文件（architecture-history, decisions, incidents, known-issues, anti-patterns, project-evolution, tech-stack）
  agents/: 7 个 Agent 角色（architect, backend, frontend, reviewer, security, devops, tester）
  executors/: 6 个执行器（analysis, code, deploy, refactor, review, workflow）
  evaluation/: 7 个评估文件（code-quality, architecture-score, hallucination-check, security-check, risk-analysis, output-validator, review-checklist）
  observability/: 1 个可观测性规范（observability-policy）
```

---

## 被否决的方案

```yaml
方案: 立即拆分为微服务
否决原因: 团队规模小，运维成本高，业务尚未稳定
状态: 暂时否决，Phase 6 时重新评估（参见 project-evolution.md）
决策编号: D001

方案: 使用 Spring Data JPA 替换 MyBatis-Plus
否决原因: 团队不熟悉，SQL 不够可控，迁移成本高
状态: 永久否决
决策编号: D002

方案: 物理删除替换逻辑删除
否决原因: 数据恢复需求、审计需求
状态: 永久否决
决策编号: D005

方案: 统一使用 Spring Cache 抽象（放弃直接 RedisTemplate）
否决原因: 购物车 Hash 结构需要直接操作 Redis
状态: 部分采用（Spring Cache 用于简单缓存，RedisTemplate 用于复杂操作）
决策编号: D004

方案: 使用 Sa-Token 替换 JWT + Redis 认证
否决原因: 项目已基于 JWT + Redis 实现完整认证体系（GatewayAuthFilter + Token 黑名单），Sa-Token 引入额外依赖且功能重叠
状态: 永久否决
决策编号: D008

方案: 使用 Elasticsearch 替换 MySQL LIKE 搜索
否决原因: 搜索需求简单（商品名/关键词），ES 运维成本高，MySQL LIKE + 热词表已满足需求
状态: 暂时否决，搜索性能瓶颈时重新评估
决策编号: D009
```
