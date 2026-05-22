# Bug 修复工作流

> AI Agent 执行 Bug 修复时的标准工作流。确保定位准确、修复安全、验证完整。

---

## 触发条件

- 用户报告 Bug
- CI/CD 流水线失败
- 运行时异常/错误日志
- 用户反馈功能不符合预期

---

## 工作流步骤

### Step 1: 理解问题与分类

```yaml
收集信息:
  - Bug 现象是什么？（报错信息、异常行为、截图）
  - 复现步骤是什么？
  - 影响范围？（哪个模块、哪些用户、是否生产）
  - 是否有相关日志？

Bug 分类（决定后续分析路径）:
  | Bug 类型 | 优先级 | 分析侧重 | 辅助 Skill |
  |---|---|---|---|
  | 编译失败 | 最高 | 依赖版本、语法兼容性 | — |
  | 运行时异常 | 高 | 堆栈追溯、空指针、类型转换 | — |
  | 数据不一致 | 高 | 事务边界、并发竞态、缓存一致性 | sql-review / transaction-analysis |
  | 功能不符合预期 | 中 | 业务逻辑、状态流转、条件分支 | — |
  | 性能问题 | 中 | 慢查询、N+1、缓存缺失 | performance-review |
  | UI 显示问题 | 低 | 浏览器兼容、响应式、数据绑定 | — |

Memory 检查（强制）:
  - 读取 .harness/memory/decisions.md — 是否有相关架构决策
  - 读取 .harness/memory/anti-patterns.md — 是否有已知反模式
  - 读取 .harness/memory/incidents.md — 是否有类似事故记录

对不明确的问题:
  - 先请求更多信息
  - 不自行假设 Bug 原因
```

### Step 2: 根因分析

> **激活 Skill**: `root-cause-analysis`
> 详细分析流程见 `.harness/skills/root-cause-analysis/SKILL.md`

```yaml
核心步骤:
  1. 信息收集与分类 — 确认 Bug 类型，选择分析路径
  2. 堆栈与调用链追溯 — 从异常位置逐层追溯（Controller → Service → Mapper）
  3. 多维度排查 — 按分类选择排查维度（代码/数据/并发/配置/依赖）
  4. 根因确认 — 根因必须能解释所有现象

按 Bug 类型激活辅助 Skill:
  - 数据库相关 → sql-review（审查 SQL 语句、索引、迁移脚本）
  - 并发/事务相关 → transaction-analysis（分析事务边界、并发安全）
  - 性能相关 → performance-review（分析慢查询、N+1、缓存缺失）

输出: 根因分析报告（含根因、分析路径、影响范围）
```

### Step 3: 制定修复方案

```yaml
方案评估:
  - 最小修改原则：只改必要的代码
  - 不修复本 Bug 范围外的代码（即使发现问题，单独提）
  - 评估修复是否影响其他功能
  - 考虑是否需要在其他类似位置做同样修复

输出: 简要描述修复方案（1-2 句），具体修改点
```

### Step 4: 实施修复

```yaml
实施原则:
  - 使用 Edit 工具精确修改，不重写整个文件
  - 修复根因而非症状
  - 不改动无关代码
  - 不加不必要的注释

安全关注:
  - 修复不引入新的安全漏洞
  - 修复不破坏现有功能
  - 数据库查询条件正确（尤其是 DELETE/UPDATE）
```

### Step 5: 验证

> **激活 Skill**: `code-verify`
> 详细验证流程见 `.harness/skills/code-verify/SKILL.md`（8 层递进验证）

```yaml
验证层级（递进，前一层失败不执行后续层）:
  1. 编译验证 — mvn compile / npx tsc --noEmit
  2. 构建验证 — mvn package / npm run build
  3. 测试验证 — mvn test -pl <模块> / npm test
  4. 架构治理检查 — 模块边界、分层合规
  5. 规范合规检查 — 编码规范逐项检查
  6. 数据库与缓存检查 — SQL/Redis/MQ 合规
  7. 安全合规检查 — 无硬编码密钥、无注入风险
  8. API 契约验证（按需） — 前后端接口一致性

回归验证（Bug 修复特有）:
  - 复现原 Bug，确认不再触发
  - 修复是否影响正常流程
  - 边界情况是否处理正确

失败处理: 最多 3 次修复尝试，3 次仍失败 → 向用户报告
```

### Step 6: Evaluation 评估（强制）

```yaml
必须按 evaluation/review-checklist.md 执行综合评估管线:
  重点维度:
  - security-check.md — 修复未引入新安全漏洞
  - hallucination-check.md — 修复使用的 API/路径真实存在
  - output-validator.md — 编译通过、测试通过
  - risk-analysis.md — 修复风险可控

评估不通过:
  - 重新分析根因
  - 修正修复方案
  - 3 次仍 FAIL → 向用户报告
```

### Step 7: 记录与归档

```yaml
向用户报告:
  - Bug 根因（一句话）
  - 修复方案（一句话）
  - 修改的文件列表
  - 验证结果

Memory 写入（强制）:
  - 如为线上事故 → 追加到 memory/incidents.md（含根因、修复方案、教训）
  - 如发现已知问题 → 追加到 memory/known-issues.md
  - 如发现反模式 → 追加到 memory/anti-patterns.md

写入规则:
  - 只追加，不修改已有条目
  - 包含时间戳和上下文
  - 按 state-manager.md 规范执行
```

## 约束验证

> 约束检查由 workflow-executor（`.harness/executors/workflow-executor.md`）统一执行。
> Bug 修复场景需特别关注：变更范围是否超出修复目标（最小修改原则）、是否引入新安全漏洞。
> 约束文件清单见 `execution-engine.md` §前置检查。

## 评估

> 评估管线见 Step 6。本工作流不再重复定义。

## 回滚

```yaml
修复失败:
  - 不影响线上: git checkout <file>
  - 已部署: 按 deployment.md 回滚流程
  - 数据库修复: Flyway 创建逆向脚本（非修改已有脚本）

修复引入新 Bug:
  - 立即回滚修复代码
  - 重新分析根因
  - 记录到 memory/incidents.md
```

---

## 数据库相关 Bug 特别注意

```yaml
数据库 Bug 处理:
  - 先检查数据状态（SELECT 确认）
  - 不直接在生产执行 DELETE/UPDATE
  - 需要 DDL 变更时创建 Flyway 迁移
  - 已执行的迁移文件禁止修改
  - 修复后验证数据一致性
```
