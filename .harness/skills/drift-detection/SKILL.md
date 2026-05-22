# 漂移检测技能

> 检测 `.harness/` 文档与代码实际状态之间的偏差，防止 AI Agent 基于过时信息工作。
> 触发时机：任务执行前（自动）、代码审查时（自动）、用户主动触发。

---

## 1. 问题定义

```yaml
上下文漂移:
  定义: .harness/ 中的治理规则、架构描述、模块边界等文档与代码实际状态不一致
  危害: AI Agent 基于过时信息做决策，导致违反架构规则、引入安全漏洞、重复踩坑

两类漂移:
  结构性漂移: 代码结构变更（新模块/新表/新接口/新MQ）但文档未同步
    特点: 可自动检测，规则明确
  语义性漂移: 代码逻辑变更（业务流程/状态机/校验规则）但文档描述仍为旧逻辑
    特点: 半自动检测，需人工确认
```

---

## 2. 漂移等级

| 等级 | 含义 | 处理方式 | 示例 |
|---|---|---|---|
| **L1 Warning** | 文档可能过时 | 提醒，不阻断 | 新增 Controller 端点但 api-governance 未记录 |
| **L2 Required Review** | 架构治理文档与代码不一致 | 必须确认后才能继续 | 新增 MQ Topic 但 ddd-governance 未记录 |
| **L3 Blocking** | 严重不一致，影响安全或架构 | 停止执行，必须修复 | 安全白名单缺失、跨模块违规未记录 |

---

## 3. 结构性漂移检测清单

### 3.1 Maven 模块结构

```yaml
扫描方法: 列出 yuemo-backend/yuemo-modules/ 下所有子目录
对比文件:
  - rules/module-boundary.md §1 模块数据所有权
  - rules/ddd-governance.md §1.1 当前领域划分
漂移判定: 目录中出现新模块或模块被删除
严重等级: L3
```

### 3.2 数据库表

```yaml
扫描方法: grep @TableName 注解，提取表名和所属模块
对比文件:
  - rules/module-boundary.md §1 模块数据所有权表
漂移判定:
  - 新增表但文档未记录 → L2
  - 表从模块中删除但文档仍记录 → L2
  - 表归属模块变更 → L3
```

### 3.3 Entity 类

```yaml
扫描方法: 列出各模块 entity/ 包下的 Java 类
对比文件:
  - rules/ddd-governance.md §3.1 当前聚合根表
漂移判定:
  - 新增 Entity 但聚合根表未记录 → L2
  - Entity 聚合归属变更 → L2
  - 内部实体被外部直接引用 → L2
```

### 3.4 Controller 端点

```yaml
扫描方法: grep @RequestMapping/@GetMapping/@PostMapping/@PutMapping/@DeleteMapping
对比文件:
  - rules/api-governance.md §1.2 完整接口清单
漂移判定:
  - 新增端点但文档未记录 → L1
  - 端点路径/方法变更但文档未同步 → L1
  - 新增 /api/admin/** 端点但未记录 → L2
```

### 3.5 MQ Topic

```yaml
扫描方法:
  - 生产者: grep RocketMQTemplate 的 convertAndSend/sendMessageInTransaction 调用
  - 消费者: grep @RocketMQMessageListener 注解
对比文件:
  - rules/ddd-governance.md §5 领域事件
  - rules/mq-governance.md
漂移判定:
  - 新增 Topic 但文档未记录 → L2
  - Topic 消费者/生产者变更但文档未同步 → L2
  - 事务消息降级为普通消息但文档仍标记为事务消息 → L2
```

### 3.6 Service 接口

```yaml
扫描方法: 列出各模块 service/ 包下的接口（非 impl）
对比文件:
  - runtime/capability-registry.md 暴露服务列
  - rules/module-boundary.md §2 模块依赖白名单
漂移判定:
  - 新增跨模块 Service 调用但白名单未更新 → L2
  - 暴露服务增删但 capability-registry 未同步 → L1
```

### 3.7 跨模块依赖

```yaml
扫描方法: 读取各模块 pom.xml 的 <dependency> 节点
对比文件:
  - rules/module-boundary.md §2 模块依赖白名单
漂移判定:
  - 新增跨模块 Maven 依赖但白名单未更新 → L3
  - 依赖被删除但白名单仍记录 → L1
```

### 3.8 跨模块 Import

```yaml
扫描方法: grep 跨模块 import 语句（如 import com.yuemo.product.entity 在 order 模块中）
对比文件:
  - rules/module-boundary.md §4 已知耦合问题
  - rules/ddd-governance.md §6 防腐层
漂移判定:
  - 新增跨模块 Entity 引用但文档未记录 → L2
  - 已记录的泄露被修复但文档仍标记 → L1
```

### 3.9 错误码

```yaml
扫描方法: 读取 ResultCode 枚举的所有值
对比文件:
  - rules/api-governance.md §3.1 错误码分段
漂移判定:
  - 新增错误码但文档未记录 → L1
  - 错误码超出分配范围 → L2
```

### 3.10 API 安全白名单

```yaml
扫描方法: 读取 GatewayAuthFilter 或 application.yml 中的白名单配置
对比文件:
  - rules/api-governance.md §5 接口安全
漂移判定:
  - 代码白名单与文档不一致 → L3
  - 新增白名单路径但文档未记录 → L3
```

---

## 4. 语义性漂移检测清单

### 4.1 Entity 领域方法

```yaml
扫描方法: 对比 Entity 类中的业务方法签名（非 getter/setter）
对比文件:
  - rules/ddd-governance.md §3.3 领域模型成熟度评估
漂移判定:
  - Entity 新增/删除业务方法但成熟度评估未更新 → L1
  - 贫血 Entity 变为充血但文档仍标记为贫血 → L1
```

### 4.2 DTO 字段变更

```yaml
扫描方法: 对比 DTO/Request 类的字段列表
对比文件:
  - rules/api-governance.md 接口清单中的参数描述
漂移判定:
  - 必填字段增删但文档未同步 → L2
  - 字段类型变更（如 Integer→BigDecimal）但文档未同步 → L1
```

### 4.3 业务流程变更

```yaml
检测方法: 人工 Review（AI 辅助识别）
对比文件:
  - rules/ddd-governance.md 领域事件
  - skills/ 下相关 SKILL.md
漂移判定:
  - 同步调用改为 MQ 异步但文档仍描述为同步 → L2
  - 状态机新增/删除状态但文档未同步 → L2
  - 事务边界变更但文档未同步 → L2
```

---

## 5. 检测执行流程

### 5.1 任务执行前（自动触发）

```yaml
触发条件: AI Agent 开始执行任何后端/架构相关任务时
执行步骤:
  1. 根据任务类型选择相关检测项（不必全部执行）
  2. 扫描代码，提取当前状态
  3. 与 .harness/ 文档对比
  4. 生成漂移报告

任务类型 → 检测项映射:
  后端开发: 3.2 + 3.4 + 3.9 + 3.10
  API 设计: 3.4 + 3.9 + 3.10
  DDD 建模: 3.2 + 3.3 + 3.5 + 3.8
  数据库变更: 3.2
  MQ 设计: 3.5
  架构评估: 3.1 + 3.3 + 3.7 + 3.8
  代码审查: 全部结构性检测项

结果处理:
  L3: 停止任务，先修复漂移
  L2: 提醒用户，用户确认后继续
  L1: 记录到漂移报告，不阻断
```

### 5.2 代码审查时（自动触发）

```yaml
触发条件: code-review 工作流执行时
执行步骤:
  1. 在审查第一层（安全检查）之前插入漂移检测层
  2. 执行全部结构性检测项
  3. 对变更涉及的模块执行语义性检测
  4. 漂移报告纳入审查报告

结果处理:
  L3 漂移: 标记为 CRITICAL
  L2 漂移: 标记为 HIGH
  L1 漂移: 标记为 MEDIUM
```

### 5.3 用户主动触发

```yaml
触发条件: 用户要求"检查文档漂移"/"同步 harness"/"drift check"
执行步骤:
  1. 执行全部检测项（结构性 + 语义性）
  2. 生成完整漂移报告
  3. 对每项漂移给出修复建议
  4. 用户确认后自动修复文档
```

---

## 6. 漂移报告格式

```markdown
## Context Drift Report

**检测时间**: {timestamp}
**触发方式**: {任务执行前 | 代码审查 | 手动触发}
**检测范围**: {检测项列表}

### L3 Blocking
| # | 漂移点 | 代码实际 | 文档描述 | 影响文件 | 修复建议 |
|---|---|---|---|---|---|
| 1 | ... | ... | ... | ... | ... |

### L2 Required Review
| # | 漂移点 | 代码实际 | 文档描述 | 影响文件 | 修复建议 |
|---|---|---|---|---|---|
| 1 | ... | ... | ... | ... | ... |

### L1 Warning
| # | 漂移点 | 代码实际 | 文档描述 | 影响文件 | 修复建议 |
|---|---|---|---|---|---|
| 1 | ... | ... | ... | ... | ... |

### Summary
- L3: {count} 项（必须修复）
- L2: {count} 项（需确认）
- L1: {count} 项（建议更新）
```

---

## 7. CI/CD 集成设计（阶段二）

### 7.1 Git Pre-commit Hook

```yaml
范围: 仅检测本次提交涉及的变更
速度要求: < 5 秒
检测项:
  - 新增 @TableName → 提示检查 module-boundary.md
  - 新增 @RocketMQMessageListener → 提示检查 ddd-governance.md
  - 新增 pom.xml 跨模块依赖 → 提示检查 module-boundary.md
  - 新增 Controller 端点 → 提示检查 api-governance.md
输出: 简短提示，不阻断提交
```

### 7.2 CI Pipeline Stage

```yaml
stage: drift-check
位置: test stage 之后，deploy stage 之前
检测项: 全部结构性检测
输出:
  - 漂移报告写入 PR Comment
  - L3 → 阻止合并
  - L2 → 添加 "needs-review" 标签
  - L1 → 仅记录

实现方式:
  - Shell 脚本扫描代码生成 current-state.json
  - Python/Node 脚本对比 current-state.json 与 .harness/ 文档
  - 输出 drift-report.json
  - GitLab CI Bot 将报告写入 PR Comment
```

---

## 8. 语义漂移检测设计（阶段三）

```yaml
策略: AI 辅助 + 人工确认

Entity 方法签名对比:
  - 维护 entity-methods-snapshot.md（记录每个 Entity 的业务方法签名）
  - 每次代码审查时对比当前方法签名与快照
  - 差异自动标记为潜在语义漂移
  - 人工确认是否为真实漂移

DTO 字段对比:
  - 维护 dto-fields-snapshot.md（记录每个 DTO 的字段列表和类型）
  - 每次代码审查时对比当前字段与快照
  - 必填字段变更自动标记为 L2

业务流程变更:
  - 最难自动化的部分
  - 策略: 在 code-review 工作流中增加"流程变更检查"步骤
  - AI Agent 分析变更是否涉及:
    - 同步→异步切换
    - 状态机变更
    - 事务边界变更
  - 检测到时标记为 L2，需人工确认
```
