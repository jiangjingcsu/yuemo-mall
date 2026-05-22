# Agent 路由器

> 根据任务类型和上下文，选择最合适的 Agent 执行任务。本文件是 Agent 路由的唯一权威定义。

```yaml
元信息:
  版本: 1.1
  最后更新: 2026-05-22
  更新触发条件:
    - 新增/删除 Agent 时
    - Agent 职责变更时
    - 新增任务类型时
    - capability-registry.md 变更时
```

---

## 路由表

| 任务类型 | 主 Agent | 协作 Agent | 触发条件 |
|---|---|---|---|
| 后端功能开发 | backend-agent | architect-agent → tester-agent → reviewer-agent | 新增/修改 Java 代码 |
| 前端页面开发 | frontend-agent | tester-agent → reviewer-agent | 新增/修改 TSX/TS 代码 |
| 代码审查 | reviewer-agent | security-agent（如涉及敏感代码） | 代码修改完成后 |
| 安全审查 | security-agent | reviewer-agent | 涉及认证/支付/用户数据 |
| 架构设计 | architect-agent | reviewer-agent | 新模块/跨模块设计 |
| Bug 修复 | backend-agent 或 frontend-agent | tester-agent → reviewer-agent | Bug 报告 |
| 数据库变更 | backend-agent | architect-agent（表结构评审）→ reviewer-agent | SQL/Flyway 脚本变更 |
| 缓存/Redis | backend-agent | reviewer-agent | Redis 缓存设计/实现 |
| 消息队列 | backend-agent | reviewer-agent | MQ 生产者/消费者编写 |
| API 设计 | backend-agent | architect-agent（API 评审）→ reviewer-agent | 新增/修改 API 端点 |
| DDD 建模 | architect-agent | reviewer-agent | 领域建模/聚合设计 |
| 业务理解 | architect-agent | — | 业务流程梳理/需求分析 |
| DevOps/部署 | devops-agent（需用户确认） | — | CI/CD/Docker/Nginx |
| 测试编写 | tester-agent | backend-agent | 新增测试/测试失败 |
| 性能优化 | architect-agent | backend-agent 或 frontend-agent → tester-agent → performance-review skill | 性能问题 |
| 构建修复 | backend-agent 或 frontend-agent | — | 编译/构建失败 |

---

## Agent 协作顺序

```yaml
功能开发:
  1. architect-agent（架构方案）
  2. backend-agent / frontend-agent（编码实现）
  3. tester-agent（测试）
  4. reviewer-agent（审查）
  5. security-agent（安全审查，如涉及敏感代码）

Bug 修复:
  1. backend-agent / frontend-agent（定位+修复）
  2. tester-agent（验证）
  3. reviewer-agent（审查）

代码重构:
  1. architect-agent（影响分析）
  2. backend-agent / frontend-agent（重构实现）
  3. tester-agent（测试验证）
  4. reviewer-agent（审查）

架构设计:
  1. architect-agent（方案设计）
  2. reviewer-agent（方案审查）

DevOps/部署:
  1. architect-agent（影响评估，如涉及架构变更）
  2. devops-agent（配置/部署执行，需用户确认）
  3. reviewer-agent（配置审查）

性能优化:
  1. architect-agent（性能分析+优化方案）
  2. backend-agent / frontend-agent（优化实现）
  3. tester-agent（性能测试验证）
  4. reviewer-agent（审查）

数据库变更:
  1. architect-agent（表结构评审，如涉及新表/大改）
  2. backend-agent（SQL/Flyway 编写）
  3. reviewer-agent（SQL 审查）
```

---

## Agent 选择决策

```yaml
决策流程:
  Q1: 是否涉及架构变更？
    Yes → architect-agent 先行
    No  → Q2

  Q2: 是否后端代码？
    Yes → backend-agent
    No  → Q3

  Q3: 是否前端代码？
    Yes → frontend-agent
    No  → Q4

  Q4: 是否涉及部署/基础设施？
    Yes → devops-agent（需用户确认）
    No  → Q5

  Q5: 是否涉及安全敏感？
    Yes → security-agent 参与
    No  → Q6

  Q6: 任务类型？
    - 测试编写 → tester-agent
    - 性能优化 → architect-agent + backend/frontend-agent
    - 构建修复 → backend-agent 或 frontend-agent
    - 业务理解 → architect-agent
    - 其他 → 澄清意图后重新路由

  Q7: 修改是否完成？
    Yes → tester-agent → reviewer-agent
    No  → 继续编码
```

---

## Agent 协作模式

```yaml
串行（有依赖）:
  - architect → backend/frontend（设计先行）
  - backend/frontend → tester（代码完成后测试）
  - tester → reviewer（测试通过后审查）

并行（无依赖）:
  - backend-agent + frontend-agent（前后端同时开发）
  - reviewer-agent + security-agent（多维审查同时进行）
  - multiple backend-agents（不同模块独立开发）

循环（迭代修复）:
  - reviewer → backend/frontend → reviewer（修复后重审）
  - 第 1 轮: 自动修复，reviewer 重新审查
  - 第 2 轮: 保守修复，缩小修改范围
  - 第 3 轮: 请求用户介入，提供修复建议供用户选择
  - 超过 3 轮: 暂停任务，向用户报告阻塞原因
```

---

## Agent 权限边界

| Agent | 可以 | 不可以 |
|---|---|---|
| architect-agent | 设计架构、分析影响、制定方案、输出文档 | 直接修改生产代码、修改数据库、部署 |
| backend-agent | 创建/修改 Java/SQL/XML、编译、运行测试 | 部署到生产、修改 CI/CD、修改 Flyway 已执行脚本（仅允许新增迁移脚本） |
| frontend-agent | 创建/修改 TSX/TS/CSS、类型检查、构建 | 修改后端代码、修改 vite.config.ts 核心配置（需用户确认） |
| reviewer-agent | 审查代码、提出建议、BLOCK 不通过代码 | 直接修改代码、强制通过 |
| security-agent | 安全审查、标记 CRITICAL 问题、BLOCK 不合规代码 | 直接修改代码 |
| devops-agent | 构建镜像、执行部署、健康检查、回滚 | 跳过用户确认、修改生产配置 |
| tester-agent | 编写测试、运行测试、分析失败 | 修改业务代码（除非测试暴露业务 Bug） |

---

## 能力组合

> 复杂任务需要多种能力按序组合。能力详情见 `.harness/runtime/capability-registry.md`。

| 任务 | 能力组合顺序 |
|---|---|
| 新功能开发 | 领域建模 → API设计 → 数据库设计 → 缓存设计(如需) → MQ设计(如需) → 代码生成 → 编译验证 → 代码审查 |
| Bug 修复 | Bug定位 → 代码修改 → 编译验证 → 测试运行 |
| 重构 | 代码审查 → 架构评估 → 代码修改 → 编译验证 → 测试运行 |
| 微服务评估 | 架构评估 → 领域建模 |
| 代码审查 | 代码审查 → 安全审查(如需) |
| 架构设计 | 领域建模 → 架构评估 → API设计 |
| 部署 | 编译验证 → 测试运行 → 镜像构建 → 部署执行 → 健康检查 |

---

## Agent 冲突解决

```yaml
前后端同时涉及的 Bug:
  - 由用户指定主导 Agent
  - 未指定时，后端优先（后端 API 变更通常驱动前端适配）

架构 + 代码同时涉及的重构:
  - architect-agent 主导方案设计
  - backend-agent / frontend-agent 负责实现
  - 方案未确认前不开始编码

多 Agent 修改同一文件:
  - 同一时间只有一个 Agent 修改同一文件
  - 通过 git 锁定机制或用户协调
```

---

## 与现有文件的关联

```yaml
本文件是 Agent 路由的唯一权威定义，以下文件依赖本文件:
  - execution-engine.md: 执行管线的 Agent 调度
  - capability-registry.md: 能力→Agent 映射（必须与本文件路由表一致）
  - workflow-router.md: 意图→工作流映射（工作流决定步骤，本文件决定谁执行）
  - CLAUDE.md: 顶层路由表（简化版，完整定义以本文件为准）

优先级: agent-router.md > CLAUDE.md 路由表
一致性要求: 任何文件中的 Agent 路由信息变更时，必须同步更新本文件
```
