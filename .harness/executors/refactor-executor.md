# 重构执行器

> 调度 backend-agent / frontend-agent 安全执行重构任务。
> 重构工作流步骤详见 `workflows/refactor.md`（8 步 SOP），本 executor 聚焦调度逻辑、上下文加载、失败处理、回滚策略。
> 通用约束验证由 `workflow-executor` 统一执行。
> 管线架构和整体流程的权威定义见 `.harness/runtime/execution-engine.md`。

---

## 输入

```yaml
输入:
  - 重构目标（文件/模块/架构层面）
  - 重构类型:
      后端: 提取方法 / 重命名 / 移动代码 / 拆分 God Service / 策略模式替换 / DTO→record
      前端: 提取组件 / 重命名 / 移动代码 / Store 拆分 / API 层重构
  - 重构原因（消除反模式/性能优化/架构升级）
  - 影响分析报告（来自 analysis-executor，推荐）
```

## 前置条件

```yaml
必须全部满足:
  - [ ] 重构影响分析已完成（按 refactor.md Step 2 维度）
  - [ ] 现有测试全部通过 或 已补测试（按 refactor.md Step 4）
  - [ ] 目标代码有测试覆盖（≥50%）或已补测试
  - [ ] 已确认回滚方案（git 可还原）
```

## 前置检查

```yaml
检查:
  1. 是否违反 constraints/ 中的红线？→ 违反则停止
  2. 是否涉及数据库变更？→ 必须先创建 Flyway 脚本
  3. 是否涉及 API 变更？→ 评估前端兼容性
  4. 是否涉及支付/认证模块？→ 激活 security-agent，额外安全审查
  5. 是否跨多个模块？→ 分步重构，每步独立验证
  6. 是否涉及跨模块调用？→ 加载 rules/module-boundary.md 验证
  7. 是否需要新增依赖？→ 需用户确认（constraints/ai-boundaries.md）
```

## 上下文加载决策

> 通用约束验证（constraints/ 红线、用户确认等）由 workflow-executor 统一执行。
> 本 executor 仅负责根据重构范围决定额外加载哪些治理规则和上下文。

```yaml
始终加载:
  - memory/decisions.md — 历史架构决策（避免与已否决方案矛盾）
  - memory/anti-patterns.md — 已知反模式（避免重复踩坑）
  - memory/architecture-history.md — 架构演进记录

按需加载:
  1. 涉及策略模式替换？→ 加载 rules/design-pattern-governance.md + rules/extensibility-governance.md
  2. 涉及跨模块移动？→ 加载 rules/module-boundary.md + docs/module-responsibility.md
  3. 涉及数据库变更？→ 加载 rules/database-governance.md + constraints/database.md
  4. 涉及支付？→ 加载 constraints/payment.md
  5. 涉及缓存/MQ？→ 加载 rules/redis-governance.md / rules/mq-governance.md
  6. 涉及 Entity 修改？→ 加载 rules/domain-modeling.md（充血模型规范）
  7. 涉及前端重构？→ 加载 rules/frontend-coding.md（前端编码规范）
  8. 涉及 API 变更？→ 加载 rules/api-governance.md（API 规范）
```

## 执行步骤

> 详细步骤按 `workflows/refactor.md` 执行。本节定义调度逻辑和每步的调度策略。

```yaml
1. 备份当前状态:
   - 记录当前 git commit hash
   - 确认工作区干净（git stash 如有必要）

2. Memory 检查（对应 refactor.md Step 1）:
   - 读取 memory/decisions.md — 是否有相关架构决策
   - 读取 memory/anti-patterns.md — 是否有已知反模式
   - 读取 memory/architecture-history.md — 是否有架构演进记录

3. 影响分析（对应 refactor.md Step 2）:
   - 如已有 analysis-executor 报告 → 直接使用
   - 如无 → 调度 architect-agent 执行影响分析

4. 分步重构（对应 refactor.md Step 4）:
   后端重构（调度 backend-agent）:
     - 每步一个独立变更
     - 每步完成后: mvn compile -q + mvn test -pl <module> -q
     - 每步独立 git commit（可单独 revert）

   前端重构（调度 frontend-agent）:
     - 每步一个独立变更
     - 每步完成后: npx tsc --noEmit + npm run build
     - 每步独立 git commit（可单独 revert）

5. 自检验证（对应 refactor.md Step 6）:
   - 后端: mvn compile -q + mvn test -q
   - 前端: npx tsc --noEmit + npm run build
   - 业务验证: 核心 API 正常响应

6. 评估管线（对应 refactor.md Step 7）:
   - 按 evaluation/review-checklist.md 执行综合评估
   - 重构前后基线对比（代码行数、圈复杂度）

7. Memory 写入（对应 refactor.md Step 8）:
   - 架构变更 → memory/architecture-history.md
   - 消除反模式 → 更新 memory/anti-patterns.md 状态为 [FIXED]
   - 技术债务 → memory/known-issues.md
```

## 约束

```yaml
必须:
  - 每次只改一个关注点
  - 每步之后编译+测试
  - 每步独立 git commit（可单独 revert）
  - 保持外部行为不变（重构≠重写）
  - 不修改测试（除非测试依赖了实现细节）
  - 重构前记录基线（代码行数、圈复杂度）

禁止:
  - 同步修改业务逻辑和架构（分两次重构）
  - 跳过测试验证步骤
  - 删除原有功能
  - 修改 public API 签名（除非是重构目标本身）
  - 在生产环境重构
  - 重构 + 新功能混合在一次提交
  - 大规模重写（rewrite）
  - 删除"看似无用"但实际在用的代码
  - 改变数据库表结构（需用 Flyway 迁移，且不可回滚）
  - 引入新的架构模式（除非明确目标）
```

## 评估

```yaml
按 evaluation/review-checklist.md 执行评估:
  完整评估: 跨模块重构 / 支付订单用户模块重构 / 新增 API / 数据库迁移
  简化评估: 单模块内 ≤ 3 文件修改
  最小评估: 纯重命名/移动代码

评估管线:
  - architecture-score.md — 架构评分应提升（≥ 85）
  - code-quality.md — 代码质量应提升（≥ 85）
  - risk-analysis.md — 重构风险可控
  - output-validator.md — 编译+测试通过
  - hallucination-check.md — 重构引用真实存在

重构前后基线对比:
  - 代码行数: 减少或不变（拆分除外）
  - 圈复杂度: 降低
  - 反模式: 目标反模式已消除，无新增

评估不通过:
  - 修复后重新评估
  - 3 次仍 FAIL → 回滚重构，向用户报告
```

## 输出

```yaml
重构执行结果:
  - 修改文件列表（含变更类型: 新增/修改/删除）
  - 评估结果（PASS/WARN/FAIL）
  - 重构前后基线对比（代码行数、圈复杂度）
  - Flyway 迁移脚本（如有，标注不可回滚风险）
  - 风险提示（如有）
  - Memory 写入记录（反模式/架构变更/技术债务）
```

## 回滚

```yaml
自动回滚（任一步骤失败）:
  - git checkout <修改的文件>（代码级回滚）
  - 每步独立 commit，可单独 revert

手动回滚（3次失败后）:
  1. git reset --hard <备份的 commit hash>（全局回滚）
  2. 分析失败原因
  3. 缩小重构范围
  4. 重新开始

涉及 Flyway 迁移:
  - Flyway 不支持自动回滚，需提前准备逆向迁移脚本 V{N+1}__rollback_{desc}.sql
  - 回滚时需用户确认

约束违反:
  → 立即停止并报告，不自动回滚（需用户确认）
```

## Agent

```yaml
调度: backend-agent（后端）/ frontend-agent（前端）
前置: architect-agent（影响分析）
协作: security-agent（涉及支付/认证模块时）
后置: reviewer-agent（代码审查）
```
