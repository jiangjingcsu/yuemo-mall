# 重构工作流

> AI Agent 执行代码重构时的标准工作流。确保重构安全、可回滚、不破坏功能。

---

## 触发条件

- 用户要求重构某个模块/功能
- 代码质量改进（消除重复、改善结构）
- 为微服务拆分做准备
- 技术债务清理

---

## 工作流步骤

### Step 1: 明确重构目标

```yaml
必须确认:
  - 重构什么？（具体模块/类/方法）
  - 为什么重构？（性能/可维护性/可扩展性/拆分准备）
  - 重构边界？（只改 X，不动 Y）
  - 不改变什么？（对外 API/数据库结构/接口签名）

Memory 检查（强制）:
  - 读取 .harness/memory/decisions.md — 是否有相关架构决策
  - 读取 .harness/memory/anti-patterns.md — 是否有已知反模式
  - 读取 .harness/memory/architecture-history.md — 是否有架构演进记录

原则: 重构不改变可观测行为
```

### Step 2: 影响分析

```yaml
分析维度:
  - 被修改的类被哪些代码引用？（Grep 查找调用方）
  - DTO/Entity 字段变更影响哪些接口？
  - 方法签名变更影响哪些调用方？
  - MQ 消息体变更影响哪些消费者？
  - Mapper 方法变更影响哪些 Service？

输出: 影响范围清单（调用方列表、接口列表）
```

### Step 3: 制定重构方案

```yaml
方案内容:
  - 重构前后架构对比
  - 分几步执行？
  - 每一步做什么？
  - 每步验证方式？

方案原则:
  - 小步快跑，每步可验证
  - 不一次修改超过 10 个文件
  - 优先消除重复、改善命名、提取方法
  - 不引入新的设计模式（除非必要）
  - 不预设计未来才需要的抽象
```

### Step 4: 执行重构

```yaml
执行顺序:
  1. 先写测试（如有缺失），确保现有行为被覆盖
  2. 提取方法/类（Extract Method/Class）
  3. 重命名（Rename）
  4. 移动代码（Move Method/Class）
  5. 删除死代码
  6. 每步验证编译和测试

注意事项:
  - 每步改动要小，一步只做一件事
  - 不顺手改无关代码
  - 重构过程中不修 Bug（单独记录、单独修复）
  - 不改 API 返回值格式（除非明确目标包含此内容）
```

### Step 5: 验证

```yaml
编译: mvn compile -q（后端）/ npx tsc --noEmit（前端）
测试: mvn test -pl <模块> -q
回归: 确保现有测试全部通过
新增测试: 如果重构涉及新抽取的方法

失败处理:
  - 测试失败 → 检查是否改变了行为
  - 编译失败 → 检查引用是否全部更新
  - 最多 3 次修复尝试
```

### Step 6: 自检

```yaml
重构自检:
  - [ ] 对外 API 行为未改变
  - [ ] 模块边界未被破坏
  - [ ] 循环依赖未引入
  - [ ] DTO/VO/Entity 分离未被破坏
  - [ ] 编译通过
  - [ ] 测试通过
  - [ ] 没有注释掉的旧代码
  - [ ] 没有新增未使用的 import
```

### Step 7: Evaluation 评估（强制）

```yaml
必须按 evaluation/review-checklist.md 执行综合评估管线:
  重点维度:
  - architecture-score.md — 架构评分应提升（≥ 85）
  - code-quality.md — 代码质量应提升（≥ 85）
  - risk-analysis.md — 重构风险可控
  - output-validator.md — 编译+测试通过
  - hallucination-check.md — 重构引用真实存在

重构前基线:
  - 记录重构前的代码行数、圈复杂度
  - 重构后必须改善（或持平）

评估不通过:
  - 修复后重新评估
  - 3 次仍 FAIL → 回滚重构，向用户报告
```

### Step 8: Memory 写入

```yaml
重构完成后必须写入 memory/:
  - 如涉及架构变更 → 追加到 memory/architecture-history.md
  - 如消除反模式 → 更新 memory/anti-patterns.md 状态为 [FIXED]
  - 如发现技术债务 → 追加到 memory/known-issues.md

写入规则:
  - 只追加，不修改已有条目（状态标记除外）
  - 包含时间戳和上下文
  - 按 state-manager.md 规范执行
```

## 约束验证

> 约束检查由 workflow-executor（`.harness/executors/workflow-executor.md`）统一执行。
> 重构场景特别关注：分层架构不变、模块边界不破坏、对外行为不变。
> 约束文件清单见 `execution-engine.md` §前置检查。

## 评估

> 评估管线见 Step 7。本工作流不再重复定义。

## 回滚

```yaml
重构风险控制:
  - 每步独立 git commit（可单独 revert）
  - 任一步失败: git checkout <step_files>
  - 全部失败: git reset --hard <重构前 commit>
  - 不强制推进（no git push --force）
```

---

## 重构类型指南

### 提取方法 (Extract Method)

```yaml
时机: 方法 > 50 行 / 重复出现 3+ 次
方式: 提取为 private 方法，命名清晰
验证: 原方法行为不变
```

### 重命名 (Rename)

```yaml
时机: 命名不清晰、不一致
方式: IDE 重构功能，确保所有引用更新
验证: 编译通过
注意: 对外 API 的路径/参数重命名需用户确认
```

### 移动代码 (Move)

```yaml
时机: 代码放在错误的包/模块
方式: 移到正确的包/模块，更新 import
验证: 编译 + 测试
注意: 跨模块移动必须确认模块边界规则
```

### DTO/VO 转 Java record

```yaml
时机: DTO/VO 使用 @Data 但无业务逻辑
方式: 转为 record，更新所有引用
验证: getter/setter 调用是否全部替换
```

---

## 重构禁忌

```yaml
禁止:
  - 重构 + 新功能混合在一次提交
  - 大规模重写（rewrite）
  - 改变对外 API 行为
  - 改变数据库表结构（需用 Flyway 迁移）
  - 引入新的架构模式（除非明确目标）
  - 删除"看似无用"但实际在用的代码
  - 重写测试（除非错误）
```
