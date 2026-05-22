# 工作流执行器

> 所有工作流的标准执行框架。定义每一步的具体操作协议。
> 管线架构和整体流程的权威定义见 `.harness/runtime/execution-engine.md`。

---

## 标准步骤

> 以下步骤与 execution-engine.md 的 6 阶段管线严格对齐（任务输入→前置检查→上下文加载→执行→评估→输出）。

### Step 1: 任务输入（意图解析 + 输入验证）

```yaml
路由:
  - 按 workflow-router.md 确定激活的 workflow 文件
  - 按 agent-router.md 确定执行 Agent

验证:
  - 任务目标是否明确？
  - 输入参数是否完整？
  - 是否有歧义需要澄清？

不通过: 请求用户澄清需求
```

### Step 2: 前置检查（含约束验证）

```yaml
检查:
  - 是否违反 constraints/ 中的红线？
    - constraints/production.md（始终）
    - constraints/database.md（如涉及数据库）
    - constraints/payment.md（如涉及支付）
    - constraints/infra.md（如涉及基础设施）
    - constraints/deployment.md（如涉及部署）
    - constraints/ai-boundaries.md（始终）
  - 是否需要用户确认（deployment/infra 等）？
  - 是否影响其他模块？

不通过: 停止执行，报告具体违规内容
```

### Step 3: 上下文加载

```yaml
按 context-router.md 加载:
  L0 — 始终加载:
    - CLAUDE.md
    - memory/decisions.md（架构决策，避免重蹈覆辙）
    - memory/anti-patterns.md（已知反模式，避免重复踩坑）
  L1 — 高优先级:
    - 对应 workflow 文件
    - 对应 executor 文件
    - 对应 agent 定义文件
  L2 — 按需加载:
    - 相关 rule 文件
    - 相关 doc 文件
    - 目标模块源码
  L3 — 延迟加载:
    - memory 其余文件（incidents/known-issues/architecture-history/tech-stack/project-evolution）
    - skill 文件

Token 预算检查:
  - 总预算: 80K
  - 超出时: 优先卸载 L3 → 使用摘要 → 大文件只读相关段落
  - 详见 context-router.md §4
```

### Step 4: 执行

```yaml
调度:
  - 按 agent-router.md 调度对应 Agent
  - 同一时间只有一个 Agent 修改同一文件
  - 并行 Agent 操作不同模块/文件
  - 关键阶段（编译/测试/审查）必须串行

执行:
  - 按激活的 workflow 执行具体步骤
  - 每步验证（编译/测试/业务检查）
  - 失败重试（最多 3 次）

上下文传递:
  - 上游 Agent 输出 → 下游 Agent 输入
  - 通过文件系统传递结果
```

### Step 5: 评估

```yaml
评估入口: evaluation/review-checklist.md

评估级别判定:
  完整评估（6 维度全执行）:
    - 跨模块变更
    - 支付/订单/用户模块变更
    - 新增 API 接口
    - 数据库迁移
    - 架构变更
  简化评估（安全 + 输出验证 + 风险分析）:
    - 单模块内 ≤ 3 文件修改
    - 不涉及支付/订单/用户模块
    - 不涉及数据库迁移
    - 不涉及 API 变更
  最小评估（仅输出验证）:
    - 纯前端样式/文案修改
    - 注释修改
    - 配置文件修改（非生产）

6 个评估维度（按顺序）:
  1. output-validator.md — 编译/构建/测试验证（基础门禁，不通过则终止）
  2. hallucination-check.md — 幻觉检测（基础门禁，不通过则终止）
  3. security-check.md — 安全评估
  4. architecture-score.md — 架构合规
  5. code-quality.md — 代码质量
  6. risk-analysis.md — 风险分析

结果: PASS → 继续 / WARN → 建议修复 / FAIL → BLOCK
3 轮仍 FAIL → 暂停，记录到 memory/known-issues.md，向用户报告
```

### Step 6: 输出

```yaml
输出:
  - 执行结果
  - 修改文件列表
  - 评估结果（按 review-checklist.md 输出格式）
  - 风险提示（如有）
  - Memory 写入（新反模式/已知问题/架构决策）
```

---

## 失败处理

```yaml
编译/构建失败:
  1. 捕获错误
  2. 分析原因
  3. 自动修复（backend-agent 或 frontend-agent）
  4. 重新编译
  3 次仍失败 → 暂停，向用户报告

测试失败:
  1. 分析失败原因
  2. 代码问题 → 修复代码 / 测试问题 → 修复测试（需用户确认）
  3 次仍失败 → 暂停

评估失败（FAIL）:
  1. 分析 FAIL 维度
  2. 修复对应问题
  3. 重新评估
  3 轮仍 FAIL → 暂停

约束违规:
  立即停止执行 → 报告具体违规内容 → 等待用户决策
```

---

## 回滚机制

> 回滚的触发条件、回滚方式、不可回滚场景的权威定义见 `execution-engine.md` §回滚机制。
> 本文件不再重复。
