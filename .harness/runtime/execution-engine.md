# 执行引擎

> 协调 Executor 执行任务的标准流程引擎。

---

## 三层职责边界

```yaml
Workflow（做什么）:
  定义: 任务的标准步骤序列（SOP）
  关注: 步骤顺序、每步的输入/输出、验证条件
  文件: .harness/workflows/*.md
  示例: feature-development.md 定义 8 步开发流程

Agent（谁来做）:
  定义: 执行者的角色定义、权限边界、协作规则
  关注: 谁负责什么、谁不能做什么、Agent 间如何协作
  文件: .harness/agents/*.md
  示例: backend-agent.md 定义后端 Agent 可修改 Java/SQL/XML

Executor（怎么做）:
  定义: 具体执行策略和调度逻辑
  关注: 调度哪个 Agent、执行顺序、失败重试、回滚策略
  文件: .harness/executors/*.md
  示例: code-executor.md 定义代码生成的执行管线

关系:
  Workflow → 定义步骤 → Executor 调度 Agent 执行 → Agent 按权限操作
  三层不可互相替代，各司其职
```

---

## 执行管线

```
                    ┌──────────────────────┐
                    │   workflow-executor   │
                    │   标准执行管线        │
                    └──────────┬───────────┘
                               │
      ┌──────────┬─────────┬──┴───┬──────────┬───────────┐
      │          │         │      │          │           │
┌─────▼─────┐ ┌──▼──────┐ ┌▼─────▼┐ ┌──────▼────┐ ┌────▼────────┐
│code-exec  │ │review-  │ │deploy-│ │analysis-  │ │refactor-exec│
│  utor     │ │executor │ │execut │ │executor   │ │             │
│代码生成   │ │代码审查 │ │ 部署  │ │架构分析   │ │安全重构     │
└───────────┘ └─────────┘ └───────┘ └───────────┘ └─────────────┘
```

## 执行流程

```yaml
1. 任务输入:
   - 解析用户意图
   - 确定任务类型
   - 路由到对应 workflow（workflow-router.md）
   - 路由到对应 agent（agent-router.md）

2. 前置检查:
   - 加载对应 executor
   - 验证前置条件
   - 检查 constraints/ 红线
   - 读取 .harness/memory/ 避免重复踩坑
   - 上下文漂移检测（skills/drift-detection/SKILL.md）
     - 根据任务类型选择相关检测项
     - L3 漂移 → 终止，先修复文档
     - L2 漂移 → 提醒用户确认
     - L1 漂移 → 记录，不阻断
   - 不通过 → 终止并报告

3. 上下文加载:
   - 按 context-router.md 策略加载
   - Token 预算检查（单次不超过 80K tokens，参见 tools-permissions.md）
   - 加载必读文件和源码
   - 优先读取方法签名和类结构，避免读取完整实现
   - 超过 500 行的文件，只读取相关段落

4. 执行:
   - 按 executor 定义的步骤执行
   - 每步验证（编译/测试）
   - 失败则重试（最多 3 次）

5. 评估:
   - 按 evaluation/review-checklist.md 执行综合评估
   - 代码质量评估（evaluation/code-quality.md）
   - 架构合规评估（evaluation/architecture-score.md）
   - 幻觉检测（evaluation/hallucination-check.md）
   - 安全评估（evaluation/security-check.md）
   - 风险评估（evaluation/risk-analysis.md）
   - 输出验证（evaluation/output-validator.md）

6. 输出:
   - 生成综合评估报告
   - 状态: PASS → 继续下一步 / WARN → 建议修复 / FAIL → BLOCK
```

## 失败处理

```yaml
编译失败:
  1. 捕获编译错误
  2. 分析错误原因
  3. 自动修复（backend-agent 或 frontend-agent）
  4. 重新编译
  5. 3 次仍失败 → 暂停，向用户报告

测试失败:
  1. 分析测试失败原因
  2. 判断是代码问题还是测试问题
  3. 代码问题 → 修复代码
  4. 测试问题 → 修复测试（需用户确认）
  5. 3 次仍失败 → 暂停

评估失败（FAIL）:
  1. 分析 FAIL 维度
  2. 修复对应问题
  3. 重新评估
  4. 3 次仍 FAIL → 暂停

约束违规:
  1. 立即停止执行
  2. 报告具体违规内容
  3. 等待用户决策

Lint 失败:
  1. 分析 lint 错误
  2. 判断是否本次修改引入
  3. 本次引入 → 自动修复
  4. 既有问题 → 标注非本次引入，不阻塞
  5. 3 次仍失败 → 暂停

Token 预算耗尽:
  1. 保存当前执行进度到 state-manager
  2. 输出已完成的部分结果
  3. 标记未完成步骤
  4. 建议用户分拆任务或从断点继续
```

## 回滚机制

```yaml
自动回滚:
  - git checkout <file>（代码级回滚）
  - 清理临时文件

手动回滚（需用户明确要求）:
  - git reset --hard <commit>（全局回滚，参见 tools-permissions.md）
  - docker-compose up -d <previous_version>（部署回滚）

不可回滚:
  - Flyway 迁移（DDL 不可逆，参见 constraints/database.md）
  - MQ 消息（已发送不可撤回，参见 architecture-governance.md 事务边界）
  - 外部 API 调用（如短信发送）
```

## Agent 调度

```yaml
调度器:
  workflow 路由: workflow-router.md
  agent 路由: agent-router.md
  能力注册: capability-registry.md

状态持久化: runtime/state-manager.md

执行 Agent:
  - backend-agent: Java 代码
  - frontend-agent: TypeScript/React 代码
  - reviewer-agent: 代码审查
  - security-agent: 安全审查
  - devops-agent: 部署（需用户确认）
  - architect-agent: 架构设计/分析
  - tester-agent: 测试编写与执行

Workflow → Executor 映射:
  feature-development → code-executor
  bug-fix → code-executor
  code-review → review-executor
  refactor → refactor-executor
  architecture-design → analysis-executor

协作规则:
  - 同一时间只有一个 Agent 修改同一文件
  - 并行 Agent 操作不同模块/文件
  - 关键阶段（编译/测试/审查）必须串行

上下文传递:
  - Agent 间通过文件系统传递结果
  - 上游 Agent 输出 → 下游 Agent 输入
  - 评价结果写入 evaluation 报告 → 后续 Agent 读取
```
