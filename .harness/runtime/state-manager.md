# 状态管理器

> 管理 AI Agent 执行过程的持久化状态。非会话级（会话级状态由 Claude Code 自身管理）。
>
> 与 CLAUDE.md Lifecycle 对齐:
>   - Task Intake → 读取 memory 避免重复踩坑（Principle 6: Memory Prevents Drift）
>   - Pre-check → 验证决策约束（decisions.md 中的已通过决策）
>   - Context Loading → 按任务类型加载对应 memory 文件（见第 6 节读取时机矩阵）
>   - Evaluation → 检查是否违反已有决策或产生新反模式

---

## 状态存储

```yaml
存储位置:
  项目长期状态: .harness/memory/ 目录
  会话临时状态: Claude Code 会话上下文（不持久化）

状态类型:
  - 决策记录: memory/decisions.md
  - 架构历史: memory/architecture-history.md
  - 反模式记录: memory/anti-patterns.md
  - 事故记录: memory/incidents.md
  - 已知问题: memory/known-issues.md
  - 演进路线: memory/project-evolution.md
  - 技术栈记录: memory/tech-stack.md
```

## 状态生命周期

```yaml
决策（decision）:
  创建: 做出架构/技术决策时
  更新: 决策变更时（追加而非覆盖）
  废弃: 决策被推翻时（标记为废弃，保留历史）

事故（incident）:
  创建: 发生线上事故时
  更新: 事故处理过程中
  关闭: 事故解决后（记录最终结果和教训）

反模式（anti-pattern）:
  创建: 发现新反模式时
  更新: 部分修复时
  关闭: 完全消除后（标记为 fixed）

已知问题（known-issue）:
  创建: 发现技术债务时
  更新: 优先级变更时
  关闭: 问题解决后（标记为 resolved）

架构历史:
  追加: 每个阶段完成时
  不更新: 历史记录不可修改

技术栈（tech-stack）:
  追加: 技术栈变更时（升级版本、新增/移除依赖）
  更新: 版本号变更时（追加变更记录，保留历史版本）
  禁止: 删除历史版本记录

演进路线（project-evolution）:
  追加: 新阶段规划时
  更新: 阶段里程碑完成时（更新状态标记）
  关闭: 阶段全部完成时（标记为 completed）
```

## 状态更新规则

```yaml
修改 memory 文件规则:
  - 只能追加新条目（不修改已有条目）
  - 新决策/事故/反模式追加到文件末尾
  - 状态变更使用标记（如 [FIXED], [DEPRECATED], [SUPERSEDED]）
  - 保留时间戳和上下文

示例:
  ## D008: 引入 Elasticsearch（2026-06-01）
  **状态**: 已通过
  **决策人**: 架构评审
  ...
  
  ## D001: Monolith多模块架构（2026-01-15）
  **状态**: [SUPERSEDED by D010]  ← 更新标记
  ...
```

## 编号规范

```yaml
编号前缀:
  - 决策记录: D{NNN}（如 D001, D016）
  - 反模式: AP{NNN}（如 AP001, AP009）
  - 事故记录: INC{NNN}（如 INC001, INC004）
  - 已知问题: TD{NNN}（如 TD001, TD024）

递增规则:
  - 编号严格递增，不跳号、不复用
  - 新增条目前必须查看当前最大编号
  - 交叉引用使用完整编号（如 关联: AP005、决策编号: D001）

编号冲突防护:
  - 同一会话内批量新增时，按顺序分配编号
  - 跨会话新增时，先读取文件确认最大编号
```

## 内容格式模板

```yaml
决策记录（decisions.md）:
  ### D{NNN}: {标题}（{日期}）
  **状态**: 已通过 / 已废弃 / [SUPERSEDED by D{NNN}]
  **决策人**: {角色}
  **背景**: {为什么需要做决策}
  **选项**: {列出的候选方案}
  **决策**: {最终选择及理由}
  **影响**: {对项目的影响范围}

反模式记录（anti-patterns.md）:
  ### AP{NNN}: {标题}
  **状态**: active / partially-fixed / fixed
  **位置**: {代码位置}
  **问题**: {问题描述}
  **建议**: {修复建议}
  **关联**: {交叉引用，如 决策: D005}

事故记录（incidents.md）:
  ### INC{NNN}: {标题}（{日期}）
  **状态**: open / investigating / resolved
  **影响**: {影响范围和程度}
  **时间线**: {事件经过}
  **根因**: {根本原因}
  **教训**: {经验教训}
  **关联**: {交叉引用，如 产生已知问题: TD012}

已知问题（known-issues.md）:
  ### TD{NNN}: {标题}
  **状态**: open / in-progress / resolved
  **优先级**: P0 / P1 / P2
  **位置**: {代码位置}
  **描述**: {问题描述}
  **关联**: {交叉引用，如 反模式: AP003、事故: INC002}

架构历史（architecture-history.md）:
  ## {阶段名称}（{时间范围}）
  **目标**: {阶段目标}
  **完成状态**: {完成情况}
  **关键决策**: {引用 D{NNN}}
  **产出**: {阶段产出物}

技术栈记录（tech-stack.md）:
  无编号体系，按分类组织（后端/前端/基础设施）
  变更时追加变更记录，保留历史版本
```

## 状态一致性

```yaml
关联更新:
  - 新决策可能废弃旧决策 → 同时更新旧的标记
  - 事故解决可能产生新的已知问题 → 同时创建 known-issue
  - 反模式修复后更新 anti-patterns.md 状态
  - 架构阶段完成后追加 architecture-history.md

交叉引用规则:
  触发场景:
    - 新决策废弃旧决策 → decisions.md 旧条目标记 [SUPERSEDED by D{NNN}]，新条目引用 旧决策: D{NNN}
    - 事故产生已知问题 → incidents.md 添加 关联: TD{NNN}，known-issues.md 添加 关联: INC{NNN}
    - 反模式修复产生决策 → anti-patterns.md 添加 决策: D{NNN}，decisions.md 添加 关联: AP{NNN}
    - 技术栈变更产生决策 → tech-stack.md 追加变更记录引用 D{NNN}，decisions.md 添加 影响范围: tech-stack
    - 架构阶段引用决策 → architecture-history.md 关键决策引用 D{NNN}

  引用格式:
    - 决策引用: 决策: D{NNN}
    - 反模式引用: 反模式: AP{NNN}
    - 事故引用: 事故: INC{NNN}
    - 已知问题引用: 已知问题: TD{NNN}

禁止:
  - 删除历史决策
  - 修改事故记录的时间线
  - 覆盖已有架构历史条目
  - 删除反模式（即使已修复，保留作为警示）
  - 交叉引用使用模糊描述（如"相关决策"），必须使用编号（如"决策: D005"）
```

## 状态读取时机矩阵

```yaml
说明: AI Agent 在不同任务场景下，必须先读取对应的 memory 文件，避免重复踩坑。

任务场景 → 必须读取:
  新增技术决策: decisions.md, tech-stack.md
  修复 Bug: incidents.md, anti-patterns.md, known-issues.md
  新增功能: decisions.md, project-evolution.md, known-issues.md
  架构设计: architecture-history.md, decisions.md, project-evolution.md
  新增依赖: tech-stack.md, decisions.md
  重构代码: anti-patterns.md, decisions.md, known-issues.md
  CI/CD 变更: decisions.md, incidents.md
  数据库变更: decisions.md, known-issues.md

任务场景 → 建议读取:
  新增功能: anti-patterns.md, tech-stack.md
  修复 Bug: decisions.md
  重构代码: architecture-history.md, project-evolution.md
  代码审查: anti-patterns.md, known-issues.md

写入时机:
  - 做出技术决策 → decisions.md
  - 发现反模式 → anti-patterns.md
  - 发生事故 → incidents.md
  - 发现技术债务 → known-issues.md
  - 技术栈变更 → tech-stack.md
  - 阶段完成 → architecture-history.md, project-evolution.md
```
