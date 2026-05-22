# 架构决策治理规则

> 建立 AI Agent 的架构决策治理体系。让 AI 理解何时允许简单实现、何时必须面向扩展设计。消灭 switch-case 地狱和 God Service。

```yaml
元信息:
  版本: 1.1
  最后更新: 2026-05-22
  更新触发条件:
    - 新增高扩展模块时
    - 高扩展模块完成策略模式重构后（更新"当前做法"列）
    - 每季度更新高变更模块统计
    - 架构决策记录（.harness/memory/decisions.md）新增相关决策时

范围声明:
  本规则仅覆盖后端 Java 代码的架构决策。
  前端架构决策参见 frontend-coding.md 和 frontend-agent.md。
```

---

## 1. switch-case 决策矩阵

### 禁止场景

```yaml
禁止 switch-case / if-else 链:
  核心业务路由:
    - action/event 消息分发（如 CartSyncConsumer）
    - 支付方式路由（如 PaymentServiceImpl）
    - MQ Topic 消费分发
    - 订单状态流转逻辑
    - 优惠券类型处理
    - 通知渠道分发

  高频扩展模块:
    - 支付系统（payType 可能增加银行卡/花呗/银联等）
    - 营销规则（满减/折扣/立减/秒杀/拼团）
    - MQ 事件（事件类型持续增加）
    - 状态流转（订单/支付/退款，状态可能增加）
    - 权限校验（角色/资源可能扩展）
    - 工作流节点
```

### 允许场景

```yaml
允许 switch-case / if-else:
  简单转换:
    - enum → 显示文本（如 OrderStatus.PENDING → "待支付"）
    - 字段值映射（无行为分发）
    - DTO 属性赋值

  非扩展逻辑:
    - 至多 2 个分支的简单判断
    - 一次性业务逻辑（确认不会扩展）
    - 日志级别判断
    - HTTP 状态码分类

  语法糖:
    - Java 14+ switch expression（仅用于值映射，不包含行为）
```

---

## 2. 架构层级决策

### Controller 层

```yaml
允许:
  - 参数校验
  - 调用 Service
  - 返回 Result<T>

禁止:
  - 任何业务判断（if/else/switch）
  - 调用多个 Service 做编排（编排在 ServiceImpl）
  - 直接操作 Redis/MQ
```

### Service 层

```yaml
允许:
  - 业务编排（调用多个 Service/Mapper）
  - 事务管理 @Transactional
  - 调用领域服务

禁止:
  - 行为分发的 switch-case（>2 分支 → 策略模式）
  - 跨模块 Mapper 调用（cart→product 只读除外）
  - 直接拼接 SQL
  - 单方法超过 50 行

强制:
  - 高扩展模块必须使用策略模式 + 工厂模式
  - 跨模块通信走 MQ（非本地事务）
```

### Domain 层（Entity + DomainService）

```yaml
强制:
  - Entity 包含领域行为（非贫血模型）
  - 状态校验写在 Entity 或 DomainService 中
  - 聚合根保证内部一致性

禁止:
  - Entity 只是数据结构（贫血）
  - 领域逻辑写在 ServiceImpl 中（应抽到 DomainService）
  - 聚合根直接暴露内部实体引用
```

---

## 3. 高扩展模块治理

### 识别标准

满足以下**任一**条件即为高扩展模块：

```yaml
高扩展模块:
  - 业务分支 ≥ 3 且未来可能增加
  - 历史上已扩展过 2 次以上
  - 需求文档中明确提到"多种"、"不同"等
  - 对应数据库中有 type/action/status 等枚举型字段，且当前值 ≥ 3 且业务上明确会继续增加
```

### 高扩展模块清单（当前项目）

| 模块 | 高扩展点 | 当前做法 | 要求 | 接口设计参考 |
|---|---|---|---|---|
| 购物车 | CartSyncMessage.Action（5种） | switch-case | 策略模式 | → extensibility-governance.md §2.2 |
| 支付 | payType（1-微信 2-支付宝 3-平台余额） | if-else | 策略+工厂 | → extensibility-governance.md §2.1 |
| 订单 | status 状态流转（5种） | ServiceImpl 中判断 | 状态模式 | → extensibility-governance.md §2.3 |
| 营销 | 优惠券类型（满减/折扣/立减） | if-else | 策略+工厂 | → extensibility-governance.md §2.4 |
| MQ消费 | Topic/action 路由 | switch-case | 策略+工厂 | → extensibility-governance.md §2.2 |
| 权限 | 角色校验（当前仅ADMIN） | 硬编码 | 权限处理器+责任链 | → extensibility-governance.md §2.5 |
| 通知 | 通知渠道（短信/邮件/推送等） | 未实现 | NotificationHandler+Factory | → extensibility-governance.md §2.6 |

### 强制架构

```yaml
高扩展模块必须:
  - Handler 接口 + 多个 @Component 实现
  - HandlerFactory（Spring 自动装配所有 Handler）
  - 新增 Handler 不修改原有代码（OCP）
  - 通过 @Component 自动注册，无需手动维护 Handler 列表

禁止:
  - switch-case 分发
  - if-else 链分发
  - 手动逐个 handler.put(type, handler) 注册（容易遗漏）
  - 在 Factory 中硬编码 new XxxHandler()（绕过 Spring 管理）
  - 新增分支需要修改 Dispatcher 类

允许:
  - 通过 List<Handler> 构造器注入 + Stream 收集到 Map（Spring 自动装配）
```

---

## 4. 高变更模块治理

### 识别标准

```yaml
高变更模块:
  - 近 3 个月修改超过 5 次
  - 多人同时修改同一文件
  - Bug 频发区域
```

### 治理要求

```yaml
高变更模块必须:
  - 解耦: 拆分为多个小类（单类 < 300 行）
  - 接口隔离: 对外暴露接口而非实现
  - SPI: 提供扩展点（新增行为不修改原有代码）
  - 可插拔: @Component 注册，删除不影响其他功能
```

### 高变更模块清单（需定期更新）

```yaml
统计方法: git log --since="3 months ago" --format="%H %an" -- <module-path>
更新频率: 每季度

| 模块 | 近3月修改次数 | 多人修改 | Bug频发 | 治理措施 |
|---|---|---|---|---|
| （待统计填入） | | | | |
```

---

## 5. AI 架构决策流程

AI Agent 在编写代码前，必须执行以下决策：

```
阶段一: 行为分发决策
  Q1: 这段代码包含行为分发吗？
  ├── 否 → 允许简单实现（进入阶段二）
  └── 是 → Q2
  Q2: 分支数 ≥ 3 或属于高扩展模块？
  ├── 否 → 允许至多 2 个 if-else（进入阶段二）
  └── 是 → 必须使用策略模式 + 工厂模式（进入阶段二）

阶段二: 领域完整性决策
  Q3: 涉及状态流转吗？
  ├── 否 → 继续
  └── 是 → 状态数 ≥ 4 且行为差异大？
      ├── 是 → 必须使用状态模式
      └── 否 → 可在 Entity 中用枚举方法处理

阶段三: OCP 合规验证
  Q4: 新增分支需要修改原有类吗？
  ├── 否 → ✅ 设计合规，可以执行
  └── 是 → ❌ 违反 OCP，回到阶段一重新设计
```

---

## 6. 与现有规则的关联

```yaml
本规则是架构决策总纲，以下规则为本规则的具体展开:
  - extensibility-governance.md: 高扩展模块详细清单和接口设计
  - design-pattern-governance.md: 策略/工厂/状态/责任链模式的标准模板
  - code-smell-governance.md: 坏味道识别标准与修复优先级
  - domain-modeling.md: 充血模型的具体实践指南

以下规则与本规则有交叉约束:
  - agent-behavior.md: AI 行为约束（违反架构决策 = 违反行为约束）
  - ddd-governance.md: 开关判断破坏领域封装
  - module-boundary.md: God Service 可能跨模块
  - architecture-governance.md: 分层架构定义（注意 Entity 业务逻辑的表述冲突，以本规则为准）

违反本规则时:
  - 同时违反 agent-behavior.md（AI 行为约束）
  - 同时违反 ddd-governance.md（开关判断破坏领域封装）
  - 同时违反 module-boundary.md（God Service 可能跨模块）

高扩展模块长期使用 switch-case 的后果:
  - 代码难维护（新增分支需要理解和修改现有逻辑）
  - 测试困难（每个 case 的测试互相影响）
  - 无法微服务拆分（逻辑过度集中）
  - 违反开闭原则（对修改开放而非对扩展开放）
```

---

## 7. 策略模式使用边界

```yaml
必须使用: 分支 ≥ 3 或属于高扩展模块
建议使用: 分支 = 2 但属于高扩展模块（预防性设计）
允许简单实现: 分支 ≤ 2 且不属于高扩展模块且确认不会扩展

判断"不会扩展"的依据:
  - 该字段是技术枚举而非业务枚举（如 HTTP 方法、日志级别）
  - 需求文档明确标注为固定值
  - 存在超过 1 年未变更的历史记录

不确定时: 优先面向扩展设计（宁可多设计，不可欠设计）
```

---

## 8. 常见决策冲突指引

```yaml
行为分发 + 状态流转并存:
  优先使用策略模式处理行为分发，
  状态流转逻辑封装在 Entity 的领域行为中。
  例: 订单支付 → PayTypeHandler(策略) 处理支付方式分发，
      Order.pay() (Entity行为) 处理状态校验和流转

Handler 间需要共享状态:
  通过方法参数传递上下文对象，禁止 Handler 持有可变状态。
  例: handler.handle(message, ProcessingContext ctx)

Handler 需要调用其他模块 Service:
  允许在 Handler 中注入其他模块的 Service 接口，
  但必须遵守 module-boundary.md 的依赖白名单。

Entity 领域行为 vs 基础设施逻辑:
  Entity 应包含领域行为（如状态校验、金额计算），
  但禁止包含基础设施逻辑（如直接调 Mapper/Redis/MQ）。
  详见 ddd-governance.md 和 domain-modeling.md。
```
