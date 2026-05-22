# 工作流路由器

> 根据任务意图，路由到对应的工作流，确保 AI Agent 按正确的 SOP 执行任务。
> Agent 路由见 `agent-router.md`，两者由 `execution-engine.md` 在任务输入阶段同时激活。
> 工作流定义"做什么"（步骤），Agent 定义"谁来做"（角色）。

---

## 1. 意图识别与路由

### 1.1 核心路由表

| 用户意图关键词 | 工作流 | Agent | 补充规则 |
|---|---|---|---|
| "新增/添加/实现/开发 + 功能 + 后端/API/接口/Service" | feature-development.md | backend-agent.md | backend-coding.md |
| "新增/添加/实现/开发 + 功能 + 前端/页面/组件/样式" | feature-development.md | frontend-agent.md | frontend-coding.md |
| "新增/添加/实现/开发 + 功能"（未指定前后端） | feature-development.md | 根据上下文判断 | — |
| "修复/Bug/报错/异常/不工作/有问题" | bug-fix.md | backend/frontend-agent | — |
| "重构/优化结构/改善/整理代码" | refactor.md | architect + backend/frontend-agent | — |
| "审查/Review/检查代码" | code-review.md | reviewer-agent.md | evaluation/ |
| "安全审查/安全检查/漏洞扫描" | code-review.md | security-agent.md | constraints/ |
| "设计架构/方案/怎么实现/选型" | architecture-design.md | architect-agent.md | — |

### 1.2 扩展路由表（映射到现有工作流）

以下任务类型无独立工作流文件，映射到现有工作流 + 补充规则驱动：

| 用户意图关键词 | 工作流 | Agent | 补充规则 |
|---|---|---|---|
| "数据库/表结构/迁移/DDL" | feature-development.md | backend-agent.md | constraints/database.md + database-governance.md |
| "缓存/Redis/缓存策略" | feature-development.md | backend-agent.md | redis-governance.md |
| "消息队列/MQ/消费者/Topic" | feature-development.md | backend-agent.md | mq-governance.md |
| "API设计/接口设计/REST" | feature-development.md | backend-agent.md | skills/api-design + api-governance.md |
| "DDD/领域建模/领域设计" | architecture-design.md | architect-agent.md | skills/ddd-design + ddd-governance.md |
| "测试/单元测试/集成测试" | feature-development.md | tester-agent.md | — |
| "业务理解/业务逻辑/业务流程" | 无工作流 | — | docs/business-domain.md + data-flow.md |
| "部署/CI/CD/发布/上线" | 无工作流 | devops-agent.md | constraints/deployment.md |

---

## 2. 路由决策树

```
用户输入
  │
  ├── 包含"报错"/"异常"/"不工作"/"Exception" ──→ bug-fix.md（最高优先级）
  │
  ├── 包含"审查"/"Review"/"安全审查"/"漏洞" ──→ code-review.md
  │
  ├── 包含"架构"/"方案"/"设计"/"DDD"/"领域建模" ──→ architecture-design.md
  │
  ├── 包含"重构"/"优化结构"/"整理"/"技术债" ──→ refactor.md
  │
  ├── 包含"新"/"添加"/"实现"/"开发" ──→ feature-development.md
  │     └── 细分: 后端关键词 → backend-agent
  │               前端关键词 → frontend-agent
  │               数据库关键词 → backend-agent + database constraints
  │               缓存/Redis关键词 → backend-agent + redis-governance
  │               MQ关键词 → backend-agent + mq-governance
  │               API关键词 → backend-agent + api-design skill
  │               测试关键词 → tester-agent
  │
  ├── 包含"部署"/"CI/CD"/"发布" ──→ 无工作流，devops-agent + deployment constraints
  │
  ├── 包含"业务理解"/"业务流程" ──→ 无工作流，加载 docs
  │
  └── 无法匹配 ──→ 澄清意图或默认 feature-development.md
```

### 优先级规则（高到低）

1. **bug-fix** — 包含"报错"/"异常"/"不工作"时优先级最高
2. **code-review** — 包含"审查"/"Review"时
3. **architecture-design** — 包含"架构"/"方案"/"设计"时
4. **refactor** — 包含"重构"/"优化"时
5. **feature-development** — 默认开发流程

### 模糊意图处理

- 多个关键词冲突时，按优先级取最高者（如"重构这个 Bug" → bug-fix 优先）
- 无法判断时向用户确认任务类型（提供选项）
- 根据上下文推断（如当前在修改 Bug 相关文件 → 推测 bug-fix）
- 默认路由：`feature-development.md`（最通用的流程）

---

## 3. 工作流组合

复杂任务可能需要多个工作流组合：

| 场景 | 工作流组合 | 触发条件 |
|---|---|---|
| 大型功能开发 | architecture-design → feature-development | 涉及 ≥ 2 个模块 或 需要先做架构决策 |
| 技术债务清理 | refactor | 用户明确提到"技术债"/"清理"/"改善代码质量" |
| 生产问题 | bug-fix → code-review | 涉及生产环境故障 |
| 微服务拆分 | architecture-design → refactor | 涉及模块独立部署 |

---

## 4. 工作流激活规则

```yaml
激活:
  - 任务开始时，根据意图自动激活对应工作流
  - 同时激活对应 Agent（见 agent-router.md）
  - 工作流中的每步必须执行，不可跳过
  - 当前步未完成，不进入下一步

跳过:
  - 用户明确指示跳过某步时（如"不需要测试"）
  - 某步条件不满足时（如无 API 变更则跳过 API 检查）

终止:
  - 关键检查失败且 3 次修复不通过时
  - 用户明确要求停止时
```

### 工作流在执行引擎中的位置

```yaml
与 execution-engine.md 的对齐:
  - 路由时机: 第1阶段（Task Intake）— 同时路由 workflow + agent
  - 前置检查: 第2阶段（Pre-check）— 加载 constraints
  - 上下文加载: 第3阶段（Context Loading）— context-router.md 根据工作流类型加载对应文件
  - 步骤执行: 第4阶段（Execution）— 按工作流步骤逐步执行
  - 每步验证: 第5阶段（Evaluation）— 代码质量/架构合规/安全评估
  - 输出: 第6阶段（Output）— 综合评估报告
```
