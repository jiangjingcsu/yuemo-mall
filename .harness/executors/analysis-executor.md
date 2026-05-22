# 分析执行器

> 调度 architect-agent 执行架构分析、影响评估、技术选型评估。
> 管线架构和整体流程的权威定义见 `.harness/runtime/execution-engine.md`。

---

## 输入

```yaml
输入:
  - 分析目标（模块/流程/技术选型）
  - 分析范围（单模块/跨模块/全局）
  - 触发原因（新需求/重构/问题排查）
```

## 前置条件

```yaml
必须全部满足:
  - [ ] 分析目标在项目范围内
  - [ ] 分析范围已明确界定
  - [ ] 相关模块源码可访问
```

## 前置检查

```yaml
检查:
  1. 分析目标是否在项目范围内？
  2. 是否需要跨模块协作分析？→ 加载 rules/module-boundary.md
  3. 是否涉及数据存储？→ 加载 rules/database-governance.md / rules/redis-governance.md + constraints/database.md
  4. 是否涉及消息队列？→ 加载 rules/mq-governance.md
  5. 是否涉及支付？→ 加载 constraints/payment.md
  6. 是否涉及安全敏感模块？→ 加载 rules/security.md
  7. 是否违反 constraints/production.md 红线？
```

## 上下文加载决策

> 通用约束验证（constraints/ 红线、用户确认等）由 workflow-executor 统一执行。
> 本 executor 仅负责根据分析范围决定额外加载哪些治理规则和上下文。

```yaml
加载决策:
  1. 涉及架构变更评估？→ 加载 rules/architecture-governance.md + rules/extensibility-governance.md
  2. 涉及跨模块依赖？→ 加载 rules/module-boundary.md + docs/module-responsibility.md
  3. 涉及数据库变更？→ 加载 rules/database-governance.md + constraints/database.md
  4. 涉及支付？→ 加载 constraints/payment.md
  5. 涉及缓存/MQ？→ 加载 rules/redis-governance.md / rules/mq-governance.md
  6. 涉及技术选型？→ 加载 memory/tech-stack.md + memory/decisions.md
  7. 涉及安全？→ 加载 rules/security.md + evaluation/security-check.md
```

## 执行步骤

```yaml
1. 上下文收集:
   - 加载目标模块源码
   - 加载模块依赖关系（docs/module-responsibility.md）
   - 加载已有架构决策（memory/decisions.md）
   - 加载已知反模式（memory/anti-patterns.md）
   - 加载架构演进历史（memory/architecture-history.md）

2. 架构分析:
   a. 分层合规性检查:
      - Controller 是否只做协议转换？
      - Service 是否做业务编排（非数据访问）？
      - Entity 是否包含领域行为（非贫血）？
      - Mapper 是否只做数据访问？

   b. 模块边界检查:
      - 是否存在禁止的跨模块依赖？
      - 是否绕过了 API 层直接调用内部实现？
      - 循环依赖检测

   c. 设计模式合规:
      - 是否存在 switch-case 行为分发（≥3 分支）？→ 应使用策略模式
      - 是否存在 if-else 链（≥3 条件）？→ 应使用策略模式
      - 是否存在 God Service（>500 行）？→ 应拆分
      - 高扩展模块是否使用了策略模式？→ rules/extensibility-governance.md

   d. 数据流分析:
      - 事务边界是否正确？
      - 缓存与数据库一致性策略？
      - MQ 消息幂等性？

3. 影响评估:
   - 修改影响范围（哪些模块/API/表会被影响）
   - 性能影响（慢查询/缓存穿透/热点 key）
   - 兼容性影响（API 变更/数据库迁移/前端适配）
   - 风险等级（按 evaluation/risk-analysis.md 分级）

4. 技术选型评估（如涉及）:
   - 是否符合现有技术栈（memory/tech-stack.md）？
   - 是否引入新的基础设施依赖？
   - 与现有架构的集成成本？
   - 备选方案对比（至少 2 个）
   - 是否与已否决方案冲突（memory/decisions.md）？
```

## 评估

```yaml
分析质量:
  - [ ] 覆盖所有相关模块
  - [ ] 覆盖所有约束层（DB/Redis/MQ/API/Security）
  - [ ] 问题有明确的严重级别（CRITICAL/HIGH/MEDIUM/LOW）
  - [ ] 建议有具体的实施路径
  - [ ] 引用具体文件位置而非泛泛描述
  - [ ] 影响评估包含风险等级
  - [ ] 技术选型引用 memory/decisions.md 避免重复已否决方案

评估管线:
  - architecture-score.md — 架构合规评分
  - hallucination-check.md — 幻觉检测（引用的类/配置/路径真实存在）
  - risk-analysis.md — 风险评估
```

## 输出

```yaml
分析报告:
  - 架构合规性评分（PASS/WARN/FAIL）
  - 发现的问题列表（含严重级别和具体文件位置）
  - 影响范围评估（含风险等级）
  - 改进建议（含优先级和实施路径）
  - 技术选型建议（如涉及，含备选方案对比）

Memory 写入:
  - 新发现反模式 → 追加到 memory/anti-patterns.md
  - 架构分析结论 → 追加到 memory/decisions.md（含决策理由）
  - 架构阶段变更 → 追加到 memory/architecture-history.md
```

## 回滚

```yaml
分析不需要回滚（只读操作）
分析结论错误 → 重新分析，更新报告
分析导致错误决策 → 记录到 memory/incidents.md，更新 memory/decisions.md
```

## Agent

```yaml
调度: architect-agent
协作:
  - reviewer-agent（分析报告审查）
  - security-agent（安全敏感模块分析）
前置: 无（分析为只读操作，无前置依赖）
后置: refactor-executor（如分析结果触发重构）
```
