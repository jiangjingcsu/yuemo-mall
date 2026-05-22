# 架构设计工作流

> AI Agent 执行架构设计时的标准工作流。确保设计决策有理有据、与现有架构一致、可落实为代码。

---

## 触发条件

- 需要设计新模块
- 需要设计跨模块功能
- 需要做技术选型
- 用户要求输出架构方案

---

## 工作流步骤

### Step 1: 现状了解

```yaml
必须阅读:
  1. .harness/memory/decisions.md — 已有架构决策，避免冲突
  2. .harness/memory/anti-patterns.md — 已知反模式，避免重蹈覆辙
  3. .harness/docs/project-overview.md — 当前架构全景
  4. .harness/docs/module-responsibility.md — 模块职责
  5. .harness/docs/business-domain.md — 业务领域
  6. .harness/docs/data-flow.md — 核心数据流
  7. 相关模块源码（Controller/Service/Mapper）

不跳过: 即使已经了解项目，也要确认当前代码状态
```

### Step 2: 需求澄清

```yaml
设计前必须明确:
  - 要解决什么问题？
  - 影响哪些业务域？
  - 涉及哪些模块？
  - 是否需要新增模块？
  - 性能要求（QPS、延迟）？
  - 一致性要求（强一致/最终一致）？
  - 是否需要为微服务拆分做准备？

输出: 用 1-2 句话描述问题，确认理解正确
```

### Step 3: 方案设计

```yaml
方案要素:
  1. 模块归属: 新代码放在哪个模块？是否需要新建模块？
  2. 数据模型: 需要哪些新表/新字段？表结构概要？
  3. API 设计: URL、请求/响应格式、错误码
  4. 数据流: 请求如何从 Controller 流转到 DB
  5. 模块交互: 是否需要 MQ？需要调哪些 Service？
  6. 缓存策略: 是否需要 Redis？Key 设计？
  7. 一致性策略: 事务 vs MQ 最终一致性

约束:
  - 必须符合现有架构分层
  - 必须符合模块边界规则
  - 必须符合 DDD 领域边界
```

### Step 4: 方案评估

```yaml
评估维度:
  - 是否符合现有架构模式？（不引入新范式）
  - 是否引入了循环依赖？
  - 是否破坏了模块边界？
  - 是否影响现有功能？
  - 是否能用现有基础设施实现？（MySQL/Redis/RocketMQ）
  - 是否过度设计？（YAGNI）
  - 是否为微服务拆分做准备？（单向依赖、接口清晰）

自问:
  - 能不能更简单？（KISS）
  - 真的需要这个抽象吗？（YAGNI）
  - 3 个月后这个设计还合理吗？
```

### Step 5: 输出架构文档

```markdown
## 架构设计：{标题}

### 背景
1-2 句话描述为什么需要这个设计。

### 方案概述
1 段话概述整体方案。

### 模块影响
| 模块 | 变更类型 | 具体变更 |
|---|---|---|
| yuemo-order | 修改 | 新增 X 方法 |
| yuemo-cart | 无影响 | — |

### 数据模型
```sql
-- 新表/新字段
```

### API 设计
```
POST /api/{module}/{action}
Request: { ... }
Response: Result<XxxVO>
```

### 数据流
```
请求 → Controller → Service → Mapper → DB
                    → MQ → Consumer → Service → DB
```

### 风险评估
- 风险 1 + 缓解措施
```

### Step 6: 用户确认

```yaml
需要确认的点:
  - 方案是否满足需求？
  - 模块归属是否合理？
  - API 设计是否符合预期？
  - 是否有遗漏的约束或需求？

不跳过确认: 架构方案影响大，必须在编码前对齐
```

### Step 7: Evaluation 评估（强制）

```yaml
必须按 evaluation/review-checklist.md 执行综合评估管线:
  重点维度:
  - architecture-score.md — 设计合规自评（≥ 85）
  - risk-analysis.md — 设计风险分析
  - hallucination-check.md — 设计中无幻觉引用

设计评审:
  - 高风险设计（risk > 60）→ 需架构评审
  - 新中间件引入 → 需架构评审
  - 跨 3+ 模块变更 → 需架构评审

评估不通过:
  - 返回需求澄清 → 重新设计
  - 方案过于复杂 → 简化方案
  - 违反约束 → 调整方案使其合规
  - 3 次仍 FAIL → 暂停，向用户报告
```

### Step 8: Memory 写入

```yaml
架构设计完成后必须写入 memory/:
  - 架构/技术决策 → 追加到 memory/decisions.md（含决策理由、替代方案、风险）
  - 架构阶段变更 → 追加到 memory/architecture-history.md
  - 如发现反模式 → 追加到 memory/anti-patterns.md

写入规则:
  - 只追加，不修改已有条目
  - 包含时间戳和上下文
  - 按 state-manager.md 规范执行
```

## 约束验证

> 约束检查由 workflow-executor（`.harness/executors/workflow-executor.md`）统一执行。
> 架构设计阶段需逐条对照 constraints/ 和 rules/ 验证方案合规性。

## 评估

> 评估管线见 Step 7。本工作流不再重复定义。

## 回滚

```yaml
设计不通过:
  - 返回需求澄清 → 重新设计
  - 方案过于复杂 → 简化方案
  - 违反约束 → 调整方案使其合规

设计通过后编码失败:
  - 检查设计是否有缺陷
  - 更新设计方案
  - 记录设计缺陷到 memory/known-issues.md
```

---

## 设计原则速查

| 原则 | 含义 | 检查 |
|---|---|---|
| KISS | 保持简单 | 能不能再简单一点？ |
| YAGNI | 不设计未来的功能 | 当前需求真的需要这个吗？ |
| DRY | 不重复 | 这段逻辑是否已经在其他地方存在？ |
| SOLID | 单一职责/开闭/里氏替换/接口隔离/依赖倒置 | 每个类职责单一吗？ |
| 最少知识 | 模块间知道得越少越好 | 调用方需要知道这些细节吗？ |

---

## 常见设计模板

### 查询类功能

```
Controller: 接收查询参数 → Service.list() → VO 列表
Service: 构建 LambdaQueryWrapper → Mapper.selectPage() → 转 VO
缓存: Redis 缓存（TTL 5min），Cache-Aside 模式
```

### 写操作功能

```
Controller: 接收 DTO → 校验 → Service.create()
Service: 校验业务规则 → Mapper.insert() → 返回 VO
如有跨模块影响: 发送 MQ 消息 → 异步处理
```

### 跨模块操作

```
模块 A: 发送 MQ 消息（事务消息保证本地DB+MQ原子性）
模块 B: 消费 MQ 消息（幂等）→ 执行自己的逻辑
一致性: 最终一致（非强一致）
```

### 库存类操作

```
预占: 下单 → RMQ事务消息 → COMMIT → 消费者实扣
释放: 取消 → RMQ消息 → 消费者恢复
幂等: WHERE stock >= quantity（行级锁）
```
